{
    "id":"isolate-laptop-ui",
    "kind":"felix",
    "node":"central",
    "httpPort":9200,
	"vmArgs":[
          "-Xms32M",
          "-Xmx64M",
          "-XX:+UnlockDiagnosticVMOptions",
          "-XX:+UnsyncloadClass"
    ],
    "bundles":[
        {
            "symbolicName":"org.psem2m.composer.api"
        },
        {
            "symbolicName":"org.psem2m.demo.july2012.api"
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
            "symbolicName":"org.psem2m.isolates.ui.admin",
            "optional":true,
            "properties":{
                "psem2m.demo.ui.viewer.top":"0scr",
                "psem2m.demo.ui.viewer.left":"0scr",
                "psem2m.demo.ui.viewer.width":"0.50scr",
                "psem2m.demo.ui.viewer.height":"0.66scr",
                "psem2m.demo.ui.viewer.color":"SkyBlue"
            }
        },
        {
            "symbolicName":"org.psem2m.composer.ui"
        },
        {
            "symbolicName":"org.psem2m.isolates.ui"
        }
    ]
},