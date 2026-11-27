package com.school.material.config;

import com.school.material.security.IdentityInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 跨域配置：来源由环境变量 CORS_ALLOWED_ORIGINS 指定（逗号分隔）。
 * 统一登录依赖 Cookie，必须 allowCredentials(true)，因此来源不能使用 "*"。
 * 生产采用同源反向代理部署时，可不配置跨域。
 */
@Configuration
@RequiredArgsConstructor
public class CorsConfig implements WebMvcConfigurer {

    private final IdentityInterceptor identityInterceptor;

    @Value("${cors.allowed-origins:http://localhost:5182,http://127.0.0.1:5182}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(allowedOrigins.split("\\s*,\\s*"))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(identityInterceptor).addPathPatterns("/**");
    }
}
