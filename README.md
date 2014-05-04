Stage Monitor [![Build Status](https://travis-ci.org/stagemonitor/stagemonitor.svg?branch=master)](https://travis-ci.org/stagemonitor/stagemonitor) [![Coverage Status](https://coveralls.io/repos/stagemonitor/stagemonitor/badge.png?branch=master)](https://coveralls.io/r/stagemonitor/stagemonitor?branch=master)
=================

Stagemonitor is a Java monitoring agent that tightly integrates with the timeseries backend [Graphite](http://graphite.readthedocs.org/en/latest/overview.html), the Graphite dashboard [Grafana](http://grafana.org/) to analyze graphed metrics and [Kibana](http://www.elasticsearch.org/overview/kibana/) to analyze requests and call stacks. It includes preconfigured Grafana and Kibana dashboards that can be customized.

## Features
### Analyze Requests and Call Stacks
![image](https://cloud.githubusercontent.com/assets/2163464/2870547/8c15a186-d2ce-11e3-9906-ece6e9ee503b.png)

### Collected Metrics
 * HTTP Request Metrics
   * Statistical distribution of response times
   * Throughput
   * Requests are grouped by use cases. Usecases are automatically detected if you use Spring MVC. For other technologies, you can group URLs by regular expressions.
   * Call Stacks (aka. Call Trees) analyze the hierarchy of method calls to identify hotspots in your application.
   * Errors
 * JVM Metrics
   * Utilisation of all memory pools
   * CPU utilisation of the JVM process
 * Server Thread Pools
 * Application Thread Pools
 * JDBC Connection Pools
 
 
### Analyze historical data
Because all metrics are persisted in Graphite, you can access a long history of the metrics. This helps you to detect regressions in your applications and to analyze the interactions of the metrics if your server crashed.

### Support for cluster environments and multiple applications
In each dashboard, you can select the application, instance and host you want to see the metrics of. If you don't select a instance and a host, you get aggregated metrics.

### Track your own metrics
Stagemonitor includes the awesome [Metrics](http://metrics.codahale.com/) library. You can use it to track additional metrics.

### Write your own Dashboards
All dashboards are customizable - you can add new dashboards, rows and panels to explore the available metrics or your custom ones.

### Low Overhead
The evaluation of a JMeter loadtest of Stagemonitor integrated in [Spring Petclinic](https://github.com/stagemonitor/spring-petclinic) resulted in a reduction in throughput of only 0.2%.
The overhead of the profiler is about 30ns and 64byte per method. You can measure it in your own environment by executing the benchmarks in stagemonitor-benchmark.
