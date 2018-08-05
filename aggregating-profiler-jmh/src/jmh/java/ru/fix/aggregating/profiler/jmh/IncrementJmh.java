package ru.fix.aggregating.profiler.jmh;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.StampedLock;

@State(Scope.Benchmark)
public class IncrementJmh {

    AtomicLong atomicLong1 = new AtomicLong();
    AtomicLong atomicLong2 = new AtomicLong();
    AtomicLong atomicLong3 = new AtomicLong();
    AtomicLong atomicLong4 = new AtomicLong();
    AtomicLong atomicLong5 = new AtomicLong();

    @Benchmark
    public void atomic_long_increment() {
        atomicLong1.incrementAndGet();
        atomicLong2.incrementAndGet();
        atomicLong3.incrementAndGet();
        atomicLong4.incrementAndGet();
        atomicLong5.incrementAndGet();
    }

    LongAdder adder1 = new LongAdder();
    LongAdder adder2 = new LongAdder();
    LongAdder adder3 = new LongAdder();
    LongAdder adder4 = new LongAdder();
    LongAdder adder5 = new LongAdder();

    @Benchmark
    public void adder_increment() {
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
    public void rwlock_long_increment() {
        readLock.lock();

        long1++;
        long2++;
        long3++;
        long4++;
        long5++;

        readLock.unlock();
    }

    StampedLock stampedLock = new StampedLock();
    @Benchmark
    public void optimistic_long_increment() {

        Long stamp = stampedLock.writeLock();
        long1++;
        long2++;
        long3++;
        long4++;
        long5++;
        stampedLock.unlockWrite(stamp);
    }


    AtomicLong singleAtomicLong = new AtomicLong();
    @Benchmark
    public void singleAtomicLong(){
        singleAtomicLong.incrementAndGet();
    }


    AtomicLong twoAtomicLong1 = new AtomicLong();
    AtomicLong twoAtomicLong2 = new AtomicLong();
    @Benchmark
    public void twoAtomicLong(){
        twoAtomicLong1.incrementAndGet();
        twoAtomicLong2.incrementAndGet();
    }

    AtomicLong threeAtomicLong1 = new AtomicLong();
    AtomicLong threeAtomicLong2 = new AtomicLong();
    AtomicLong threeAtomicLong3 = new AtomicLong();
    @Benchmark
    public void threeAtomicLong(){
        threeAtomicLong1.incrementAndGet();
        threeAtomicLong2.incrementAndGet();
        threeAtomicLong3.incrementAndGet();
    }



    ReentrantReadWriteLock singleReadWriteLock = new ReentrantReadWriteLock();
    ReentrantReadWriteLock.ReadLock singleReadLock = readWriteLock.readLock();
    long singleLong;

    @Benchmark
    public void singleRwLockLong(){
        singleReadLock.lock();
        singleLong++;
        singleReadLock.unlock();
    }

}
