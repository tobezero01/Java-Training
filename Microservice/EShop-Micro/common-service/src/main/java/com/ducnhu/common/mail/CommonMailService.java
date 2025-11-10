package com.ducnhu.common.mail;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

//@Service
//public class CommonMailService {
//
//    public CommonMailService() { }
@Service
@ConditionalOnClass(JavaMailSender.class)
public class CommonMailService {
    public void sendHtml(JavaMailSender javaMailSender, String from, String fromName,
                         String to, String subject, String html) throws Exception {
        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
        helper.setFrom(from, fromName);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(html, true);
        javaMailSender.send(message);
    }
}
