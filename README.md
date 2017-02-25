# commons-profiler
Commons Profiler provide basic API for application metrics measurement.  

## Profile simple method call 
```java
Profiler profiler = new SimpleProfiler();

ProfiledCall call = profiler.profiledCall("some.method");
call.start();
someMethod();
call.stop();
```

## Profile method call with payload
```java
Profiler profiler = new SimpleProfiler();

ProfiledCall call = profiler.profiledCall("some.method");
call.start();
Long paylaod = someMethodWithPayload();
call.stop(payload);
```