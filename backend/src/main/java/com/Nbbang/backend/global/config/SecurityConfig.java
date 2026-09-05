package com.Nbbang.backend.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // [SEC-RQ-001] 이 앱은 Spring Security의 로그인 메커니즘을 쓰지 않고 커스텀 HttpSession(userId/role)으로
        // 로그인 상태를 관리한다. 그래서 Spring Security는 로그인 여부를 알 방법이 없어 anyRequest().authenticated()를
        // 켜면 로그인한 사용자까지 전부 401이 된다. 실제 "기본 차단 + 화이트리스트" 인가는
        // global/interceptor/AuthInterceptor(+ domain/admin/interceptor/AdminAuthInterceptor)에서
        // 세션 기반으로 처리하므로(WebMvcConfig에 등록), 여기서는 permitAll을 유지한다.
        // [SEC-RQ-006] CSRF는 이번 변경 범위에서 제외했다. 켜려면 SPA의 모든 fetch 호출에 CSRF 헤더를 붙이는
        // 작업(프론트 전역)이 선행되어야 하며, 지금 상태로 켜면 로그인한 사용자의 모든 상태변경 요청이 403이 된다.
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource())) // CORS 프리플라이트(OPTIONS) 허용
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            )
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            );

        return http.build();
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
        config.setAllowedOrigins(List.of("http://localhost:3000"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
