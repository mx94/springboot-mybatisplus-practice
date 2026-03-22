package com.example.demo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 启动类
 *
 * 【知识点：@SpringBootApplication】
 * 这是一个复合注解，等同于同时使用：
 *   @SpringBootConfiguration  → 标记为 Spring 配置类
 *   @EnableAutoConfiguration  → 开启自动配置（根据 pom.xml 的依赖自动配置 Bean）
 *   @ComponentScan            → 扫描当前包及子包下的所有 @Component/@Service/@Controller 等
 *
 * 【知识点：@MapperScan】
 * 告诉 MyBatis Plus：去 com.example.demo.mapper 包下扫描所有 Mapper 接口，
 * 并自动生成代理实现类注入到 Spring 容器。
 */
@SpringBootApplication
@MapperScan("com.example.demo.mapper")
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}

