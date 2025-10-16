package com.ducnhu.common.mail;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

public final class MailUtil {
    private MailUtil() {
    }

    public static JavaMailSender buildSender(String host, int port, String username, String password,
                                             String smtpAuth, String smtpSecured) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        sender.setUsername(username);
        sender.setPassword(password);
        Properties properties = new Properties();
        properties.setProperty("mail.smtp.auth", smtpAuth);
        properties.setProperty("mail.smtp.starttls.enable", smtpSecured);
        properties.setProperty("mail.smtp.ssl.trust", "*");
        properties.setProperty("mail.smtp.ssl.checkserveridentity", "false");
        sender.setJavaMailProperties(properties);
        return sender;
    }
}
