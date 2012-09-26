#!/usr/bin/env python
#-- Content-Encoding: UTF-8 --
"""
Created on 1 mars 2012

:author: Thomas Calmant
"""

# ------------------------------------------------------------------------------

import logging
import threading
import traceback

from jsonrpclib.SimpleJSONRPCServer import SimpleJSONRPCDispatcher
import jsonrpclib

# ------------------------------------------------------------------------------

from pelix.ipopo.decorators import ComponentFactory, Requires, Validate, \
    Invalidate, Instantiate, Property, Provides
from base.javautils import to_jabsorb, from_jabsorb, JAVA_CLASS

import pelix.framework as pelix

# ------------------------------------------------------------------------------

_logger = logging.getLogger(__name__)

EXPORTED_SERVICE_FILTER = "(|(service.exported.interfaces=*)" \
                            "(service.exported.configs=*))"

JAVA_BEANS_PACKAGE = "org.psem2m.isolates.services.remote.beans"
JAVA_ENDPOINT_DESCRIPTION = "%s.EndpointDescription" % JAVA_BEANS_PACKAGE
JAVA_REMOTE_SERVICE_EVENT = "%s.RemoteServiceEvent" % JAVA_BEANS_PACKAGE
JAVA_SERVICE_EVENT_TYPE = "%s$ServiceEventType" % JAVA_REMOTE_SERVICE_EVENT
JAVA_REMOTE_SERVICE_REGISTRATION = "%s.RemoteServiceRegistration" \
                                    % JAVA_BEANS_PACKAGE

BROADCASTER_SIGNAL_NAME_PREFIX = "/psem2m/remote-service-broadcaster"
BROADCASTER_SIGNAL_REQUEST_ENDPOINTS = "%s/request-endpoints" \
                                        % BROADCASTER_SIGNAL_NAME_PREFIX
SIGNAL_REMOTE_EVENT = "%s/remote-event" % BROADCASTER_SIGNAL_NAME_PREFIX
SIGNAL_REQUEST_ENDPOINTS = "%s/request-endpoints" \
                                        % BROADCASTER_SIGNAL_NAME_PREFIX

SERVICE_EXPORTED_PREFIX = "service.exported."
SERVICE_EXPORTED_CONFIGS = "service.exported.configs"
SERVICE_EXPORTED_INTERFACES = "service.exported.interfaces"
SERVICE_IMPORTED = "service.imported"
SERVICE_IMPORTED_CONFIGS = "service.imported.configs"

REGISTERED = 0
""" Isolate presence event: Isolate registered """

UNREGISTERED = 1
""" Isolate presence event: Isolate unregistered or lost """

# ------------------------------------------------------------------------------

class _JsonRpcServlet(SimpleJSONRPCDispatcher):
    """
    A JSON-RPC servlet, replacing the SimpelJSONRPCServer from jsonrpclib.
    """
    def __init__(self, encoding=None):
        """
        Constructor
        """
        SimpleJSONRPCDispatcher.__init__(self, encoding)


    def do_POST(self, handler):
        """
        Handle a post request
        
        :param handler: The basic RequestHandler that received the request
        """
        try:
            max_chunk_size = 10 * 1024 * 1024
            size_remaining = int(handler.headers["content-length"])
            chunks_list = []
            while size_remaining:
                chunk_size = min(size_remaining, max_chunk_size)
                chunks_list.append(handler.rfile.read(chunk_size).decode())
                size_remaining -= len(chunks_list[-1])
            data = ''.join(chunks_list)
            response = self._marshaled_dispatch(data)
            handler.send_response(200)

        except:
            handler.send_response(500)
            err_lines = traceback.format_exc().splitlines()
            trace_string = '%s | %s' % (err_lines[-3], err_lines[-1])
            fault = jsonrpclib.Fault(-32603, 'Server error: %s' % trace_string)
            response = fault.response()

        if response is None:
            response = ''

        # Convert response
        response = response.encode()

        handler.send_header("Content-type", "application/json-rpc")
        handler.send_header("Content-length", str(len(response)))
        handler.end_headers()
        handler.wfile.write(response)
        handler.wfile.flush()

# ------------------------------------------------------------------------------

@ComponentFactory("ServiceExporterFactory")
@Instantiate("ServiceExporter")
@Requires("sender", "org.psem2m.signals.ISignalBroadcaster")
@Requires("receiver", "org.psem2m.signals.ISignalReceiver")
@Requires("directory", "org.psem2m.signals.ISignalDirectory")
@Requires("http", "HttpService")
@Property("servlet_path", "jsonrpc.servlet.path", "/JSON-RPC")
class ServiceExporter(object):
    """
    PSEM2M Remote Services exporter
    """
    def __init__(self):
        """
        Constructor
        """
        # HTTP Service
        self.http = None
        self.servlet_path = None

        # The JSON-RPC servlet
        self.server = None

        # Bundle context
        self.context = None

        # Signal sender
        self.directory = None
        self.receiver = None
        self.sender = None

        # Exported services
        self._exported_references = []
        self._endpoints = {}
        self._registrations = {}


    def _dispatch(self, method, params):
        """
        Called by (xml|json)rpclib
        """
        found = None
        len_found = 0

        for name in self._endpoints:
            if len(name) > len_found and method.startswith(name + "."):
                # Better matching end point name (longer that previous one)
                found = name
                len_found = len(found)

        if found is None:
            # Method not found
            raise NameError("No end point found for %s" % method)

        # Get the method name. +1 for the trailing dot
        method_name = method[len_found + 1:]

        # Get the service
        svc = self._endpoints[found][1]

        # Convert parameters from Jabsorb
        converted_params = []
        for param in params:
            converted_params.append(from_jabsorb(param))

        result = getattr(svc, method_name)(*converted_params)

        # Transform result to Jabsorb
        return to_jabsorb(result)


    def _export_service(self, reference):
        """
        Exports the given service
        """
        if reference in self._exported_references:
            # Already exported service
            return

        # Compute the end point name
        endpoint_name = reference.get_property("endpoint.name")
        if endpoint_name is None:
            endpoint_name = "service_%d" \
                                % reference.get_property(pelix.SERVICE_ID)

        # Get the service
        try:
            svc = self.context.get_service(reference)
            if svc is None:
                _logger.error("Invalid service for reference %s",
                              str(reference))

        except pelix.BundleException:
            _logger.exception("Error retrieving the service to export")
            return

        # Register the reference and the service
        self._exported_references.append(reference)
        self._endpoints[endpoint_name] = (reference, svc)

        specifications = reference.get_property("objectClass")
        if isinstance(specifications, str):
            specifications = tuple([specifications])
        else:
            specifications = tuple(specifications)

        exported_config = reference.get_property("service.exported.configs")
        if not exported_config:
            exported_config = "*"

        # Get the service properties
        properties = reference.get_properties()

        # Generate the service ID
        isolate_id = self.directory.get_isolate_id()
        service_id = "%s.%s" % (isolate_id, properties.get(pelix.SERVICE_ID))

        # Create the registration map
        registration = {
            JAVA_CLASS: JAVA_REMOTE_SERVICE_REGISTRATION,
            "endpoints": ({
                    JAVA_CLASS: JAVA_ENDPOINT_DESCRIPTION,
                    "endpointName": endpoint_name,
                    "endpointUri": self.servlet_path,
                    "exportedConfig": exported_config,
                    "node": self.directory.get_local_node(),
                    "port": self.http.get_port(),
                    "protocol": "http"
                },),
            "exportedInterfaces": specifications,
            "hostIsolate": isolate_id,
            "serviceProperties": properties,
            "serviceId": service_id
        }

        # Store it
        self._registrations[reference] = registration

        # Send registration signal
        remote_event = {
            JAVA_CLASS: JAVA_REMOTE_SERVICE_EVENT,
            "eventType": {
                JAVA_CLASS: JAVA_SERVICE_EVENT_TYPE,
                "enumValue": "REGISTERED"
            },
            "serviceRegistration": registration
        }

        _logger.debug("> Export Service %s: %s", reference, properties)
        self.sender.fire(SIGNAL_REMOTE_EVENT, remote_event, dir_group="ALL")


    def _unexport_service(self, reference):
        """
        Stops the export of the given service
        """
        if reference not in self._exported_references:
            # Unknown reference
            return

        # Remove corresponding end points
        endpoints = [endpoint
                     for endpoint, svcref_svc in self._endpoints.items()
                     if svcref_svc[0] == reference]

        for endpoint in endpoints:
            del self._endpoints[endpoint]

        # Remove the reference from the list
        self._exported_references.remove(reference)

        # Pop the registration object
        registration = self._registrations.pop(reference)

        # Send signal
        remote_event = {
            JAVA_CLASS: JAVA_REMOTE_SERVICE_EVENT,
            "eventType": {
                JAVA_CLASS: JAVA_SERVICE_EVENT_TYPE,
                "enumValue": "UNREGISTERED"
            },
            "serviceRegistration": registration
        }

        _logger.debug("X Un-exported service %s: %s", reference,
                      reference.get_properties()[pelix.OBJECTCLASS])
        self.sender.fire(SIGNAL_REMOTE_EVENT, remote_event, dir_group="ALL")


    def service_changed(self, event):
        """
        Called when a service event is triggered
        """
        kind = event.get_type()
        ref = event.get_service_reference()

        if kind == pelix.ServiceEvent.REGISTERED or \
                (kind == pelix.ServiceEvent.MODIFIED \
                 and ref not in self._exported_references):
            # Matching registering or updated service
            self._export_service(ref)

        elif ref in self._exported_references and\
                (kind == pelix.ServiceEvent.UNREGISTERING or \
                 kind == pelix.ServiceEvent.MODIFIED_ENDMATCH):
            # Service is updated or unregistering
            self._unexport_service(ref)


    def handle_received_signal(self, name, signal_data):
        """
        Called when a remote services signal is received
        """
        if name != BROADCASTER_SIGNAL_REQUEST_ENDPOINTS:
            # Never happens... in theory...
            return

        sender = signal_data["senderId"]
        if sender == self.directory.get_isolate_id():
            # Ignore local events
            return

        events = [
          {
           JAVA_CLASS: JAVA_REMOTE_SERVICE_EVENT,
           "eventType": {
                         JAVA_CLASS: JAVA_SERVICE_EVENT_TYPE,
                         "enumValue": "REGISTERED"
                         },
           "serviceRegistration": registration
           } for registration in self._registrations.values()]

        _logger.debug("Got endpoints request from %s", sender)
        self.sender.fire(SIGNAL_REMOTE_EVENT, events, isolate=sender)


    @Validate
    def validate(self, context):
        """
        Component validated
        """
        # Store the bundle context
        self.context = context

        # Create the servlet
        self.server = _JsonRpcServlet()
        self.server.register_instance(self)

        # Register the servlet
        self.http.register_servlet(self.servlet_path, self.server)

        # Export existing services
        existing_ref = context.get_all_service_references(None,
                                                        EXPORTED_SERVICE_FILTER)

        if existing_ref is not None:
            for reference in existing_ref:
                self._export_service(reference)

        # Register a service listener, to update the exported services state
        context.add_service_listener(self, EXPORTED_SERVICE_FILTER)

        # Register to the REQUEST_ALL_ENDPOINTS signal
        self.receiver.register_listener(BROADCASTER_SIGNAL_REQUEST_ENDPOINTS,
                                        self)
        _logger.debug("ServiceExporter Ready")


    @Invalidate
    def invalidate(self, context):
        """
        Component invalidated
        """
        # Unregister to the REQUEST_ALL_ENDPOINTS signal
        self.receiver.unregister_listener(BROADCASTER_SIGNAL_REQUEST_ENDPOINTS,
                                          self)

        # Unregister the servlet
        self.http.unregister_servlet(self.server)
        self.server = None

        # Unregister the service listener
        context.remove_service_listener(self)

        # Remove all exports
        references_copy = self._exported_references[:]
        for reference in references_copy:
            self._unexport_service(reference)

        # Clean up the storage, to be sure of our state
        self._endpoints.clear()
        del self._exported_references[:]

        # Remove the reference to the context
        self.context = None
        _logger.debug("ServiceExporter Gone")

# ------------------------------------------------------------------------------

def _filter_export_properties(properties):
    """
    Filters imported service properties. Makes a new dictionary

    @param properties: Imported service properties
    @return: A filtered dictionary
    """
    if properties is None:
        return None

    result = {}

    # Remove export properties
    for key, value in properties.items():
        if not key.startswith(SERVICE_EXPORTED_PREFIX):
            result[key] = value

    # Add import properties
    result[SERVICE_IMPORTED] = "True"

    if SERVICE_EXPORTED_CONFIGS in properties:
        result[SERVICE_IMPORTED_CONFIGS] = properties[SERVICE_EXPORTED_CONFIGS]

    return result


class _JSONProxy(object):
    """
    JSON-RPC proxy
    """
    def __init__(self, endpoint, endpoint_name):
        """
        Constructor
        """
        self.server = jsonrpclib.Server(endpoint)
        self.endpoint = endpoint_name


    def __getattr__(self, name):
        """
        Proxy core
        """
        def wrapped_call(*args, **kwargs):
            """
            Wrapped call
            """
            method_name = "%s.%s" % (self.endpoint, name)
            method = self.server.__getattr__(method_name)
            if args:
                args = [to_jabsorb(arg) for arg in args]

            if kwargs:
                kwargs = dict([(key, to_jabsorb(value))
                               for key, value in kwargs.items()])

            result = method(*args, **kwargs)
            return from_jabsorb(result)
            # FIXME: in case of Exception, look if the service has gone

        return wrapped_call


    def __stop__(self):
        """
        Stops the proxy
        """
        self.server = None
        self.endpoint = None


@ComponentFactory("ServiceImporterFactory")
@Instantiate("ServiceImporter")
@Requires("directory", "org.psem2m.signals.ISignalDirectory")
@Requires("receiver", "org.psem2m.signals.ISignalReceiver")
@Requires("sender", "org.psem2m.signals.ISignalBroadcaster")
@Provides("org.psem2m.isolates.services.monitoring.IIsolatePresenceListener")
class ServiceImporter(object):
    """
    PSEM2M Remote Services importer
    """
    def __init__(self):
        """
        Constructor
        """
        # Dependencies
        self.directory = None
        self.sender = None
        self.receiver = None

        # Bundle context
        self.context = None

        # Isolate ID -> [Service ID]
        self._services = {}

        # Service ID -> (proxy, reference)
        self._registered_services = {}
        self._registry_lock = threading.RLock()


    def handle_isolate_presence(self, isolate_id, isolate_node, event):
        """
        Called when an isolate appears or disappears
        
        :param isolate_id: ID of the isolate
        :param isolate_node: Node of the isolate
        :param event: Kind of event
        """
        if event == REGISTERED:
            # Isolate registered: ask for its end points
            self.sender.fire(BROADCASTER_SIGNAL_REQUEST_ENDPOINTS, None,
                             isolate=isolate_id)

        elif event == UNREGISTERED:
            # Isolate lost
            self._handle_isolate_lost(isolate_id)


    def handle_received_signal(self, name, signal_data):
        """
        Called when a remote services signal is received
        """
        sender = signal_data["senderId"]
        if sender == self.directory.get_isolate_id():
            # Ignore local events
            return

        # Get the raw signal content
        data = signal_data["signalContent"]

        if name == SIGNAL_REMOTE_EVENT:
            # Remote service event
            if isinstance(data, list):
                # Multiple events in one signal
                for event in data:
                    try:
                        self._handle_remote_event(sender, event)
                    except:
                        _logger.exception("Error reading RemoteServiceEvent")

            else:
                # Single event
                self._handle_remote_event(sender, data)


    def _handle_remote_event(self, sender, remote_event):
        """
        Handle a remote service event.

        @param remote_event: Raw remote service event dictionary
        """
        try:
            event_type = remote_event["eventType"]["enumValue"]
        except:
            _logger.exception("Invalid RemoteEvent object\n%s", remote_event)
            return

        if event_type == "REGISTERED":
            self._import_service(remote_event)

        elif event_type == "UNREGISTERED":
            self._unimport_service(sender,
                            remote_event["serviceRegistration"]["serviceId"])


    def _handle_isolate_lost(self, isolate_name):
        """
        Handle an isolate lost signal

        @param isolate_name: Name of the lost isolate
        """
        services = self._services.get(isolate_name, None)
        if not services:
            # No service registered for this isolate
            return

        services_copy = services[:]
        for service_id in services_copy:
            # Cancel the import
            self._unimport_service(isolate_name, service_id)

        # Clean up the isolate services list
        del services[:]


    def _import_service(self, remote_event):
        """
        Import the given remote service
        """
        remote_reg = remote_event["serviceRegistration"]

        # Store remote service ID
        service_id = remote_reg["serviceId"]

        # Store the host isolate ID
        host_isolate = remote_reg["hostIsolate"]

        with self._registry_lock:
            if service_id in self._registered_services:
                # Already registered service
                _logger.debug("Service ID already registered")
                return

            # TODO: add service filter

            # Select the end point
            endpoints = remote_reg["endpoints"]
            if not endpoints:
                # No end point
                _logger.error("No end point given")
                return

            # TODO: select it upon available proxies
            endpoint = endpoints[0]

            # Extract end point information
            protocol = endpoint.get("protocol", "http")
            node = endpoint.get("node", self.directory.get_local_node())
            port = int(endpoint.get("port", 80))
            uri = endpoint.get("endpointUri", "/")
            endpoint_name = endpoint.get("endpointName", service_id)

            # Resolve the host
            host = self.directory.get_host_for_node(node)

            # Create the proxy (select the factory according to export config)
            endpoint_url = "%s://%s:%d%s" % (protocol, host, port, uri)
            proxy = _JSONProxy(endpoint_url, endpoint_name)
            if proxy is None:
                _logger.error("Remote service proxy not created")
                return

            # Filter properties
            properties = _filter_export_properties(\
                                                remote_reg["serviceProperties"])
            properties["service.imported.from"] = host_isolate

            # Register the service
            try:
                reg = self.context.register_service(
                            remote_reg["exportedInterfaces"], proxy, properties)

            except pelix.BundleException:
                _logger.exception("Error registering the proxy service")
                reg = None

            if reg is None:
                # Error creating the proxy
                proxy.__stop__()
                return

            # Store information
            self._registered_services[service_id] = (proxy, reg)

            services = self._services.get(host_isolate, None)
            if services is None:
                services = []
                self._services[host_isolate] = services

            services.append(service_id)

            _logger.debug("< Imported Service from %s: %s", host_isolate,
                          remote_reg["exportedInterfaces"])


    def _unimport_service(self, isolate_name, service_id):
        """
        Cancel the import of the given service
        """
        with self._registry_lock:
            service = self._registered_services.get(service_id, None)
            if not service:
                _logger.debug("Unknown service - %s", service_id)
                # Unknown service
                return

            # Delete the references to this service
            del self._registered_services[service_id]

            # Extract service registration and proxy instance
            proxy, reg = service

            try:
                # Unregister the service from Pelix
                reg.unregister()

            except pelix.BundleException:
                _logger.exception("Error unregistering the imported service %s",
                                  service_id)

            except:
                _logger.exception("UNHANDLED EXCEPTION")
                raise

            # Stop the proxy
            proxy.__stop__()

            isolate_services = self._services.get(isolate_name, None)
            if isolate_services is not None and service_id in isolate_services:
                isolate_services.remove(service_id)

            _logger.debug("X UNimported service from %s: %s", isolate_name,
                          reg.get_reference().get_properties()\
                          .get(pelix.OBJECTCLASS))


    @Validate
    def validate(self, context):
        """
        Component validated
        """
        self.context = context

        # Clean up a little, just in case
        self._services.clear()
        self._registered_services.clear()

        # Register remote services signals
        self.receiver.register_listener(BROADCASTER_SIGNAL_NAME_PREFIX + "/*",
                                        self)

        # Send "request endpoints" signal
        _logger.debug("Sending endpoints request...")
        self.sender.fire(BROADCASTER_SIGNAL_REQUEST_ENDPOINTS, None,
                         dir_group="ALL")
        _logger.debug("ServiceImporter Ready")


    @Invalidate
    def invalidate(self, context):
        """
        Component invalidated
        """
        # Unregister remote services signals
        self.receiver.unregister_listener(BROADCASTER_SIGNAL_NAME_PREFIX + "/*",
                                          self)

        # Unregister imported services (in a single loop)
        with self._registry_lock:
            for imported_service in self._registered_services.values():
                proxy, reg = imported_service
                try:
                    # Unregister the service
                    reg.unregister()

                except pelix.BundleException:
                    _logger.exception("Error unregistering remote service")

                if proxy is not None:
                    # Stop the proxy
                    proxy.__stop__()

        # Clean up the collections
        self._services.clear()
        self._registered_services.clear()

        # Forget the context
        self.context = None
        _logger.debug("ServiceImporter Gone")
