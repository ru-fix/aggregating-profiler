# aggregating-profiler
[![Maven Central](https://img.shields.io/maven-central/v/ru.fix/aggregating-profiler.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22ru.fix%22)

Aggregating Profiler provide basic API for application metrics measurement.

## Motivation 

Profiler records metrics and accumulates them in memory. 
Then profiler flushes aggregated values to external store. 
It could be timeseries databse like Graphite, OpenTSDB, InfluxDB or Prometheus or relation database like PostgreSql.
You can tune time span for aggregation and flushing rate.

This approach with pre-aggregation metrics in memory allows profiler to record huge amount of measurements 
and do not depend on storage performance.     

Profiler consist of two parts: 
* Metric recording API to trace events 
* Metric reporting API to flush metrics aggregates to external storage system

## Metric recording

### ProfiledCall
Suppose that we want to trace how much time it takes for SmartService to compute and return result. 

Sync service measurement: 
```java
// Init single profiler instance per application 
Profiler profiler = new AggregatingProfiler();
...

// Create profiled call each time you want to make measurement 
ProfiledCall call = profiler.profiledCall("smart.service");
call.start();

SmartResult result = smartService.doSmartComputation();

call.stop();
```
or short version

```java
ProfiledCall call = profiler.start("smart.service");

SmartResult result = smartService.doSmartComputation();

call.stop();
```
or even shorter
```java
SmartResult result = profiler.profile(
        "smart.service",
        () -> smartService.doSmartComputation());
```

Async service measurement: 
```java
ProfiledCall call = profiler.start("smart.service");

CompletableFuture<SmartResult> result = smartService.doSmartComputation();

result.whenComplete((exc, result)->{ 
    call.stop();
});
```
or short version
```java

CompletableFuture<SmartResult> result = profiler.profileFuture(
        "smart.service",
         () -> smartService.doSmartComputation());
```

In real life application you can use custom aspect to wrap method invocation, etc.

### ProfiledCall with payload
Some times method invocation is not sufficient and you need to record information about payload 
of the method. Profiler API provide a way to register integer value of custom method payload: 

```java
ProfiledCall call = profiler.start("batching.consumer");

Entry[] processedData = consumer.processData();

call.stop(processedData.size);
```

### Indicators
Suppose you want to measure current state of the system, like how many pending request are in you buffer.
In such cases `indicator` tracing could be very handy.
```java
//During service initialization
profiler.attachIndicator("buffer.size", () -> buffer.size());

//During service destruction
profiler.dettachIndicator("buffer.size");
```
When you are attaching indicator you are saving lambda within profiler. 
Each time profiler builds report and flushing it into external storage 
such lambdas will be used to gather indicators values.


### ProfiledCall metrics  
Here is list of metrics that Profiler will accumulate and flush to external storage for each ProfiledCall:
 
ProfiledCalls:
 - name - dot separated metric name
 - reportingTimeAvg - reporting interval in milliseconds
 - latency - time in milliseconds between two points: profiledCall start and stop
   - latencyMax maximum latency
   - latencyMin minimum latency
   - latencyAvg average latency
 - callsCountSum - how many times profiledCall was invoked
 - callsThroughputAvg - average rate of profiledCall milli invocation per second 
 (123 means that there was 0.123 invocation per second)
 - payload - payload provided via stop method of profiledCall
   - payloadMin - min value of payload
   - payloadMax - max value of payload
   - payloadAvg - avg value of payload
   - payloadSum - total sum of payload provided within reporting interval
   - payloadThroughputAvg - payload rate milli invocation per second
   (123 means that there was 0.123 invocation per second)
 - throughputPerSecondMax - maximum rate within second time interval that was achieved during reporting period 
 (17 means that there was 17 invocation within 1 second interval)
 - activeCalls - calls that are still running at the end of reporting period
   - activeCallsCountMax - count of active calls that still running at the end of reporting period  
   - activeCallsLatencyMax - maximum latency of active call

### Indicator metrics  
Here is list of metrics that Profiler will request and flush to external storage for each Indicator:

Indicators:
- name - dot separated metric name
- indicatorMax - instant value of indicator that was reported during report building at the end of reporting period
 
### Metric aggregation in metric store
User can provide different storages for metrics: graphite, influx, prometheus, opentsdb. 
All metrics names ends with suffixes: min, max, sum, avg. 
This suffix could be used as a suggestion to specify how storage could compress cold data.   


## Metric reporting
How to register Profiler Reporter and start to record metrics to external storage.

## How to mock profiler in Tests
`NoopProfiler` is a stub that you can use as a dependency in tests. This stub does not do anything.
```kotlin
val service = MyService(NoopProfiler() )

```
## How to build this project
Build: 
```
gradle clean build
```
Run jmh tests:
```
gradle clean build -xtest jmh --info --no-daemon
```

## Other tracing projects
There are several projects for tracing that you can take a look:

OpenTracing:  
https://github.com/opentracing/opentracing-java  

HTrace:  
https://github.com/apache/incubator-htrace  

Zipkin:  
https://github.com/openzipkin/zipkin/

Dropwizard metrics:  
https://github.com/dropwizard/metrics