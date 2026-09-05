package com.Nbbang.backend.domain.admin.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LegacyMigrationJobService {

    private final Map<String, MigrationJob> jobs = new ConcurrentHashMap<>();
    private final Object creationLock = new Object();
    private String activeJobId;

    public StartResult createJob() {
        synchronized (creationLock) {
            MigrationJob activeJob = activeJobId == null ? null : jobs.get(activeJobId);
            if (activeJob != null && activeJob.isActive()) {
                return new StartResult(false, activeJobId, activeJob.snapshot());
            }

            String jobId = UUID.randomUUID().toString();
            MigrationJob job = new MigrationJob(jobId);
            jobs.put(jobId, job);
            activeJobId = jobId;
            return new StartResult(true, jobId, job.snapshot());
        }
    }

    public void markRunning(String jobId) {
        MigrationJob job = jobs.get(jobId);
        if (job != null) {
            job.markRunning();
        }
    }

    public void updateProgress(String jobId, AdminService.MigrationProgress progress) {
        MigrationJob job = jobs.get(jobId);
        if (job != null) {
            job.updateProgress(progress);
        }
    }

    public void complete(String jobId, Map<String, Object> result) {
        MigrationJob job = jobs.get(jobId);
        if (job != null) {
            job.complete(result);
        }
        clearActiveJob(jobId);
    }

    public void fail(String jobId, Exception exception) {
        MigrationJob job = jobs.get(jobId);
        if (job != null) {
            job.fail(exception);
        }
        clearActiveJob(jobId);
    }

    public Map<String, Object> getJob(String jobId) {
        MigrationJob job = jobs.get(jobId);
        return job == null ? null : job.snapshot();
    }

    private void clearActiveJob(String jobId) {
        synchronized (creationLock) {
            if (jobId.equals(activeJobId)) {
                activeJobId = null;
            }
        }
    }

    public record StartResult(
            boolean created,
            String jobId,
            Map<String, Object> snapshot) {
    }

    private static final class MigrationJob {
        private final Map<String, Object> state = new LinkedHashMap<>();

        private MigrationJob(String jobId) {
            state.put("jobId", jobId);
            state.put("status", "QUEUED");
            state.put("totalCount", 0);
            state.put("processedCount", 0);
            state.put("confirmedCount", 0);
            state.put("failedCount", 0);
            state.put("alreadySyncedCount", 0);
            state.put("remediatedCount", 0);
            state.put("mismatchCount", 0);
            state.put("progressPercent", 0);
            state.put("currentProductId", null);
            state.put("createdAt", Instant.now().toString());
        }

        private synchronized boolean isActive() {
            String status = (String) state.get("status");
            return "QUEUED".equals(status) || "RUNNING".equals(status);
        }

        private synchronized void markRunning() {
            state.put("status", "RUNNING");
            state.put("startedAt", Instant.now().toString());
        }

        private synchronized void updateProgress(AdminService.MigrationProgress progress) {
            state.put("totalCount", progress.totalCount());
            state.put("processedCount", progress.processedCount());
            state.put("confirmedCount", progress.confirmedCount());
            state.put("failedCount", progress.failedCount());
            state.put("alreadySyncedCount", progress.alreadySyncedCount());
            state.put("remediatedCount", progress.remediatedCount());
            state.put("mismatchCount", progress.mismatchCount());
            state.put("currentProductId", progress.currentProductId());
            state.put("progressPercent", calculatePercent(
                    progress.processedCount(), progress.totalCount()));
        }

        private synchronized void complete(Map<String, Object> result) {
            state.putAll(result);
            state.put("progressPercent", 100);
            state.put("currentProductId", null);
            state.put("completedAt", Instant.now().toString());
        }

        private synchronized void fail(Exception exception) {
            state.put("status", "FAILED");
            state.put("message", exception.getMessage() == null
                    ? exception.getClass().getSimpleName()
                    : exception.getMessage());
            state.put("currentProductId", null);
            state.put("completedAt", Instant.now().toString());
        }

        private synchronized Map<String, Object> snapshot() {
            return new LinkedHashMap<>(state);
        }

        private static int calculatePercent(int processedCount, int totalCount) {
            if (totalCount <= 0) {
                return 0;
            }
            return Math.min(100, (int) ((long) processedCount * 100 / totalCount));
        }
    }
}
