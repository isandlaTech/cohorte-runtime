{
    "id":"isolate-dataserver",
    "kind":"felix",
    "node":"central",
    "httpPort":9210,
    "vmArgs":[
        "-Xms64M",
        "-Xmx96M"
    ],
    "bundles":[
        {
            "symbolicName":"org.psem2m.isolates.ui.admin",
            "optional":true,
            "properties":{
                "psem2m.demo.ui.viewer.top":"0scr",
                "psem2m.demo.ui.viewer.left":"0.25scr",
                "psem2m.demo.ui.viewer.width":"0.25scr",
                "psem2m.demo.ui.viewer.height":"0.66scr",
                "psem2m.demo.ui.viewer.color":"YellowGreen"
            }
        },
        {
            "symbolicName":"org.apache.felix.shell"
        },
        {
            "symbolicName":"org.apache.felix.shell.remote",
            "properties":{
                "osgi.shell.telnet.port":"6002"
            }
        },
        {
            "from":"signals-http.js"
        },
        {
            "from":"jsonrpc.js"
        },
        {
            "from":"remote-services.js"
        },
        {
            "symbolicName":"org.psem2m.composer.api"
        },
        {
            "symbolicName":"org.psem2m.composer.agent"
        },
        {
            "symbolicName":"org.psem2m.composer.demo.api"
        },
        {
            "symbolicName":"org.psem2m.composer.demo.dataserver"
        }
    ]
}