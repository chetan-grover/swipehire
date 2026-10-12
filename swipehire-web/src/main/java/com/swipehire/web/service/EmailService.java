package com.swipehire.web.service;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Sends email with the raw Jakarta Mail API (Session, MimeMessage, Transport) on a background
 * thread, so a slow SMTP server never delays a web request. In dev (mail disabled) the email
 * is printed to the console instead.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final ExecutorService taskPool;
    private final boolean enabled;
    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final String from;

    public EmailService(ExecutorService taskPool,
                        @Value("${swipehire.mail.enabled:false}") boolean enabled,
                        @Value("${swipehire.mail.host:}") String host,
                        @Value("${swipehire.mail.port:587}") int port,
                        @Value("${swipehire.mail.username:}") String username,
                        @Value("${swipehire.mail.password:}") String password,
                        @Value("${swipehire.mail.from:no-reply@swipehire.in}") String from) {
        this.taskPool = taskPool;
        this.enabled = enabled;
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.from = from;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public CompletableFuture<Void> sendAsync(String to, String subject, String body) {
        return CompletableFuture.runAsync(() -> send(to, subject, body), taskPool)
                .exceptionally(ex -> {
                    log.warn("Email to {} failed: {}", to, ex.getMessage());
                    return null;
                });
    }

    void send(String to, String subject, String body) {
        if (!enabled) {
            log.info("\n===== EMAIL (dev mode, printed instead of sent) =====\nTo: {}\nSubject: {}\n\n{}\n=====================================================",
                    to, subject, body);
            return;
        }
        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", String.valueOf(port));
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
        try {
            MimeMessage msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(from));
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            msg.setSubject(subject);
            msg.setText(body);
            Transport.send(msg);
        } catch (MessagingException e) {
            throw new IllegalStateException(e);
        }
    }
}
