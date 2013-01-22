/**
 * Start configuration for the monitor (Java configuration read by Python)
 */
{
	/*
	 * Common files
	 */
	"import-files" : [ "java-common.js", "java-common-signals.js",
			"java-common-remote.js", "java-common-http.js" ],

	/*
	 * Java bundles
	 */
	"bundles" : [ {
		"name" : "org.psem2m.isolates.slave.agent"
	} ],

	/*
	 * Components
	 */
	"composition" : [
	/* Configuration of common components */
	{
		// Slave Agent: provides JMX methods to handle bundles
		"factory" : "psem2m-slave-agent-core-factory",
		"name" : "psem2m-slave-agent-core"
	} ],

	/*
	 * Properties
	 */
	"properties" : {
		// HTTP port
		"org.osgi.service.http.port" : 9000
	}
}