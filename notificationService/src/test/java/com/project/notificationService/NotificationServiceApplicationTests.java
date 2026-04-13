package com.project.notificationService;

import com.project.notificationService.Service.EmailService;
import com.project.notificationService.Service.KafkaListenerNotification;
import com.project.notificationService.Utility.LogUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import jakarta.mail.internet.MimeMessage;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class NotificationServiceApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private KafkaListenerNotification kafkaListenerNotification;

    @MockitoBean
    private JavaMailSender mailSender;

    @MockitoBean
    private SpringTemplateEngine templateEngine;

    // --- health ---

    @Test
    void health_returnsTrue() throws Exception {
        mockMvc.perform(get("/api/notification-service/open/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    // --- LogUtil ---

    @Test
    void logUtil_toJson_returnsJson() {
        String json = LogUtil.toJson(Map.of("userEmail", "test@example.com", "userRole", "USER"));
        assertNotNull(json);
        assertTrue(json.contains("test@example.com"));
    }

    @Test
    void logUtil_masksPassword() {
        String json = LogUtil.toJson(Map.of("userPassword", "secret123", "userEmail", "test@example.com"));
        assertTrue(json.contains("***"));
        assertFalse(json.contains("secret123"));
    }

    @Test
    void logUtil_truncatesLongPayload() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 600; i++) sb.append("a");
        assertTrue(LogUtil.toJson(sb.toString()).contains("[truncated]"));
    }

    @Test
    void logUtil_nullObject_returnsNull() {
        assertEquals("null", LogUtil.toJson(null));
    }

    // --- EmailService ---

    private EmailService buildEmailService() {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html>test</html>");
        return new EmailService(mailSender, templateEngine);
    }

    @Test
    void sendWelcomeEmail_sendsEmail() {
        EmailService emailService = buildEmailService();
        emailService.sendWelcomeEmail(Map.of(
                "userEmail", "test@example.com", "userName", "Test User",
                "userAge", 25, "userRole", "USER"));
        verify(mailSender).send(any(org.springframework.mail.javamail.MimeMessagePreparator.class));
    }

    @Test
    void sendRoleApprovedEmail_sendsEmail() {
        EmailService emailService = buildEmailService();
        emailService.sendRoleApprovedEmail(Map.of("userEmail", "test@example.com", "userRole", "DOCTOR"));
        verify(mailSender).send(any(org.springframework.mail.javamail.MimeMessagePreparator.class));
    }

    @Test
    void sendRoleDeclinedEmail_sendsEmail() {
        EmailService emailService = buildEmailService();
        emailService.sendRoleDeclinedEmail(Map.of("userEmail", "test@example.com", "userRole", "DOCTOR"));
        verify(mailSender).send(any(org.springframework.mail.javamail.MimeMessagePreparator.class));
    }

    @Test
    void sendAppointmentBookedEmail_sendsTwoEmails() {
        EmailService emailService = buildEmailService();
        emailService.sendAppointmentBookedEmail(Map.of(
                "id", "appt-1", "patientId", "patient@example.com",
                "doctorId", "doctor@example.com", "date", "2026-05-01",
                "subject", "Checkup", "status", "UPCOMING"));
        verify(mailSender, times(2)).send(any(org.springframework.mail.javamail.MimeMessagePreparator.class));
    }

    @Test
    void sendAppointmentCancelledEmail_nullPatient_skips() {
        EmailService emailService = buildEmailService();
        emailService.sendAppointmentCancelledEmail(Map.of(
                "appointmentId", "appt-1", "date", "2026-05-01", "cancelledBy", "doctor@example.com"));
        verify(mailSender, never()).send(any(org.springframework.mail.javamail.MimeMessagePreparator.class));
    }

    @Test
    void sendAppointmentCancelledEmail_withPatient_sendsEmail() {
        EmailService emailService = buildEmailService();
        emailService.sendAppointmentCancelledEmail(Map.of(
                "id", "appt-1", "patientId", "patient@example.com",
                "doctorId", "doctor@example.com", "date", "2026-05-01",
                "cancelledBy", "patient@example.com"));
        verify(mailSender).send(any(org.springframework.mail.javamail.MimeMessagePreparator.class));
    }

    @Test
    void sendAppointmentCompletedEmail_sendsEmail() {
        EmailService emailService = buildEmailService();
        emailService.sendAppointmentCompletedEmail(Map.of(
                "id", "appt-1", "patientId", "patient@example.com",
                "doctorId", "doctor@example.com", "date", "2026-05-01"));
        verify(mailSender).send(any(org.springframework.mail.javamail.MimeMessagePreparator.class));
    }

    @Test
    void sendDoctorDailyScheduleEmail_sendsEmail() {
        EmailService emailService = buildEmailService();
        emailService.sendDoctorDailyScheduleEmail(Map.of(
                "doctorEmail", "doctor@example.com", "date", "2026-05-01",
                "appointmentIds", List.of("appt-1", "appt-2")));
        verify(mailSender).send(any(org.springframework.mail.javamail.MimeMessagePreparator.class));
    }
}
