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

## License

Copyright © 2016 Johannes Staffans

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

[lein]: http://leiningen.org/
