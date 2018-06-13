# commons-profiler
[![Maven Central](https://img.shields.io/maven-central/v/ru.fix/commons-profiler-api.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22ru.fix%22)

Commons Profiler provide basic API for application metrics measurement.  
Profiler consist of two parts: metric recording API to trace events and metric reporting API 
to export metrics to external storage system or in simple case into stdout.  

## Metric recording

Suppose that we want to trace how much time it takes for SmartService to compute and return result.  
We can  measure several metrics: 
 - latency - time between two points: when method invoked and when method completed);
 - throughput - how often method was invoked (invocation per second)
 - max throughput - we made measurement  during 2 minutes and want to find point in time 
 where throughput reached its maximum
 - callsCount - how many time method was invoked 
 
Here is and example how we can record such metrics with profiler.  
In real life application you can use custom aspect to wrap method invocation, etc.  
 
Sync example: 
```java
// Init single profiler instance per application 
Profiler profiler = new SimpleProfiler();

// Create profiled call each time you want to make measurement 
ProfiledCall call = profiler.profiledCall("smart.service");
call.start();

SmartResult result = smartService.doSmartComputation();

call.stop();
```

Async example: 
```java
// Init single profiler instance per application
Profiler profiler = new SimpleProfiler();

// Create profiled call each time you want to make measurement
// Profiled call could be started and closed in different threads
ProfiledCall call = profiler.profiledCall("smart.service");
call.start();

CompletableFuture<SmartResult> result = smartService.doSmartComputation();

result.thenRun(){ 
    call.stop();
}

```

Some times method invocation is not sufficient and you need to record information about payload 
of the method. Profiler API provide a way to register integer value of custom payload: 

```java

long argumentForComputation = ...

Profiler profiler = new SimpleProfiler();

ProfiledCall call = profiler.profiledCall("smart.service,with.payload");
call.start();

Long paylaod = smartService.doAnotherSmartComputation(argumentForComputation);

call.stop(argumentForComputation);
```

## Metric reporting

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