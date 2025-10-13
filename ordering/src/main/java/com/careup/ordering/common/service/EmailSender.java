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

    // 고객용 기본 링크로 변경(환경에 맞춰 교체 가능)
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

    /** [Care-Up] 고객 계정 비밀번호 재설정 메일 (링크 포함) */
    public void sendPasswordReset(String toEmail, String token) {
        String encEmail = URLEncoder.encode(toEmail, StandardCharsets.UTF_8);
        String encToken = URLEncoder.encode(token, StandardCharsets.UTF_8);
        String link = resetBaseUrl + "?email=" + encEmail + "&token=" + encToken;

        String subject = "[Care-Up] 고객 계정 비밀번호 재설정 안내 (15분 내 유효)";
        String body = """
                안녕하세요.

                비밀번호 재설정을 요청하셨습니다.
                아래 링크로 접속하여 새 비밀번호를 설정해 주세요. (유효시간: 15분)

                재설정 링크: %s

                만약 본인이 요청하지 않았다면 이 메일을 무시하셔도 됩니다.
                """.formatted(link);

        send(toEmail, subject, body);
    }
}
