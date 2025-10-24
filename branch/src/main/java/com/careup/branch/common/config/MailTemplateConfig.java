package com.careup.branch.common.config;

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

    /** HTML 전용 템플릿 엔진 */
    @Primary
    @Bean("mailHtmlEngine")
    public SpringTemplateEngine mailHtmlEngine() {
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.addTemplateResolver(resolver("file:src/main/resources/templates", ".html", TemplateMode.HTML, 1, false));
        engine.addTemplateResolver(resolver("classpath:/templates", ".html", TemplateMode.HTML, 2, true));
        engine.setEnableSpringELCompiler(true);
        return engine;
    }

    /** TEXT(대체 본문) 전용 템플릿 엔진 */
    @Bean("mailTextEngine")
    public SpringTemplateEngine mailTextEngine() {
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.addTemplateResolver(resolver("file:src/main/resources/templates", ".txt", TemplateMode.TEXT, 1, false));
        engine.addTemplateResolver(resolver("classpath:/templates", ".txt", TemplateMode.TEXT, 2, true));
        engine.setEnableSpringELCompiler(true);
        return engine;
    }
}
