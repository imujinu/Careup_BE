package com.careup.ordering.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;

@Configuration
public class MailTemplateConfig {

    private SpringResourceTemplateResolver resolver(
            String prefix, String suffix, TemplateMode mode, int order, boolean cacheable) {
        SpringResourceTemplateResolver r = new SpringResourceTemplateResolver();
        r.setPrefix(prefix.endsWith("/") ? prefix : prefix + "/");
        r.setSuffix(suffix);
        r.setTemplateMode(mode);
        r.setCharacterEncoding("UTF-8");
        r.setOrder(order);
        r.setCacheable(cacheable);
        r.setCheckExistence(true);
        return r;
    }

    /**
     * HTML 전용 템플릿 엔진
     * - 개발중: file:src/main/resources/templates (핫리로드)
     * - 배포:   classpath:/templates (패키징 리소스)
     */
    @Primary
    @Bean("mailHtmlEngine")
    public SpringTemplateEngine mailHtmlEngine() {
        SpringTemplateEngine engine = new SpringTemplateEngine();

        // 개발 편의(핫리로드)
        engine.addTemplateResolver(
                resolver("file:src/main/resources/templates", ".html", TemplateMode.HTML, 1, false));

        // 배포(classpath)
        engine.addTemplateResolver(
                resolver("classpath:/templates", ".html", TemplateMode.HTML, 2, true));

        engine.setEnableSpringELCompiler(true);
        return engine;
    }

    /**
     * TEXT(대체 본문) 전용 템플릿 엔진
     */
    @Bean("mailTextEngine")
    public SpringTemplateEngine mailTextEngine() {
        SpringTemplateEngine engine = new SpringTemplateEngine();

        // 개발 편의(핫리로드)
        engine.addTemplateResolver(
                resolver("file:src/main/resources/templates", ".txt", TemplateMode.TEXT, 1, false));

        // 배포(classpath)
        engine.addTemplateResolver(
                resolver("classpath:/templates", ".txt", TemplateMode.TEXT, 2, true));

        engine.setEnableSpringELCompiler(true);
        return engine;
    }
}
