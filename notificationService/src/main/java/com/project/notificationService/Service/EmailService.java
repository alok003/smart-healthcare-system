package com.project.notificationService.Service;

import com.project.notificationService.Model.Subjects;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

@Component
@AllArgsConstructor
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private JavaMailSender mailSender;
    private TemplateEngine templateEngine;

    public void sendWelcomeEmail(Map<String, Object> details) {
        String email = (String) details.get("userEmail");
        Context context = new Context();
        context.setVariable("name", details.get("userName"));
        context.setVariable("email", email);
        context.setVariable("age", details.get("userAge"));
        context.setVariable("role", details.get("userRole"));
        String content = templateEngine.process("welcome-email", context);
        sendEmail(email, Subjects.WELCOME.getSubject(), content, null, null);
    }

    public void sendRoleApprovedEmail(Map<String, Object> details) {
        String email = (String) details.get("userEmail");
        Context context = new Context();
        context.setVariable("email", email);
        context.setVariable("role", details.get("userRole"));
        String content = templateEngine.process("role-approved-email", context);
        sendEmail(email, Subjects.ROLE_APPROVED.getSubject(), content, null, null);
    }

    public void sendRoleDeclinedEmail(Map<String, Object> details) {
        String email = (String) details.get("userEmail");
        Context context = new Context();
        context.setVariable("email", email);
        context.setVariable("role", details.get("userRole"));
        String content = templateEngine.process("role-declined-email", context);
        sendEmail(email, Subjects.ROLE_DECLINED.getSubject(), content, null, null);
    }

    public void sendAppointmentBookedEmail(Map<String, Object> details) {
        String patientEmail = (String) details.get("patientId");
        String doctorEmail = (String) details.get("doctorId");
        Context context = new Context();
        context.setVariable("appointmentId", details.get("id"));
        context.setVariable("patientEmail", patientEmail);
        context.setVariable("doctorEmail", doctorEmail);
        context.setVariable("date", details.get("date"));
        context.setVariable("subject", details.get("subject"));
        context.setVariable("status", details.get("status"));
        String patientContent = templateEngine.process("appointment-booked-patient-email", context);
        sendEmail(patientEmail, Subjects.APPOINTMENT_BOOKED.getSubject(), patientContent, null, null);
        String doctorContent = templateEngine.process("appointment-booked-doctor-email", context);
        sendEmail(doctorEmail, Subjects.APPOINTMENT_BOOKED.getSubject(), doctorContent, null, null);
    }

    public void sendAppointmentCancelledEmail(Map<String, Object> details) {
        String patientEmail = (String) details.get("patientId");
        if (patientEmail == null) {
            log.warn("action=SEND_EMAIL status=SKIPPED topic=appointment-cancelled-notification reason=PATIENT_EMAIL_NULL");
            return;
        }
        Context context = new Context();
        context.setVariable("appointmentId", details.getOrDefault("id", details.get("appointmentId")));
        context.setVariable("patientEmail", patientEmail);
        context.setVariable("doctorEmail", details.get("doctorId"));
        context.setVariable("date", details.get("date"));
        context.setVariable("cancelledBy", details.get("cancelledBy") != null ? details.get("cancelledBy") : details.get("description"));
        String content = templateEngine.process("appointment-cancelled-email", context);
        sendEmail(patientEmail, Subjects.APPOINTMENT_CANCELLED.getSubject(), content, null, null);
    }

    public void sendAppointmentCompletedEmail(Map<String, Object> details) {
        String patientEmail = (String) details.get("patientId");
        Context context = new Context();
        context.setVariable("appointmentId", details.get("id"));
        context.setVariable("patientEmail", patientEmail);
        context.setVariable("doctorEmail", details.get("doctorId"));
        context.setVariable("date", details.get("date"));
        context.setVariable("visitDetails", details.get("visitDetails"));
        String content = templateEngine.process("appointment-completed-email", context);
        sendEmail(patientEmail, Subjects.APPOINTMENT_COMPLETED.getSubject(), content, null, null);
    }

    public void sendPrescriptionEmail(Map<String, Object> details) {
        String patientEmail = (String) details.get("patientId");
        Context emailContext = new Context();
        emailContext.setVariables(details);
        String emailContent = templateEngine.process("prescription-email", emailContext);
        Context pdfContext = new Context();
        pdfContext.setVariables(details);
        String pdfHtml = templateEngine.process("prescription-pdf", pdfContext);
        byte[] pdfBytes = generatePdf(pdfHtml);
        sendEmail(patientEmail, Subjects.PRESCRIPTION_READY.getSubject(), emailContent, pdfBytes, "prescription.pdf");
    }

    @SuppressWarnings("unchecked")
    public void sendDoctorDailyScheduleEmail(Map<String, Object> details) {
        String doctorEmail = (String) details.get("doctorEmail");
        Context context = new Context();
        context.setVariable("doctorEmail", doctorEmail);
        context.setVariable("date", details.get("date"));
        context.setVariable("appointmentIds", (List<String>) details.get("appointmentIds"));
        String content = templateEngine.process("doctor-daily-schedule-email", context);
        sendEmail(doctorEmail, Subjects.DOCTOR_DAILY_SCHEDULE.getSubject(), content, null, null);
    }

    protected void sendEmail(String to, String subject, String content, byte[] attachment, String attachmentName) {
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
        log.info("action=SEND_EMAIL status=SUCCESS recipient={} subject={}", to, subject);
    }

    protected byte[] generatePdf(String html) {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(os);
            return os.toByteArray();
        } catch (Exception e) {
            log.error("action=GENERATE_PDF status=FAILED error={}", e.getMessage());
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }
}
