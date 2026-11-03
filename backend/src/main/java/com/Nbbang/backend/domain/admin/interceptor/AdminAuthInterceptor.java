package com.Nbbang.backend.domain.admin.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;

// /api/admin/** 요청은 세션에 role=ROLE_ADMIN이 있어야만 통과시킨다.
public class AdminAuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);
        Object role = (session != null) ? session.getAttribute("role") : null;

        if (!"ROLE_ADMIN".equals(role)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"관리자 권한이 필요합니다.\"}");
            return false;
        }
        return true;
    }
}
