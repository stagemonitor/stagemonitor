# EhCache Performance Dashboard

This dashboard contains metrics about [EhCache](http://www.ehcache.org/).

## Cache Hit Rate (worst 10)
This panel contains cache names and their hit rate.
It is important to let the application and caches "warm up" before deciding if caching improves the response time of the application.
A low hit rate is a possible indicator for:
* too early evaluation of cache usefulness (use warm caches!)
* too frequent invalidation of cache entries
* too many cache entries
* too small caches
* too short cache entry lifetime
* too infrequent access of cache entries

## Cache Size
This panel contains the 10 biggest caches by heap size.

## Gets per second (highest 10)
This panel contains the 10 most frequent accessed caches.

## Expires per second (highest 10)
This panel contains the 10 caches with the highest expiration rate.

