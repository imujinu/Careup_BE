package com.careup.branch.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailSender {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from:no-reply@careup.com}")
    private String from;

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

    /** [Care-Up] 지점 계정 비밀번호 재설정 메일 템플릿 */
    public void sendPasswordReset(String toEmail, String token) {
        String subject = "[Care-Up] 지점 계정 비밀번호 재설정 안내";
        String body = """
                안녕하세요.

                아래 토큰으로 15분 내 비밀번호를 재설정하세요.

                이메일: %s
                토큰: %s

                이 메일을 요청하지 않으셨다면 무시하셔도 됩니다.
                """.formatted(toEmail, token);
        send(toEmail, subject, body);
    }
}
