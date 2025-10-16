package com.ducnhu.common.mail;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommonMailService {

    private final JavaMailSender mailSender;

    public void sendHtml(JavaMailSender javaMailSender, String from, String fromName, String to, String subject, String html) throws Exception {
        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
        helper.setFrom(from, fromName);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(html, true);
        javaMailSender.send(message);
    }
}
