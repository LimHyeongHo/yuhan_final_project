package com.Nbbang.backend.domain.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class LegacyMigrationWorker {

    private final AdminService adminService;
    private final LegacyMigrationJobService jobService;

    @Async
    public void run(String jobId) {
        jobService.markRunning(jobId);
        try {
            Map<String, Object> result = adminService.migrateLegacyData(
                    progress -> jobService.updateProgress(jobId, progress));
            jobService.complete(jobId, result);
        } catch (Exception exception) {
            jobService.fail(jobId, exception);
        }
    }
}
