package com.Nbbang.backend.global.config;

import com.Nbbang.backend.domain.admin.interceptor.AdminAuthInterceptor;
import com.Nbbang.backend.global.interceptor.AuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 프론트엔드에서 /uploads/파일명.jpg 로 접근할 때 서버의 로컬 uploads 폴더로 매핑
        String uploadPath = Paths.get(System.getProperty("user.dir"), "uploads").toUri().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadPath);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // SEC-RQ-001: /api/** 기본 로그인 요구 (화이트리스트는 AuthInterceptor 내부에서 관리)
        registry.addInterceptor(new AuthInterceptor())
                .addPathPatterns("/api/**");
        // /api/admin/**은 로그인 확인 후 추가로 ROLE_ADMIN까지 요구
        registry.addInterceptor(new AdminAuthInterceptor())
                .addPathPatterns("/api/admin/**");
    }
}
