package ru.fix.aggregating.profiler.jmh;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import ru.fix.aggregating.profiler.AggregatingProfiler;
import ru.fix.aggregating.profiler.ProfiledCall;
import ru.fix.aggregating.profiler.Profiler;


@State(Scope.Benchmark)
public class ProfilerJmh {

    final Profiler profiler = new AggregatingProfiler(new NoopTagger());

    @Benchmark
    public long system_current_time() {
        return System.currentTimeMillis();
    }

    @Benchmark
    public long system_nanotime() {
        return System.nanoTime();
    }

    @Benchmark
    public void profiledCall_call() {
        ProfiledCall call = profiler.profiledCall("name");
        call.call();
    }

    @Benchmark
    public void profiledCall_call_with_latency() {
        ProfiledCall call = profiler.profiledCall("name");
        call.call(System.currentTimeMillis() - 25);
    }

    @Benchmark
    public void profiledCall_call_with_latency_and_payload() {
        ProfiledCall call = profiler.profiledCall("name");
        call.call(System.currentTimeMillis() - 25, 5);
    }

    @Benchmark
    public void profiler_call() {
        profiler.call("name");
    }

    @Benchmark
    public void profiledCall_start_stop() {
        ProfiledCall call = profiler.profiledCall("name");
        call.start();
        call.stop();
    }
}
