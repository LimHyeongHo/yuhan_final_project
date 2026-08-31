package com.Nbbang.backend.global.config;

import com.Nbbang.backend.domain.admin.interceptor.AdminAuthInterceptor;
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
        registry.addInterceptor(new AdminAuthInterceptor())
                .addPathPatterns("/api/admin/**");
    }
}
