{
    "id":"org.psem2m.internals.isolates.monitor-1",
    "kind":"felix",
    "httpPort":9000,
    "vmArgs":[
        "-Xms32M",
        "-Xmx64M"
    ],
    "bundles":[
        {
            "symbolicName":"org.psem2m.isolates.ui.admin",
            "optional":true,
            "properties":{
                "psem2m.demo.ui.viewer.top":"0scr",
                "psem2m.demo.ui.viewer.left":"0scr",
                "psem2m.demo.ui.viewer.width":"0.25scr",
                "psem2m.demo.ui.viewer.height":"0.66scr",
                "psem2m.demo.ui.viewer.color":"SkyBlue"
            }
        },
        {
            "symbolicName":"org.psem2m.composer.ui"
        },
        {
            "symbolicName":"org.apache.felix.shell"
        },
        {
            "symbolicName":"org.apache.felix.shell.remote",
            "properties":{
                "osgi.shell.telnet.port":"6000"
            }
        },
        {
            "from":"signals-http.js"
        },
        {
            "from":"jsonrpc.js"
        },
        {
            "from":"remote-services.js",
            "overriddenProperties":{
                    "org.psem2m.remote.filters.exclude":"*.demo.*"
            }
        },
        {
            "symbolicName":"org.psem2m.isolates.master.manager"
        },
        {
            "symbolicName":"org.psem2m.isolates.monitor"
        },
        {
            "symbolicName":"org.psem2m.composer.api"
        },
        {
            "symbolicName":"org.psem2m.libs.xerces"
        },
        {
            "symbolicName":"org.psem2m.sca.converter"
        },
        {
            "symbolicName":"org.psem2m.composer.config"
        },
        {
            "symbolicName":"org.psem2m.composer.core"
        },
		{
		    "symbolicName":"org.psem2m.composer.demo.api"
		}
    ]
}