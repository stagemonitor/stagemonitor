# JDBC Dashboard
This dashboard contains various information about the database connection.

## JDBC Response Time
This panel aggregates the response time over all database queries.
To see the response time of an individual database query, choose the one you are interested in in the Signature dropdown.
The database queries are named after the Java method which initiated the query.

## Average Queries / Second
The average query count per second.

## Slowest Queries (p75)
This panel shows the 75th percentile of all database queries.
The database queries are named after the Java method which initiated the query.
To optimize the performance of these queries, you could analyze the execution plan of the query to see if it uses
expensive full table scans or if an index would be beneficial.

## Requests with most Queries/sec
This panel shows the endpoints (aka. Use Cases or Business Transactions) with the highest query count.
In order to optimize the performance of these Use Cases, you should try to make fewer calls to the database, for example with batch loading.
A high query count might also indicate a classical [n+1 Hibernate problem](http://stackoverflow.com/questions/97197/what-is-the-n1-selects-issue).

## Time to get connection (p95)
A spike in the time to get a connection usually means that the connection pool is exhausted. 
It could also indicate network problems or an overloaded database server.

## Most Frequent Queries
The most frequent queries. Slow but frequent queries may have the potential to benefit from caching.
