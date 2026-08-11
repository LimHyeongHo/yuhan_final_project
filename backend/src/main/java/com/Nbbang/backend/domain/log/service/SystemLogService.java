package com.Nbbang.backend.domain.log.service;

import com.Nbbang.backend.domain.log.entity.SystemLog;
import com.Nbbang.backend.domain.log.repository.SystemLogRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SystemLogService {

    private final SystemLogRepository systemLogRepository;

    @PostConstruct
    @Transactional
    public void initMockLogs() {
        // DB가 비어있을 경우에만 초기 데이터 세팅
        if (systemLogRepository.count() == 0) {
            systemLogRepository.save(SystemLog.builder()
                    .displayId("TX-99824-B")
                    .type("SECURITY")
                    .status("SUCCESS")
                    .diff("0x0000...0000")
                    .detail("상세 정보")
                    .build());
            
            systemLogRepository.save(SystemLog.builder()
                    .displayId("TX-99712-F")
                    .type("SECURITY")
                    .status("TAMPERED")
                    .diff("Diff: -12.4% (PRICE_FIELD)")
                    .detail("위변조 추적")
                    .build());
            
            systemLogRepository.save(SystemLog.builder()
                    .displayId("TX-99709-X")
                    .type("SECURITY")
                    .status("SUCCESS")
                    .diff("0x0000...0000")
                    .detail("상세 정보")
                    .build());
            
            systemLogRepository.save(SystemLog.builder()
                    .displayId("TX-99698-A")
                    .type("SECURITY")
                    .status("SUCCESS")
                    .diff("0x0000...0000")
                    .detail("상세 정보")
                    .build());
        }
    }

    @Transactional(readOnly = true)
    public List<SystemLog> getLogsByType(String type) {
        return systemLogRepository.findByTypeOrderByTimestampDesc(type);
    }
}
