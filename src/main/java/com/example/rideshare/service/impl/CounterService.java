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

    // НЕ потокобезопасный счётчик (будет race condition)
    @Getter
    private int unsafeCounter = 0;

    // Потокобезопасный счётчик (Atomic)
    private final AtomicInteger safeCounter = new AtomicInteger(0);

    // Потокобезопасный счётчик (synchronized)
    @Getter
    private int synchronizedCounter = 0;

    // Для статистики
    private final AtomicLong totalOperations = new AtomicLong(0);

    public void unsafeIncrement() {
        int current = unsafeCounter;
        int next = current + 1;
        unsafeCounter = next;
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
        reset();

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);



        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < incrementsPerThread; j++) {
                        unsafeIncrement();
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
        int actual = getUnsafeCounter();
        int lostUpdates = expected - actual;

        return new RaceConditionResult(
                threadCount,
                incrementsPerThread,
                expected,
                actual,
                lostUpdates,
                (endTime - startTime),
                lostUpdates > 0
        );
    }

    long startTime = System.currentTimeMillis();

    public RaceConditionResult demonstrateSolution(int threadCount, int incrementsPerThread) {
        reset();

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        startTime = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < incrementsPerThread; j++) {
                        atomicIncrement();
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
        int actual = getSafeCounter();

        return new RaceConditionResult(
                threadCount,
                incrementsPerThread,
                expected,
                actual,
                0,
                (endTime - startTime),
                false
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
            return String.format(
                    "Threads: %d, Ops/thread: %d, Expected: %d, Actual: %d, Lost: %d (%d%%), Duration: %dms, Race: %s",
                    threadCount, incrementsPerThread, expectedValue, actualValue, lostUpdates,
                    (lostUpdates * 100 / expectedValue), durationMs, hasRaceCondition ? "YES" : "NO"
            );
        }
    }
}