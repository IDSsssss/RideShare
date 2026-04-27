package com.example.rideshare.utils;

import com.example.rideshare.model.dto.TaskStatusResponse;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class BookingTaskTracker {

    private final ConcurrentHashMap<String, TaskStatus> tasks = new ConcurrentHashMap<>();
    private final AtomicInteger taskCounter = new AtomicInteger(0);

    public String generateAndCreateTask(int totalCount) {
        String taskId = "task-" + taskCounter.incrementAndGet() + "-" + System.currentTimeMillis();
        tasks.put(taskId, new TaskStatus(totalCount));
        return taskId;
    }

    public void createTask(String taskId, int totalCount) {
        tasks.put(taskId, new TaskStatus(totalCount));
    }

    public void updateStatus(String taskId, String status) {
        TaskStatus task = tasks.get(taskId);
        if (task != null) {
            task.setStatus(status);
        }
    }

    public void incrementProcessed(String taskId) {
        TaskStatus task = tasks.get(taskId);
        if (task != null) {
            task.getProcessedCount().incrementAndGet();
        }
    }

    // Новый метод для добавления ошибки
    public void addError(String taskId, String error) {
        TaskStatus task = tasks.get(taskId);
        if (task != null) {
            task.addError(error);
        }
    }

    public void completeTask(String taskId) {
        TaskStatus task = tasks.get(taskId);
        if (task != null) {
            task.setStatus("COMPLETED");
            task.setEndTime(LocalDateTime.now());
        }
    }

    public void failTask(String taskId, String error) {
        TaskStatus task = tasks.get(taskId);
        if (task != null) {
            task.setStatus("FAILED");
            task.setEndTime(LocalDateTime.now());
            task.setErrorMessage(error);
            task.addError(error);
        }
    }

    public TaskStatusResponse toResponse(String taskId) {
        TaskStatus status = tasks.get(taskId);
        if (status == null) {
            return null;
        }

        return new TaskStatusResponse(
                taskId,
                status.getStatus(),
                status.getProcessedCount().get(),
                status.getTotalCount(),
                status.getProgressPercent(),
                status.getStartTime(),
                status.getEndTime(),
                status.getErrorMessage(),
                status.getErrors()  // добавляем список ошибок
        );
    }

    public static class TaskStatus {
        @Setter
        @Getter
        private volatile String status;
        @Getter
        private final AtomicInteger processedCount = new AtomicInteger(0);
        @Getter
        private final int totalCount;
        @Getter
        private final LocalDateTime startTime;
        @Setter
        @Getter
        private LocalDateTime endTime;
        @Setter
        @Getter
        private String errorMessage;
        private final List<String> errors;  // список ошибок

        public TaskStatus(int totalCount) {
            this.status = "PENDING";
            this.totalCount = totalCount;
            this.startTime = LocalDateTime.now();
            this.errors = new ArrayList<>();
        }

        public void addError(String error) {
            this.errors.add(error);
        }

        public List<String> getErrors() {
            return new ArrayList<>(errors);  // возвращаем копию для безопасности
        }

        public double getProgressPercent() {
            return totalCount > 0 ? processedCount.get() * 100.0 / totalCount : 0;
        }
    }
}