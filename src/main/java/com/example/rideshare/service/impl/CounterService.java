package com.example.rideshare.service.impl;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class CounterService {

    @Getter
    private int unsafeCounter = 0;

    private final AtomicInteger safeCounter = new AtomicInteger(0);

    @Getter
    private int synchronizedCounter = 0;

    private final AtomicLong totalOperations = new AtomicLong(0);

    public void unsafeIncrement() {
        int current = unsafeCounter;
        unsafeCounter = current + 1;
        totalOperations.incrementAndGet();
    }

    public void atomicIncrement() {
        totalOperations.incrementAndGet();
        safeCounter.incrementAndGet();
    }

    public int getSafeCounter() {
        return safeCounter.get();
    }

    public void reset() {
        unsafeCounter = 0;
        safeCounter.set(0);
        synchronizedCounter = 0;
        totalOperations.set(0);
    }

    public RaceConditionResult demonstrateRaceCondition(int threadCount, int incrementsPerThread) {
        return demonstrate(threadCount, incrementsPerThread, false);
    }

    public RaceConditionResult demonstrateSolution(int threadCount, int incrementsPerThread) {
        return demonstrate(threadCount, incrementsPerThread, true);
    }

    private RaceConditionResult demonstrate(int threadCount, int incrementsPerThread, boolean useSafeCounter) {
        reset();

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);


        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < incrementsPerThread; j++) {
                        if (useSafeCounter) {
                            atomicIncrement();
                        } else {
                            unsafeIncrement();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long endTime = System.currentTimeMillis();
        executor.shutdown();

        int expected = threadCount * incrementsPerThread;
        int actual = useSafeCounter ? getSafeCounter() : getUnsafeCounter();
        int lostUpdates = useSafeCounter ? 0 : expected - actual;

        long startTime = System.currentTimeMillis();

        return new RaceConditionResult(
                threadCount,
                incrementsPerThread,
                expected,
                actual,
                lostUpdates,
                endTime - startTime,
                !useSafeCounter && lostUpdates > 0
        );
    }

    public record RaceConditionResult(
            int threadCount,
            int incrementsPerThread,
            int expectedValue,
            int actualValue,
            int lostUpdates,
            long durationMs,
            boolean hasRaceCondition
    ) {
        public String getSummary() {
            int lostPercent = expectedValue > 0 ? (lostUpdates * 100 / expectedValue) : 0;
            return String.format(
                    "Threads: %d, Ops/thread: %d, Expected: %d, Actual: %d, Lost: %d (%d%%), Duration: %dms, Race: %s",
                    threadCount, incrementsPerThread, expectedValue, actualValue, lostUpdates,
                    lostPercent, durationMs, hasRaceCondition ? "YES" : "NO"
            );
        }
    }
}