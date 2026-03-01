package com.project.notificationService.Service;

import com.project.notificationService.Model.Subjects;
import lombok.AllArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;

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
        
        org.thymeleaf.context.Context context = new org.thymeleaf.context.Context();
        context.setVariable("name", name);
        context.setVariable("email", email);
        context.setVariable("age", age);
        context.setVariable("role", role);
        context.setVariable("isPatient", isPatient);
        
        String content = templateEngine.process("welcome-email", context);
        sendEmail(email, Subjects.WELCOME.getSubject(), content);
    }

    private void sendEmail(String to,String subject,String content){
        MimeMessagePreparator messagePreparator = mimeMessage -> {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);

        };
        mailSender.send(messagePreparator);
    }

}
