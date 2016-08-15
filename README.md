# foo-proxy

A proxy for a simple TCP protocol that records certain message-level metrics.
Uses Java's non-blocking I/O building blocks in the `java.nio` package.

## Usage

Prerequisites: the [Leiningen][lein] build tool, and Java >= 7.

```
$> lein uberjar
$> java -Dlisten=8002 -Dforward=localhost:8001 -jar target/foo-proxy-0.1.0-SNAPSHOT-standalone.jar
```

Metrics can be requested by sending SIGUSR2 to the JVM process:

```
$> kill -s USR2 $(jps | grep foo-proxy | awk '{ print $1 }')
```
The JVM process will dump metrics to stdout.

SIGUSR1 was not used because it is reserved by the JVM and cannot be used in applications.

## Todo

There is one flaw in the metrics processing: for sliding window metrics,
requests are kept in a deque, which is never cleaned up. This will sooner or
later lead to out-of-memory errors. 

The deque should be cleaned up periodically, deleting events that are no longer needed.

As a stop-gap measure for testing over a longer period of time, the heap size can simply
be increased:

```
java -Xmx=1024m ... 
``` 

## License

Copyright © 2016 Johannes Staffans

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

[lein]: http://leiningen.org/
