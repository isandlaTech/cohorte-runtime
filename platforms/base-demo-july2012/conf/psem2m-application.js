{
    "appId":"demo-app",
    "multicast":"239.0.0.1",
    "isolates":[
        {
            "from":"monitor.js"
        },
        {
            "from":"forker.js"
        },
        {
            "id":"demo.central.ui",
            "kind":"felix",
            "node":"central",
            "httpPort":9099,
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
                        "psem2m.demo.ui.viewer.left":"0.25scr",
                        "psem2m.demo.ui.viewer.width":"0.25scr",
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
        {
            "id":"demo.stratus.aggregator",
            "kind":"pelix",
            "node":"stratus",
            "httpPort":9100,
            "bundles":[
				{
				    "symbolicName":"pelix.shell.core"
				},
				{
					"symbolicName":"pelix.shell.remote"
				},
				{
					"symbolicName":"pelix.shell.ipopo"
				},
				{
					"symbolicName":"base.psem2m_shell"
				},
                {
                    "symbolicName":"base.httpsvc"
                },
                {
                    "symbolicName":"base.signals.directory"
                },
                {
                    "symbolicName":"base.signals.directory_updater"
                },
                {
                    "symbolicName":"base.signals.http"
                },
                {
                    "symbolicName":"base.remoteservices"
                },
                {
                    "symbolicName":"base.composer"
                },
                {
                    "symbolicName":"demo.aggregator"
                },
                {
                    "symbolicName":"demo.aggregator_ui"
                }
            ],
            "environment":{
                "PYTHONPATH":"/home/tcalmant/programmation/workspaces/psem2m/demos/demo-july2012/demo.july2012.python",
                "shell.port":"4200"
            }
        },
        {
            "id":"demo.temper",
            "kind":"pelix",
            "node":"stratus",
            "httpPort":9101,
            "bundles":[
                {
                    "symbolicName":"pelix.shell.core"
                },
                {
                    "symbolicName":"pelix.shell.remote"
                },
                {
                    "symbolicName":"pelix.shell.ipopo"
                },
                {
					"symbolicName":"base.psem2m_shell"
				},
                {
                    "symbolicName":"base.composer"
                },
                {
                    "symbolicName":"base.httpsvc"
                },
                {
                    "symbolicName":"base.signals.directory"
                },
                {
                    "symbolicName":"base.signals.directory_updater"
                },
                {
                    "symbolicName":"base.signals.http"
                },
                {
                    "symbolicName":"base.remoteservices"
                },
                {
                    "symbolicName":"demo.temperature"
                }
            ],
            "environment":{
                "PYTHONPATH":"/home/tcalmant/programmation/workspaces/psem2m/demos/demo-july2012/demo.july2012.python",
                "shell.port":"4201"
            }
        },
        {
            "id":"demo.temper-2",
            "kind":"pelix",
            "node":"stratus",
            "httpPort":9102,
            "bundles":[
				{
				    "symbolicName":"pelix.shell.core"
				},
				{
				    "symbolicName":"pelix.shell.remote"
				},
				{
				    "symbolicName":"pelix.shell.ipopo"
				},
				{
					"symbolicName":"base.psem2m_shell"
				},
                {
                    "symbolicName":"base.httpsvc"
                },
                {
                    "symbolicName":"base.signals.directory"
                },
                {
                    "symbolicName":"base.signals.directory_updater"
                },
                {
                    "symbolicName":"base.signals.http"
                },
                {
                    "symbolicName":"base.remoteservices"
                },
                {
                    "symbolicName":"base.composer"
                },
                {
                    "symbolicName":"demo.temperature"
                }
            ],
            "environment":{
                "PYTHONPATH":"/home/tcalmant/programmation/workspaces/psem2m/demos/demo-july2012/demo.july2012.python",
                "shell.port":"4202"
            }
        },
        {
        	"id":"demo.temper-java",
            "kind":"felix",
            "node":"central",
            "httpPort":9103,
            "vmArgs":[
                  "-Xms32M",
	              "-Xmx64M",
	              "-XX:+UnlockDiagnosticVMOptions",
	              "-XX:+UnsyncloadClass"
	        ],
            "bundles":[
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
                    "symbolicName":"org.psem2m.composer.agent"
                },
		        {
		        	"symbolicName":"org.psem2m.demo.july2012.api"
		        },
		        {
		        	"symbolicName":"org.psem2m.demo.july2012.impl"
		        }
            ]
        }
    ]
}