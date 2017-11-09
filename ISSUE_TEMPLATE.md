⚠️ stagemonitor is not working as expected? We want to help you. Please provide the following information:

* information about your environment (standalone application, servlet container, application server, ...)
* any information that enable us to reproduce the problem
* The optimum is if you have a small demo application that shows the problem.
* the startup logs, they look like this:

```
# stagemonitor status
System information: Java 1.8.0_111 (Oracle Corporation) Mac OS X 10.12.6
OK   - Agent attachment 
OK   - AlertingPlugin (version 0.83.0-SNAPSHOT)
OK   - CorePlugin (version 0.83.0-SNAPSHOT)
OK   - EhCachePlugin (version 0.83.0-SNAPSHOT)
FAIL - Elasticsearch (Elasticsearch is not available)
OK   - ElasticsearchTracingPlugin (version 0.83.0-SNAPSHOT)
OK   - JdbcPlugin (version 0.83.0-SNAPSHOT)
OK   - JvmPlugin (version 0.83.0-SNAPSHOT)
OK   - LoggingPlugin (version 0.83.0-SNAPSHOT)
OK   - OsPlugin (version 0.83.0-SNAPSHOT)
OK   - ServletPlugin (version 0.83.0-SNAPSHOT)
OK   - SoapTracingPlugin (version 0.83.0-SNAPSHOT)
OK   - Startup 
OK   - TracingPlugin (version 0.83.0-SNAPSHOT)
# stagemonitor configuration, listing non-default values:
stagemonitor.requestmonitor.http.parseUserAgent: true (source: stagemonitor.properties)
stagemonitor.web.paths.excluded: /resources, /webjars, /dandelion (source: stagemonitor.properties)
stagemonitor.eum.enabled: true (source: stagemonitor.properties)
stagemonitor.reporting.interval.elasticsearch: 10 (source: stagemonitor.properties)
stagemonitor.reporting.elasticsearch.url: http://localhost:9200 (source: stagemonitor.properties)
stagemonitor.instrument.exclude: org.springframework.samples.petclinic.model (source: stagemonitor.properties)
stagemonitor.instrument.include: org.springframework.samples.petclinic (source: stagemonitor.properties)
stagemonitor.grafana.apiKey: XXXX (source: stagemonitor.properties)
```
