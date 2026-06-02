package com.dataGovernance.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;
import org.springframework.core.io.Resource;

import java.io.IOException;

/**
 * 静态资源与前端单页应用路由 fallback 配置。
 *
 * 前端构建产物输出至 src/main/resources/static，访问入口：
 *   http://localhost:18082/DataGovernance/
 *
 * - / 与 /index.html 都映射到 static/index.html
 * - 任意未匹配的非 API 路径，转发到 index.html，由前端路由处理
 * - /assets/** 等静态资源走默认资源链
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/index.html");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requested = location.createRelative(resourcePath);
                        if (requested.exists() && requested.isReadable()) {
                            return requested;
                        }
                        // API 路径不要 fallback 到 index.html
                        if (resourcePath.startsWith("grid/") || resourcePath.startsWith("api/")
                                || resourcePath.startsWith("data-process/") || resourcePath.startsWith("log/")) {
                            return null;
                        }
                        // 其余视为前端路由，统一返回 index.html
                        return location.createRelative("index.html");
                    }
                });
    }
}
