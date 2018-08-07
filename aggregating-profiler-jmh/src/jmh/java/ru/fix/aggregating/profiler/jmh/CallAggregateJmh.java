package ru.fix.aggregating.profiler.jmh;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import ru.fix.aggregating.profiler.engine.AggregatingCall;
import ru.fix.aggregating.profiler.engine.CallAggregate;

import java.util.concurrent.atomic.AtomicInteger;


@State(Scope.Benchmark)
public class CallAggregateJmh {

    final long timestamp = System.currentTimeMillis();

    final CallAggregate callAggregate = new CallAggregate(
            "name",
            new AtomicInteger(0)
    );

    final AggregatingCall call = new AggregatingCall(
            "name",
            (profiledCallName, updateAction) -> updateAction.accept(callAggregate));


    @Benchmark
    public void call() {
        callAggregate.call(timestamp, 27L, 1);
    }

    @Benchmark
    public void start_stop() {
        callAggregate.start(call);
        callAggregate.stop(call, timestamp, 27L, 1);
    }
}
