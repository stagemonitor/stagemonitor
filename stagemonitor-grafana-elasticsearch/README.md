This app bundles dashboards for the open source Java monitoring tool [stagemonitor](http://www.stagemonitor.org/).

# Dashboards
 * Request Response Time Metrics
 * Application Server
 * Custom stagemonitor Metrics
 * EhCache
 * External Requests (for example JDBC)
 * Host
 * JVM
 * Logging (for example Log4j, slf4j, ...)

# Requirements
 * [Elasticsearch](https://www.elastic.co/products/elasticsearch) v2+
 * [stagemonitor](http://www.stagemonitor.org/)

# Setup

## Add Datasource
In the config tab of the app, enter the Elasticsearch url and your stagemonitor reporting interval and click the `Enable` button.
The app then automatically creates a Elasticsearch datasource named `ES stagemonitor` for you.

## Import dashboards
Select the dashboards tab and import the dashboards you are interested in.

## Install stagemonitor
Follow the installation steps in the stagemonitor wiki: <https://github.com/stagemonitor/stagemonitor/wiki/Installation>
