package com.Nbbang.backend.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.Set;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Set<String> PRE_AUTH_CSRF_EXEMPT_POST_PATHS = Set.of(
            "/api/pki/admin/login",
            "/api/pki/verify-portone",
            "/api/pki/login/verify",
            "/api/pki/register"
    );

    static final RequestMatcher PRE_AUTH_CSRF_EXEMPT_REQUESTS = request ->
            "POST".equalsIgnoreCase(request.getMethod())
                    && PRE_AUTH_CSRF_EXEMPT_POST_PATHS.contains(request.getRequestURI());

    @Value("${app.cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    @Value("${server.servlet.session.cookie.same-site:lax}")
    private String cookieSameSite;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // [SEC-RQ-001] 이 앱은 Spring Security의 로그인 메커니즘을 쓰지 않고 커스텀 HttpSession(userId/role)으로
        // 로그인 상태를 관리한다. 그래서 Spring Security는 로그인 여부를 알 방법이 없어 anyRequest().authenticated()를
        // 켜면 로그인한 사용자까지 전부 401이 된다. 실제 "기본 차단 + 화이트리스트" 인가는
        // global/interceptor/AuthInterceptor(+ domain/admin/interceptor/AdminAuthInterceptor)에서
        // 세션 기반으로 처리하므로(WebMvcConfig에 등록), 여기서는 permitAll을 유지한다.
        // [SEC-RQ-006] SPA가 /api/csrf에서 발급받은 토큰을 상태 변경 요청의 X-XSRF-TOKEN 헤더로 보낸다.
        // 쿠키와 헤더 값이 모두 일치해야 요청이 컨트롤러까지 전달된다.
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource())) // CORS 프리플라이트(OPTIONS) 허용
            .csrf(csrf -> csrf
                .ignoringRequestMatchers(PRE_AUTH_CSRF_EXEMPT_REQUESTS)
                .csrfTokenRepository(csrfTokenRepository())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            )
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            );

        return http.build();
    }

    private CookieCsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieCustomizer(cookie -> cookie.sameSite(cookieSameSite));
        return repository;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 로그인 세션 쿠키(JSESSIONID)를 프론트와 주고받으려면 자격증명 포함 CORS가 필요하다.
    // CorsConfigurationSource 빈이 없으면 .cors(Customizer.withDefaults())가 실질적으로 아무 동작도 하지 않아
    // 프리플라이트가 빈 200으로 막히므로(브라우저에서 Failed to fetch), 여기서 명시적으로 등록한다.
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
