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

    @Value("${spring.mail.from.name:Care-Up}")
    private String fromName;

    @Value("${careup.mail.support-email:support@careup.com}")
    private String supportEmail;

    @Value("${careup.mail.customer.reset-base-url:https://ordering.careup.com/reset-password}")
    private String resetBaseUrl;

    @Value("${careup.mail.customer.welcome-cta-url:https://ordering.careup.com/customer/home}")
    private String welcomeCtaUrl;

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
            try {
                helper.setFrom(from, fromName);
            } catch (Exception e) {
                helper.setFrom(from);
            }
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

    /** 비밀번호 재설정(텍스트) */
    public void sendPasswordReset(String toEmail, String token) {
        String encEmail = URLEncoder.encode(toEmail, StandardCharsets.UTF_8);
        String encToken = URLEncoder.encode(token, StandardCharsets.UTF_8);
        String link = resetBaseUrl + "?email=" + encEmail + "&token=" + encToken;

        String subject = "[Care-Up] 비밀번호 재설정 안내 (15분 내 유효)";
        String body = """
                안녕하세요.

                비밀번호 재설정을 요청하셨습니다.
                아래 링크로 접속하여 새 비밀번호를 설정해 주세요. (유효시간: 15분)

                재설정 링크: %s

                만약 본인이 요청하지 않았다면 이 메일을 무시하셔도 됩니다.
                """.formatted(link);

        send(toEmail, subject, body);
    }

    /** 회원가입 환영(HTML) — 이름/닉네임 동시 지원 */
    public void sendWelcomeEmail(String toEmail, String name, String nickname) {
        String displayName = buildDisplayName(name, nickname);

        Map<String, Object> model = new HashMap<>();
        model.put("name", name != null ? name : "");
        model.put("nickname", nickname != null ? nickname : "");
        model.put("displayName", displayName);
        model.put("actionUrl", welcomeCtaUrl);
        model.put("supportEmail", supportEmail);
        model.put("year", Year.now().getValue());

        String subject = "[Care-Up] 회원가입을 환영합니다";

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
                  <p style="margin:12px 0 0; color:#6b7280;">© %d Care-Up</p>
                </body></html>
            """.formatted(escapeHtml(displayName), welcomeCtaUrl, welcomeCtaUrl, supportEmail, Year.now().getValue());
        }

        String text = renderSafely(textEngine, "mail/welcome", model);
        if (text == null || text.isBlank()) {
            text = """
                환영합니다 %s 님.

                SHARK 가입이 정상적으로 완료되었습니다.

                지금부터 저희 서비스를 원활하게 이용하실 수 있습니다.

                감사합니다.

                바로가기: %s
                문의: %s

                © %d Care-Up
            """.formatted(defaultString(displayName), welcomeCtaUrl, supportEmail, Year.now().getValue());
        }

        sendHtml(toEmail, subject, html, text);
    }

    /** 기존 시그니처(닉네임 미전달) 호환용 */
    public void sendWelcomeEmail(String toEmail, String name) {
        sendWelcomeEmail(toEmail, name, null);
    }

    private String buildDisplayName(String name, String nickname) {
        String n = defaultString(name).trim();
        String nn = defaultString(nickname).trim();
        if (!nn.isBlank() && !nn.equalsIgnoreCase(n)) {
            return n.isBlank() ? nn : n + " (" + nn + ")";
        }
        return n.isBlank() ? nn : n;
    }

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

    private static String defaultString(String s) { return (s == null) ? "" : s; }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")
                .replace("\"","&quot;").replace("'","&#39;");
    }
}
