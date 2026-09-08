package com.Nbbang.backend.global.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

// SEC-RQ-001: /api/** 요청은 기본적으로 로그인(세션의 userId)이 있어야 통과한다.
// 이 프로젝트는 Spring Security의 로그인 메커니즘 대신 커스텀 HttpSession(userId/role)으로 로그인 상태를
// 관리하므로, Spring Security의 authorizeHttpRequests().authenticated()를 켜면 Spring Security가 이
// 세션을 인식하지 못해 로그인한 사용자까지 전부 401이 된다. 그래서 기존 AdminAuthInterceptor와 동일한
// 방식(세션 기반 인터셉터)으로 "기본 차단 + 명시적 허용 목록" 정책을 구현한다.
// PUBLIC_ROUTES에 없는 모든 /api/** 요청은 로그인이 필요하다.
public class AuthInterceptor implements HandlerInterceptor {

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    // method + path 패턴. 같은 경로라도 메서드가 다르면(GET은 공개, POST/PUT/DELETE는 로그인 필요) 구분해야 하므로
    // WebMvcConfig의 excludePathPatterns(경로만 구분 가능) 대신 인터셉터 내부에서 직접 매칭한다.
    private static final List<Route> PUBLIC_ROUTES = List.of(
            new Route("GET", "/api/products"),
            new Route("GET", "/api/products/*"),
            new Route("GET", "/api/products/*/verify"),
            new Route("GET", "/api/sellers/*/profile"),
            new Route("GET", "/api/ca/root-cert"),
            new Route("GET", "/hello"),
            new Route("POST", "/api/pki/verify-portone"),
            new Route("GET", "/api/pki/check-email"),
            new Route("POST", "/api/pki/register"),
            new Route("POST", "/api/pki/admin/login"),
            new Route("GET", "/api/pki/login/challenge"),
            new Route("POST", "/api/pki/login/verify")
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String method = request.getMethod();
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true; // CORS 프리플라이트는 세션 쿠키 없이 오므로 통과시켜야 한다.
        }

        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }

        if (isPublicRoute(method, path)) {
            return true;
        }

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"로그인이 필요합니다.\"}");
            return false;
        }
        return true;
    }

    private boolean isPublicRoute(String method, String path) {
        for (Route route : PUBLIC_ROUTES) {
            if (route.method.equalsIgnoreCase(method) && pathMatcher.match(route.pattern, path)) {
                return true;
            }
        }
        return false;
    }

    private static final class Route {
        private final String method;
        private final String pattern;

        private Route(String method, String pattern) {
            this.method = method;
            this.pattern = pattern;
        }
    }
}
