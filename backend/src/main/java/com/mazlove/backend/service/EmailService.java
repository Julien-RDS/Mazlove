package com.mazlove.backend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
  private final JavaMailSender mailSender;
  private final String from;

  public EmailService(
      JavaMailSender mailSender, @Value("${mail.from:no-reply@mazlove.fr}") String from) {
    this.mailSender = mailSender;
    this.from = from;
  }

  public void sendText(String to, String subject, String text) {
    var message = new org.springframework.mail.SimpleMailMessage();
    message.setFrom(from);
    message.setTo(to);
    message.setSubject(subject);
    message.setText(text);
    mailSender.send(message);
  }

  public void sendOtpEmail(String to, String otp) {
    // Le code est produit par MfaService ; vérifier son format évite toute injection HTML.
    if (otp == null || !otp.matches("[0-9]{6}")) {
      throw new IllegalArgumentException("Le code OTP doit contenir six chiffres");
    }
    MimeMessage message = mailSender.createMimeMessage();
    try {
      MimeMessageHelper helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());
      helper.setFrom(from);
      helper.setTo(to);
      helper.setSubject("Votre code Mazlove : " + otp);
      helper.setText(
          "<p>Votre code de connexion Mazlove :</p>"
              + "<p style=\"font-size: 32px; font-weight: bold\">"
              + otp
              + "</p>"
              + "<p>Ce code est valable 5 minutes.</p>",
          true);
    } catch (MessagingException e) {
      throw new MailPreparationException("Impossible de préparer l'email OTP", e);
    }
    mailSender.send(message);
  }
}
