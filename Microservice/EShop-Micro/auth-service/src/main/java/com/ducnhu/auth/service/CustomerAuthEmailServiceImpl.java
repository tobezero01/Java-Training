package com.ducnhu.auth.service;

import com.ducnhu.auth.entity.Customer;
import com.ducnhu.auth.setting.EmailSettingBag;
import com.ducnhu.auth.setting.Utility;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

@Service
@RequiredArgsConstructor
public class CustomerAuthEmailServiceImpl implements CustomerAuthEmailService {

    private final SettingService settingService;
    private final JavaMailSender mailSender; // TIÊM BEAN

    @Override
    public void sendRegistrationVerification(HttpServletRequest req, Customer customer)
            throws MessagingException, UnsupportedEncodingException {

        EmailSettingBag bag = settingService.getEmailSettings();
        String subject = bag.getCustomerVerifySubject();
        String content = bag.getCustomerVerifyContent(); // có [[name]] [[URL]]

        // Nên trỏ thẳng vào API verify (hoặc trỏ FE rồi FE gọi API — tuỳ kiến trúc)
        String verifyUrl = Utility.getSiteURL(req) + "/api/auth/verify?code=" + customer.getVerificationCode();

        MimeMessage msg = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(msg, "UTF-8");
        helper.setFrom(bag.getMailFrom(), bag.getSenderName());
        helper.setTo(customer.getEmail());
        helper.setSubject(subject);
        helper.setText(
                content.replace("[[name]]", safe(customer.getFullName()))
                        .replace("[[URL]]", verifyUrl),
                true
        );
        mailSender.send(msg);
    }

    /** Email reset mật khẩu */
    @Override
    public void sendResetPassword(HttpServletRequest req, String email, String token)
            throws MessagingException, UnsupportedEncodingException {

        EmailSettingBag bag = settingService.getEmailSettings();
        JavaMailSenderImpl mailer = Utility.prepareMailSender(bag);

        String subject = coalesce(
                bag.getValue("CUSTOMER_RESET_PASSWORD_SUBJECT"),
                "Here's the link to reset password"
        );
        String content = coalesce(
                bag.getValue("CUSTOMER_RESET_PASSWORD_CONTENT"),
                """
                <p>Hello,</p>
                <p>Click the link below to change your password</p>
                <p><a href="[[URL]]">Change password</a></p>
                <p>Thanks</p>
                """
        );

        // FE page: người dùng mở link này, nhập mật khẩu mới, FE gọi POST /api/auth/reset-password
        String link = Utility.getSiteURL(req) + "/reset-password?token=" + token;

        MimeMessage msg = mailer.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(msg, "UTF-8");
        helper.setFrom(bag.getMailFrom(), bag.getSenderName());
        helper.setTo(email);
        helper.setSubject(subject);
        helper.setText(content.replace("[[URL]]", link), true);
        mailer.send(msg);
    }

    private static String coalesce(String v, String def) { return (v == null || v.isBlank()) ? def : v; }
    private static String safe(String s){ return s == null ? "" : s; }
}