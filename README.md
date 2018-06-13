# commons-profiler
[![Maven Central](https://img.shields.io/maven-central/v/ru.fix/commons-profiler-api.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22ru.fix%22)

Aggregating Profiler provide basic API for application metrics measurement.

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
Profiler profiler = new SimpleProfiler();
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
* latency 
* 
*
 - latency - time between two points: when method invoked and when method completed);
 - throughput - how often method was invoked (invocation per second)
 - max throughput - we made measurement  during 2 minutes and want to find point in time 
 where throughput reached its maximum
 - callsCount - how many time method was invoked 

### Indicator metrics  
Here is list of metrics that Profiler will request and flush to external storage for each Indicator:

Indicators:
* value
* 
* 



## Metric reporting
How to register Profiler Reporter and start to record metrics to external storage.

## How to build this project
```
gradle clean build
```

## Other tracing projects
There are several projects for tracing that you can take a look to:

OpenTracing:  
https://github.com/opentracing/opentracing-java  

HTrace:  
https://github.com/apache/incubator-htrace  

Zipkin:  
https://github.com/openzipkin/zipkin/

Dropwizard metrics:
https://github.com/dropwizard/metrics