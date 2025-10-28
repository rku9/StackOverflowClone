package com.mountblue.stackoverflowclone.services;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.mountblue.stackoverflowclone.dtos.EmailTaskDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final SendGrid sendGrid;
    private final String fromEmail;
    private final String fromName;

    public EmailService(@Value("${sendgrid.api-key}") String apiKey,
                        @Value("${sendgrid.from-email}") String fromEmail,
                        @Value("${sendgrid.from-name:StackOverflowClone}") String fromName) {
        this.sendGrid = new SendGrid(apiKey);
        this.fromEmail = fromEmail;
        this.fromName = fromName;
    }

    public boolean sendEmail(String toEmail, String subject, String body) {
        Email from = new Email(fromEmail, fromName);
        Email to = new Email(toEmail);
        Content content = new Content("text/html", body);
        Mail mail = new Mail(from, subject, to, content);

        Request request = new Request();
        try {
            String preview = previewBody(body, 200);
            logger.info("Sending email via SendGrid: to={}, subject={}, bodyPreview=\"{}\"", toEmail, subject, preview);
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sendGrid.api(request);

            int statusCode = response.getStatusCode();
            if (statusCode >= 400) {
                logger.error("SendGrid responded with status {} and body {}", statusCode, response.getBody());
                return false;
            }

            logger.info("SendGrid accepted: status={}, to={}, subject={}", statusCode, toEmail, subject);
            return true;
        } catch (IOException ex) {
            logger.error("Failed to send email via SendGrid", ex);
            return false;
        }
    }

    public boolean sendEmail(EmailTaskDto dto) {
        return sendEmail(dto.getEmail(), dto.getSubject(), dto.getBody());
    }

    private String previewBody(String body, int maxLen) {
        if (body == null) return "";
        String cleaned = body.replace("\n", "\\n");
        return cleaned.length() <= maxLen ? cleaned : cleaned.substring(0, maxLen) + "...";
    }
}
