package com.Nbbang.backend.domain.bookreport.service;

import com.Nbbang.backend.domain.bookreport.entity.DailyDepartmentBookReport;
import com.Nbbang.backend.domain.product.entity.ProductCategory;
import com.Nbbang.backend.global.exception.CustomException;
import com.Nbbang.backend.global.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BookReportManualJobService {

    private final Map<String, ManualJob> jobs = new ConcurrentHashMap<>();
    private final Object creationLock = new Object();
    private String activeJobId;

    public StartResult createJob(ProductCategory category) {
        synchronized (creationLock) {
            ManualJob activeJob = activeJobId == null ? null : jobs.get(activeJobId);
            if (activeJob != null && activeJob.isActive()) {
                return new StartResult(false, activeJobId, activeJob.snapshot());
            }

            String jobId = UUID.randomUUID().toString();
            ManualJob job = new ManualJob(jobId, category);
            jobs.put(jobId, job);
            activeJobId = jobId;
            return new StartResult(true, jobId, job.snapshot());
        }
    }

    public void markRunning(String jobId) {
        ManualJob job = jobs.get(jobId);
        if (job != null) {
            job.markRunning();
        }
    }

    public void complete(String jobId, DailyDepartmentBookReport report) {
        ManualJob job = jobs.get(jobId);
        if (job != null) {
            job.complete(report);
        }
        clearActiveJob(jobId);
    }

    public void fail(String jobId, Exception exception) {
        ManualJob job = jobs.get(jobId);
        if (job != null) {
            job.fail(exception);
        }
        clearActiveJob(jobId);
    }

    public Map<String, Object> getJob(String jobId) {
        ManualJob job = jobs.get(jobId);
        return job == null ? null : job.snapshot();
    }

    private void clearActiveJob(String jobId) {
        synchronized (creationLock) {
            if (jobId.equals(activeJobId)) {
                activeJobId = null;
            }
        }
    }

    public record StartResult(boolean created, String jobId, Map<String, Object> snapshot) {
    }

    private static final class ManualJob {
        private final Map<String, Object> state = new LinkedHashMap<>();

        private ManualJob(String jobId, ProductCategory category) {
            state.put("jobId", jobId);
            state.put("category", category.name());
            state.put("status", "QUEUED");
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

        private synchronized void complete(DailyDepartmentBookReport report) {
            state.put("status", "COMPLETED");
            state.put("reportDate", report.getReportDate().toString());
            state.put("reportStatus", report.getStatus().name());
            state.put("generatedAt", report.getGeneratedAt().toString());
            state.put("completedAt", Instant.now().toString());
        }

        private synchronized void fail(Exception exception) {
            ErrorCode errorCode = exception instanceof CustomException customException
                    ? customException.getErrorCode()
                    : ErrorCode.INTERNAL_SERVER_ERROR;
            state.put("status", "FAILED");
            state.put("code", errorCode.name());
            state.put("message", errorCode.getMessage());
            state.put("completedAt", Instant.now().toString());
        }

        private synchronized Map<String, Object> snapshot() {
            return new LinkedHashMap<>(state);
        }
    }
}
