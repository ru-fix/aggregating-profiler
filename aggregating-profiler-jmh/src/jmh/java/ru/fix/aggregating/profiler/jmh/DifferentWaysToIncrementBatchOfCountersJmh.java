package ru.fix.aggregating.profiler.jmh;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.StampedLock;

@State(Scope.Benchmark)
public class DifferentWaysToIncrementBatchOfCountersJmh {

    AtomicLong atomicLong1 = new AtomicLong();
    AtomicLong atomicLong2 = new AtomicLong();
    AtomicLong atomicLong3 = new AtomicLong();
    AtomicLong atomicLong4 = new AtomicLong();
    AtomicLong atomicLong5 = new AtomicLong();

    @Benchmark
    public void atomic_long1(){
        atomicLong1.incrementAndGet();
    }

    @Benchmark
    public void atomic_long3(){
        atomicLong1.incrementAndGet();
        atomicLong2.incrementAndGet();
        atomicLong3.incrementAndGet();
    }

    @Benchmark
    public void atomic_long5() {
        atomicLong1.incrementAndGet();
        atomicLong2.incrementAndGet();
        atomicLong3.incrementAndGet();
        atomicLong4.incrementAndGet();
        atomicLong5.incrementAndGet();
    }

    AtomicLongArray atomicLongArray = new AtomicLongArray(5);
    @Benchmark
    public void atomic_long_array5() {
        atomicLongArray.incrementAndGet(0);
        atomicLongArray.incrementAndGet(1);
        atomicLongArray.incrementAndGet(2);
        atomicLongArray.incrementAndGet(3);
        atomicLongArray.incrementAndGet(4);
    }

    @Benchmark
    public long swapLong1(){
        long v1 = atomicLong1.getAndSet(174L);
        return v1;
    }

    @Benchmark
    public long swapLong3(){
        long v1 = atomicLong1.getAndSet(174L);
        long v2 = atomicLong1.getAndSet(174L);
        long v3 = atomicLong1.getAndSet(174L);
        return v1+v2+v3;
    }

    @Benchmark
    public long swapLong5(){
        long v1 = atomicLong1.getAndSet(174L);
        long v2 = atomicLong1.getAndSet(174L);
        long v3 = atomicLong1.getAndSet(174L);
        long v4 = atomicLong1.getAndSet(174L);
        long v5 = atomicLong1.getAndSet(174L);
        return v1+v2+v3+v4+v5;
    }

    LongAdder adder1 = new LongAdder();
    LongAdder adder2 = new LongAdder();
    LongAdder adder3 = new LongAdder();
    LongAdder adder4 = new LongAdder();
    LongAdder adder5 = new LongAdder();


    @Benchmark
    public void long_adder1() {
        adder1.increment();
    }

    @Benchmark
    public void long_adder3() {
        adder1.increment();
        adder2.increment();
        adder3.increment();
    }

    @Benchmark
    public void long_adder5() {
        adder1.increment();
        adder2.increment();
        adder3.increment();
        adder4.increment();
        adder5.increment();
    }

    long long1 = 0L;
    long long2 = 0L;
    long long3 = 0L;
    long long4 = 0L;
    long long5 = 0L;

    ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    ReentrantReadWriteLock.ReadLock readLock = readWriteLock.readLock();

    @Benchmark
    public void read_write_lock5() {
        readLock.lock();

        long1++;
        long2++;
        long3++;
        long4++;
        long5++;

        readLock.unlock();
    }

    @Benchmark
    public void read_write_lock1() {
        readLock.lock();

        long1++;

        readLock.unlock();
    }


    StampedLock stampedLock = new StampedLock();
    @Benchmark
    public void stamped_lock5() {

        Long stamp = stampedLock.writeLock();
        long1++;
        long2++;
        long3++;
        long4++;
        long5++;
        stampedLock.unlockWrite(stamp);
    }


    @Benchmark
    public void stamped_lock1() {
        Long stamp = stampedLock.writeLock();
        long1++;
        stampedLock.unlockWrite(stamp);
    }

    @Benchmark
    public void cas1(){
        atomicLong1.compareAndSet(105L, 106L);
    }

    @Benchmark
    public boolean cas5(){
        boolean r1 = atomicLong1.compareAndSet(105L, 106L);
        boolean r2 = atomicLong2.compareAndSet(106L, 107L);
        boolean r3 = atomicLong3.compareAndSet(107L, 108L);
        boolean r4 = atomicLong4.compareAndSet(108L, 109L);
        boolean r5 = atomicLong5.compareAndSet(109L, 110L);
        return r1 && r2 && r3 && r4 && r5;
    }
}
