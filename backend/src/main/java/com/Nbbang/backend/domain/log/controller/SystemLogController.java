package com.Nbbang.backend.domain.log.controller;

import com.Nbbang.backend.domain.log.entity.SystemLog;
import com.Nbbang.backend.domain.log.service.SystemLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/logs")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class SystemLogController {

    private final SystemLogService systemLogService;

    @GetMapping
    public List<SystemLog> getLogsByType(@RequestParam String type) {
        return systemLogService.getLogsByType(type);
    }
}
