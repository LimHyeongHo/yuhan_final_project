package com.Nbbang.backend.domain.member.filter;

import com.Nbbang.backend.domain.member.service.CertificateSessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// 프론트 타이머와 무관하게, 만료된 인증서 세션으로 들어오는 모든 요청을 서버에서 독립적으로 차단/폐기한다.
@Component
public class CertificateExpirationFilter extends OncePerRequestFilter {

    private final CertificateSessionService certificateSessionService;

    public CertificateExpirationFilter(CertificateSessionService certificateSessionService) {
        this.certificateSessionService = certificateSessionService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("userId") != null) {
            String userId = (String) session.getAttribute("userId");
            if (certificateSessionService.hasExpiredSession(userId)) {
                // N분 타이머가 실제로 만료된 경우에만 인증서 자체를 폐기하고 로그아웃.
                certificateSessionService.revoke(userId);
                session.invalidate();
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"error\":\"인증서 시간이 만료되었습니다.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
