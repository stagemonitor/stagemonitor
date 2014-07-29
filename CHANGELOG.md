# Features
 * A version number is now appended to each dashboard, so that you can migrate to the new dashboards more easily
 * Integrated p6spy to monitor SQL statements
  * SQL statements are displayed in the call stack (optinally with parameters)
  * The request traces now include executionTimeDb and executionCountDb
  * DB Query dashboard that displays the statistical distribution of SQL statement execution times and the number of
    queries executed per second. It also lets you easily identify the slowest and most frequent SQL statements as well
    as the requests that issue the most SQL statements.
 * Dynamically update properties via query parameters (?<config-key>=<config-value>).
  * Set a password to protect configuration updates. If the password is not set, dynamic updates are disabled.
    If the password is set to an empty string, the password is not required for configuration changes.
 * Reload configuration (including changes in stagemonitor.properties) with stagemonitorReloadConfig query parameter.
 * Tracking how many servers are online. This information is displayed in the server dashboard.
 * Introduced KPIs over time dashboard. Shows how the most important metrics (Key Performance Indicators) are
   behaving now, 1 week ago and 4 weeks ago.
 * Support for custom configuration sources

# Breaking Changes
 * the graphite metric paths for requests have changed, so you won't be able to see old request metrics in the new
   dashboard
 * grafana >= 1.6.1 is required

# Dependency Updates
  * updated dependencies to jackson, uadetector
  * new dependency to p6spy