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

#### Example

There are several metrics that Profiler will accumulate and flush to external storage for each ProfiledCall invocation.  
Each metric have a name.

- name - dot separated metric name

![](docs/metric-example-start-stop.png?raw=true "Graph View")

In given example reporting period is 1 minute. That means that profiler aggregates information during 1 minute 
and then flushes it to storage. 
- reportingTimeAvg = 60_000

Profiler takes into consideration only metrics received from profiled calls that was closed
during reporting period.   
Profiled calls 1 was closed and reported in previous reporting period and will be ignored.   
Profiled calls #2, #3, #4, #5 will be reported. Their metrics will be used to build report.
Latency is the time in milliseconds between two points: profiledCall start and stop
  - latencyMax = 1000ms
  - latencyMin = 400ms
  - latencyAvg = (1000+1000+400+500) / 4 = 725
   
Total calls count will be 4.  
  - callsCountSum - 4 - how many times profiledCall was invoked

Profiled call #6 not started during current reporting period and will be ignored.
Profiled call #7 started during current reporting period but not stopped yet. 
It will not be used in latency and callsCount calculation but it will be used during activeCalls calculation.  
Profiled calls #7 and #8 started in current or previous reporting period and still running. 
They considered as activeCalls.  
Profiled call #8 will have maximum latency: 90000ms (96000 is total time, 
but at the end of reporting period duration of the profiled call #8 was only 90_000ms.).
Total count of active calls are 2.  
   - activeCallsCountMax - 2  
   - activeCallsLatencyMax - 90_000ms
 
![](docs/metric-example-throughput.png?raw=true "Graph View")

There are two metrics that measure throughput: callsThroughputAvg and throughputPerSecondMax.
In given example there was  9 invocations during 1 minute:   
9 / 60  =  0.15 invocation per second
- callsThroughputAvg - 0.15

During reporting period there was time then invocations occurred most often.  
We can find time interval of size 1 second where were 4 invocations.
This means that during reporting period there was time when throughput reached 4 invocations per second.  
And average throughput during reporting period of 1 minute is only 0.15 invocation per second. 
- throughputPerSecondMax - 4
 

#### Metrics summary
 - name - dot separated metric name
 - reportingTimeAvg - reporting interval in milliseconds
 - latency - time in milliseconds between two points: profiledCall start and stop
   - latencyMax maximum latency
   - latencyMin minimum latency
   - latencyAvg average latency
 - callsCountSum - how many times profiledCall was invoked
 - callsThroughputAvg - average rate of profiledCall invocation per second 
 - payload - payload provided via stop method of profiledCall
   - payloadMin - min value of payload
   - payloadMax - max value of payload
   - payloadAvg - avg value of payload
   - payloadSum - total sum of payload provided within reporting interval
   - payloadThroughputAvg - payload rate invocation per second
 - start - start metrics provide information about throughput and count of start invocation of ProfiledCall  
   - startSum - how many times start method was invoked
   - startThroughputAvg - what is an average throughput for start invocation
   - startThroughputPerSecondMax - what is a maximum throughput of start method invocation
 - throughputPerSecondMax - maximum rate within second time interval that was achieved during reporting period 
 (17 means that there was a maximum of 17 invocation within 1 second interval)
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

## Tags and Labels 
Tag is a key-value pair defined by user.
User can define tags during ProfiledCall or IndicationProvider construction. 
Tags works similar to labels in Prometheus or tags in InfluxDB.

Labels is a key-value pair that is automatically linked with metrics in runtime.
User can setup RegexpLabelSticker or custom LabelSticker for Profiler instance.
Then we can use Labels to filter particular metrics in Reporter.
  


## Metric reporting
How to register Profiler Reporter and start to record metrics to external storage.


### Graphite
Graphite uses aggregation rules to compact metric storage.
To simplify aggregation rules all metric names ends with suffix
* *.*Max
* *.*Min
* *.*Avg
* *.*Sum

### Prometheus

### InfluxDB



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

## Source guidebook

* tag as part of idenity of ProfiledCall or Indicator, provided manually by a user during metric construction.
* auto tag that assigned by Tagger based on Identity name. 
  
