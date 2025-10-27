package com.careup.ordering.common.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.exceptions.TemplateInputException;

@Slf4j
@Component
public class EmailSender {

    private final JavaMailSender mailSender;
    private final TemplateEngine htmlEngine;
    private final TemplateEngine textEngine;

    @Value("${spring.mail.from:no-reply@careup.com}")
    private String from;

    // 고객 메일 From 표시명 기본값 SHARK
    @Value("${spring.mail.from.name:SHARK}")
    private String fromName;

    @Value("${careup.mail.support-email:support@careup.com}")
    private String supportEmail;

    // 고객 비번 재설정 링크(프론트 고객 앱)
    @Value("${careup.mail.customer.reset-base-url:https://ordering.careup.com/reset-password}")
    private String resetBaseUrl;

    // (환영 메일 CTA)
    @Value("${careup.mail.customer.welcome-cta-url:https://ordering.careup.com/customer/home}")
    private String welcomeCtaUrl;

    // ===== 브랜드(고객: SHARK) — top-level `brand.*` 사용 =====
    @Value("${brand.name:SHARK}")
    private String brandName;

    @Value("${brand.slogan:KICKS RULE EVERYTHING AROUND ME}")
    private String brandSlogan;

    @Value("${brand.logo-url:}")
    private String brandLogoUrl;

    // 고객 버튼색: 검정
    @Value("${brand.cta-color:#111827}")
    private String brandCtaColor;

    @Autowired
    public EmailSender(
            JavaMailSender mailSender,
            @Qualifier("mailHtmlEngine") TemplateEngine htmlEngine,
            @Qualifier("mailTextEngine") TemplateEngine textEngine
    ) {
        this.mailSender = mailSender;
        this.htmlEngine = htmlEngine;
        this.textEngine = textEngine;
    }

    /** 순수 텍스트 메일 */
    public void send(String to, String subject, String body) {
        SimpleMailMessage m = new SimpleMailMessage();
        m.setFrom(from);
        m.setTo(to);
        m.setSubject(subject);
        m.setText(body);
        try {
            mailSender.send(m);
            log.info("[MAIL] sent (text) to={}", to);
        } catch (MailException e) {
            log.error("[MAIL] send failed to={} : {}", to, e.getMessage(), e);
            throw new RuntimeException("이메일 전송에 실패했습니다.");
        }
    }

    /** HTML + 텍스트 대체 본문 */
    public void sendHtml(String to, String subject, String html, String textAlt) {
        MimeMessage msg = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, StandardCharsets.UTF_8.name());
            try { helper.setFrom(from, fromName); } catch (Exception e) { helper.setFrom(from); }
            helper.setReplyTo(supportEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText((textAlt != null && !textAlt.isBlank()) ? textAlt : " ", html);

            mailSender.send(msg);
            log.info("[MAIL] sent (html) to={}", to);
        } catch (MessagingException e) {
            log.error("[MAIL] send html failed to={} : {}", to, e.getMessage(), e);
            throw new RuntimeException("이메일 전송에 실패했습니다.");
        }
    }

    /** 비밀번호 재설정(고객) — 공용 템플릿 사용 */
    public void sendPasswordReset(String toEmail, String token) {
        String encEmail = URLEncoder.encode(toEmail, StandardCharsets.UTF_8);
        String encToken = URLEncoder.encode(token, StandardCharsets.UTF_8);
        String link = resetBaseUrl + "?email=" + encEmail + "&token=" + encToken;

        Map<String, Object> model = new HashMap<>();
        model.put("brandName", brandName);
        model.put("brandSlogan", brandSlogan);
        model.put("brandLogoUrl", brandLogoUrl);
        model.put("brandCtaColor", brandCtaColor);
        model.put("supportEmail", supportEmail);
        model.put("actionUrl", link);
        model.put("year", Year.now().getValue());

        String subject = "[" + brandName + "] 비밀번호 재설정 안내 (15분 내 유효)";

        String html = renderSafely(htmlEngine, "mail/password_reset", model);
        if (html == null || html.isBlank()) {
            html = fallbackHtml(link);
        }
        String text = renderSafely(textEngine, "mail/password_reset", model);
        if (text == null || text.isBlank()) {
            text = fallbackText(link);
        }

        sendHtml(toEmail, subject, html, text);
    }

    /** 회원가입 환영(HTML) — 이름/닉네임 동시 지원(기존 시그니처 유지) */
    public void sendWelcomeEmail(String toEmail, String name, String nickname) {
        String displayName = buildDisplayName(name, nickname);

        Map<String, Object> model = new HashMap<>();
        model.put("name", defaultString(name));
        model.put("nickname", defaultString(nickname));
        model.put("displayName", displayName);
        model.put("actionUrl", welcomeCtaUrl);
        model.put("supportEmail", supportEmail);
        model.put("year", Year.now().getValue());

        String subject = "[" + brandName + "] 회원가입을 환영합니다";

        String html = renderSafely(htmlEngine, "mail/welcome", model);
        if (html == null || html.isBlank()) {
            html = """
                <html><body style="font-family:-apple-system, Segoe UI, Roboto, Arial, 'Noto Sans KR', sans-serif; color:#111827;">
                  <h2 style="margin:0 0 10px;">환영합니다
                    <span style="color:#2563eb; font-weight:800;">%s</span> 님.
                  </h2>
                  <p style="margin:0 0 10px;">SHARK 가입이 정상적으로 완료되었습니다.</p>
                  <p style="margin:0 0 10px;">지금부터 저희 서비스를 원활하게 이용하실 수 있습니다.</p>
                  <p style="margin:0 0 18px;">감사합니다.</p>
                  <p style="margin:0 0 6px;">바로가기: <a href="%s">%s</a></p>
                  <p style="margin:0 0 6px;">문의: %s</p>
                  <p style="margin:12px 0 0; color:#6b7280;">© %d %s</p>
                </body></html>
            """.formatted(escapeHtml(displayName), welcomeCtaUrl, welcomeCtaUrl, supportEmail, Year.now().getValue(), escapeHtml(brandName));
        }

        String text = renderSafely(textEngine, "mail/welcome", model);
        if (text == null || text.isBlank()) {
            text = """
                환영합니다 %s 님.

                %s 가입이 정상적으로 완료되었습니다.

                지금부터 저희 서비스를 원활하게 이용하실 수 있습니다.

                감사합니다.

                바로가기: %s
                문의: %s

                © %d %s
            """.formatted(defaultString(displayName), brandName, welcomeCtaUrl, supportEmail, Year.now().getValue(), brandName);
        }

        sendHtml(toEmail, subject, html, text);
    }

    /** 기존 시그니처(닉네임 미전달) 호환용 */
    public void sendWelcomeEmail(String toEmail, String name) {
        sendWelcomeEmail(toEmail, name, null);
    }

    // ===== 내부 유틸 =====

    private String renderSafely(TemplateEngine engine, String templateName, Map<String, Object> model) {
        try {
            Context ctx = new Context();
            if (model != null) model.forEach(ctx::setVariable);
            return engine.process(templateName, ctx);
        } catch (TemplateInputException tie) {
            log.warn("[MAIL][TPL] render failed name={} : {}", templateName, tie.getMessage());
            return null;
        } catch (Exception e) {
            log.warn("[MAIL][TPL] render error name={} : {}", templateName, e.getMessage());
            return null;
        }
    }

    private String fallbackHtml(String link) {
        return """
            <html><body style="font-family:-apple-system, Segoe UI, Roboto, Arial, 'Noto Sans KR', sans-serif; color:#111827;">
              <h2 style="margin:0 0 10px;">비밀번호 재설정</h2>
              <p style="margin:0 0 8px;">아래 버튼을 눌러 새 비밀번호를 설정하세요. (유효시간: 15분)</p>
              <p style="margin:16px 0;">
                <a href="%s" style="background:%s;color:#fff;padding:12px 18px;border-radius:10px;text-decoration:none;font-weight:700;">비밀번호 재설정</a>
              </p>
              <p style="margin:8px 0 0;color:#6b7280;">링크가 동작하지 않으면 아래 주소를 복사해 브라우저에 붙여넣기 하세요.</p>
              <p style="margin:0 0 8px;"><a href="%s">%s</a></p>
              <p style="margin:12px 0 0;color:#6b7280;">문의: %s</p>
              <p style="margin:12px 0 0;color:#9ca3af;">© %d %s</p>
            </body></html>
        """.formatted(link, brandCtaColor, link, link, supportEmail, Year.now().getValue(), brandName);
    }

    private String fallbackText(String link) {
        return """
            [%s] 비밀번호 재설정 안내 (15분 내 유효)

            아래 링크로 접속하여 새 비밀번호를 설정하세요.
            %s

            링크가 동작하지 않으면 주소를 복사해 브라우저에 붙여넣기 해주세요.
            문의: %s
            © %d %s
        """.formatted(brandName, link, supportEmail, Year.now().getValue(), brandName);
    }

    private static String defaultString(String s) { return (s == null) ? "" : s; }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")
                .replace("\"","&quot;").replace("'","&#39;");
    }

    private String buildDisplayName(String name, String nickname) {
        String n = defaultString(name).trim();
        String nn = defaultString(nickname).trim();
        if (!nn.isBlank() && !nn.equalsIgnoreCase(n)) {
            return n.isBlank() ? nn : n + " (" + nn + ")";
        }
        return n.isBlank() ? nn : n;
    }
}
