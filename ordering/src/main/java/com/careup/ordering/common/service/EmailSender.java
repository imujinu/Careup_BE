package com.careup.ordering.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailSender {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from:no-reply@careup.com}")
    private String from;

    // 고객(주문/프론트)용 비밀번호 재설정 링크 베이스 URL
    @Value("${careup.mail.customer.reset-base-url:https://ordering.careup.com/reset-password}")
    private String resetBaseUrl;

    /** 공용 전송기 */
    public void send(String to, String subject, String body) {
        SimpleMailMessage m = new SimpleMailMessage();
        m.setFrom(from);
        m.setTo(to);
        m.setSubject(subject);
        m.setText(body);
        try {
            mailSender.send(m);
            log.info("[MAIL] sent to={}", to);
        } catch (Exception e) {
            log.error("[MAIL] send failed to={} : {}", to, e.getMessage(), e);
            throw new RuntimeException("이메일 전송에 실패했습니다.");
        }
    }

    /** [Care-Up][고객] 비밀번호 재설정 메일 (링크 포함) */
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

    /** [Care-Up][고객] 회원가입 환영 메일 */
    public void sendWelcomeEmail(String toEmail, String name) {
        String subject = "[Care-Up] 회원가입이 완료되었습니다";
        String body = """
                안녕하세요, %s님.

                Care-Up 고객 회원가입이 정상적으로 완료되었습니다.
                지금 바로 서비스를 이용해 보세요.

                감사합니다.
                """.formatted(name == null ? "" : name);

        send(toEmail, subject, body);
    }
}
