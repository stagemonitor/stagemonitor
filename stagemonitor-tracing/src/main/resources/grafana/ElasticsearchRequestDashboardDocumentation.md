# Request Metrics dashboard

This dashboards allows the observation of the applications response timing.

To analyze the individual requests go to the corresponding Kibana dashboard: <http://your-kibana-host:5601/app/kibana#/dashboard/Request-Analysis>.

## Response Time
This panel contains the percentiles of all response timings.
The p99 percentile is the time, below which 99% of the request times will be.
Therefore can a strong increase from p95 to p99 indicate that most of the application performs good with the exception of a few slow endpoints.
A lower response time is generally desired.

## Throughput by status
This panel groups the requests by the HTTP status code.
A high count of everything expect 2xx and 3xx could be an indicator for application logic errors.

## Slowest Requests (Median)
This panel groups the slowest requests for the median of all requests.
Frequently accessed slow endpoints may provide potential for optimization.

## Highest Throughput
This panel groups the fastest endpoints.

## Slowest Requests (p95)
This panel groups the slowest requests for the 95th percentile of all requests.
Frequently accessed slow endpoints may provide potential for optimization.
You can use the stagemonitor Kibana Dashboard for a more in-depth analysis of the slow response time cause.

## Most Errors
This panel contains the endpoints with the highest error count.
A high error count is an indicator for application logic errors.
You can use the stagemonitor Kibana Dashboard for a more in-depth analysis of the error cause.

## Page Load Time Breakdown
This panel summarizes the load time from the perspective of web page visitors.

* Dom Processing: The DOM Processing time is the time between the arrival of the first byte in the browser until the DOM is ready.
* Network Time: The network time is the time from requesting a page until the first byte arrived minus the server processing time.
* Page Rendering: The time between the DOM ready and load event.
* Server: The server time is the time it took the server to process the request.

By default, only aggregated page load times for all requests be collected. To see detailed page load times for each individual request group, you have to enable the configuration option [Collect Page Load Time data per request group](https://github.com/stagemonitor/stagemonitor/wiki/Configuration-Options#collect-page-load-time-data-per-request-group). 

