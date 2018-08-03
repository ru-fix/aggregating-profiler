package ru.fix.aggregating.profiler.jmh;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import ru.fix.aggregating.profiler.AggregatingProfiler;
import ru.fix.aggregating.profiler.ProfiledCall;
import ru.fix.aggregating.profiler.Profiler;
import ru.fix.aggregating.profiler.ProfilerReporter;
import ru.fix.aggregating.profiler.engine.AggregatingCall;
import ru.fix.aggregating.profiler.engine.SharedCounters;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


@State(Scope.Thread)
public class SharedCountersJmh {

    SharedCounters sharedCounters;
    AggregatingCall call;
    AggregatingCall lockCall;
    ProfiledCall profilerCall;
    Profiler profiler;
    ProfilerReporter reporter;

    @Setup
    public void setup() {
        sharedCounters = new SharedCounters(new AtomicInteger(0));

        call = new AggregatingCall("name", (profiledCallName, updateAction) -> {
            updateAction.accept(sharedCounters);
        });

        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();


        lockCall = new AggregatingCall("name", (profiledCallName, updateAction) -> {
            ReentrantReadWriteLock.ReadLock rl = lock.readLock();
            rl.lock();
            updateAction.accept(sharedCounters);
            rl.unlock();
        });

        profiler = new AggregatingProfiler();
        reporter = profiler.createReporter();

        profilerCall = profiler.profiledCall("my-profiled-call");
    }


    @Benchmark
    public void simple_increment() {
        call.call(1000, 1010, 1);
    }

    @Benchmark
    public void lock_increment() {
        lockCall.call(1000, 1010, 1);
    }

    @Benchmark
    public void profiler_increment() {
        profilerCall.call(1000, 1010, 1);
    }
}
