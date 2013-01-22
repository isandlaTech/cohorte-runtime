/**
 * Common Java configuration: HTTP / JSON bundles
 */
{
	/*
	 * Java HTTP bundles
	 */
	"bundles" : [
	/* Apache Felix HTTP Bundle */
	{
		"name" : "org.apache.felix.http.bundle"
	},

	/* PSEM2M HTTP Signals */
	{
		"name" : "org.psem2m.signals.http"
	},

	/* JSON Support */
	{
		"name" : "org.jabsorb.ng"
	}, {
		"name" : "org.psem2m.signals.serializer.json"
	}, {
		"name" : "org.psem2m.remote.jsonrpc"
	} ],

	/*
	 * Components
	 */
	"composition" : [
	/* HTTP Signals */
	{
		"factory" : "psem2m-signals-sender-http-factory",
		"name" : "psem2m-signals-sender-http"
	}, {
		"factory" : "psem2m-signals-receiver-http-factory",
		"name" : "psem2m-signals-receiver-http"
	},

	/* JSON Serializer */
	{
		"factory" : "psem2m-signals-data-json-factory",
		"name" : "psem2m-signals-data-json"
	},

	/* JSON Remote Services */
	{
		"factory" : "psem2m-remote-endpoint-jsonrpc-factory",
		"name" : "psem2m-remote-endpoint-jsonrpc"
	}, {
		"factory" : "psem2m-remote-client-jsonrpc-factory",
		"name" : "psem2m-remote-client-jsonrpc"
	} ],

	/*
	 * Properties
	 */
	"properties" : {
		// Activate Jetty
		"org.apache.felix.http.jettyEnabled" : "true"
	}
}