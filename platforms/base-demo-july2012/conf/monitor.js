{
    "id":"org.psem2m.internals.isolates.monitor-1",
    "kind":"felix",
    "node":"stratus",
    "httpPort":9000,
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
            "symbolicName":"org.psem2m.forker.api"
        },
        {
            "symbolicName":"org.psem2m.forkers.aggregator"
        },
        {
            "from":"remote-services.js",
            "overriddenProperties":{
                "org.psem2m.remote.filters.exclude":"*.demo.*"
            }
        },
        {
            "symbolicName":"org.psem2m.isolates.monitor"
        },
        {
            "symbolicName":"org.psem2m.composer.api"
        },
        {
            "symbolicName":"org.psem2m.composer.config"
        },
        {
            "symbolicName":"org.psem2m.composer.core"
        },
        {
            "symbolicName":"org.apache.felix.shell"
        },
        {
            "symbolicName":"org.apache.felix.shell.remote",
            "properties":{
                "osgi.shell.telnet.port":"6000"
            }
        }
    ]
}