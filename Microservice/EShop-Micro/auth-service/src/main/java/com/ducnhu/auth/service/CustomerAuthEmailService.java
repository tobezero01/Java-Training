package com.ducnhu.auth.service;

import com.ducnhu.auth.entity.Customer;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;

import java.io.UnsupportedEncodingException;

public interface CustomerAuthEmailService {
    void sendRegistrationVerification(HttpServletRequest req, Customer customer)
            throws MessagingException, UnsupportedEncodingException;

    void sendResetPassword(HttpServletRequest req, String email, String token)
            throws MessagingException, UnsupportedEncodingException;
}
