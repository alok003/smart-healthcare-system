package com.project.notificationService.Service;

import com.project.notificationService.Model.Subjects;
import lombok.AllArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.util.Map;

@Component
@AllArgsConstructor
public class EmailService {

    private JavaMailSender mailSender;
    private TemplateEngine templateEngine;

    public void sendWelcomeEmail(Map<String,Object> details) {
        String email = (String) details.get("userEmail");
        String name = (String) details.get("userName");
        Integer age = (Integer) details.get("userAge");
        String role = (String) details.get("role");
        Boolean isPatient = (Boolean) details.get("isPatient");
        
        Context context = new Context();
        context.setVariable("name", name);
        context.setVariable("email", email);
        context.setVariable("age", age);
        context.setVariable("role", role);
        context.setVariable("isPatient", isPatient);
        
        String content = templateEngine.process("welcome-email", context);
        sendEmail(email, Subjects.WELCOME.getSubject(), content, null, null);
    }

    public void sendAppointmentEmail(Map<String, Object> details) {
        String patientEmail = (String) details.get("patientId");

        Context emailContext = new Context();
        emailContext.setVariables(details);
        String emailContent = templateEngine.process("appointment-email", emailContext);

        Context pdfContext = new Context();
        pdfContext.setVariables(details);
        String pdfHtml = templateEngine.process("appointment-pdf", pdfContext);
        byte[] pdfBytes = generatePdf(pdfHtml);

        sendEmail(patientEmail, Subjects.PRESCRIPTION_READY.getSubject(), emailContent, pdfBytes, "prescription.pdf");
    }

    private byte[] generatePdf(String html) {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(os);
            return os.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate appointment PDF", e);
        }
    }

    private void sendEmail(String to, String subject, String content, byte[] attachment, String attachmentName) {
        MimeMessagePreparator messagePreparator = mimeMessage -> {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, attachment != null);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);
            if (attachment != null) {
                helper.addAttachment(attachmentName, () -> new java.io.ByteArrayInputStream(attachment));
            }
        };
        mailSender.send(messagePreparator);
    }
}
