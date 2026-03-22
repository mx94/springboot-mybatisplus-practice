package com.example.demo.config;

import com.example.demo.common.interceptor.AuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 配置 — 注册自定义拦截器
 *
 * 【知识点：WebMvcConfigurer】
 * 实现这个接口可以对 Spring MVC 进行扩展配置。
 * addInterceptors() 方法专门用来注册拦截器链。
 *
 * 拦截器执行顺序（多个拦截器时按注册顺序）：
 *   请求进来：interceptor1.preHandle → interceptor2.preHandle → Controller
 *   请求结束：interceptor2.afterCompletion → interceptor1.afterCompletion
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**");   // 拦截所有路径
        // 如需排除某些路径（如登录接口、静态资源），可以追加：
        // .excludePathPatterns("/login", "/static/**")
    }
}
