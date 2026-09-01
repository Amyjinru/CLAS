package com.clas.config;

import com.clas.common.web.InternalServiceAuthInterceptor;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class IamWebConfig implements WebMvcConfigurer {
    private final AuthInterceptor authInterceptor;
    private final InternalServiceAuthInterceptor internalServiceAuthInterceptor;

    @Value("${app.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173}")
    private List<String> allowedOrigins;

    public IamWebConfig(AuthInterceptor authInterceptor, InternalServiceAuthInterceptor internalServiceAuthInterceptor) {
        this.authInterceptor = authInterceptor;
        this.internalServiceAuthInterceptor = internalServiceAuthInterceptor;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOriginPatterns(allowedOrigins.toArray(new String[0]))
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            .allowedHeaders("Authorization", "Content-Type")
            .allowCredentials(true);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor).addPathPatterns("/api/**");
        registry.addInterceptor(internalServiceAuthInterceptor).addPathPatterns("/internal/**");
    }
}
