package io.linkinben.springbootsecurityjwt.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Component;

import io.linkinben.springbootsecurityjwt.configs.EmailConfig;

@Component
public class EmailUtils {
	@Autowired
	private EmailConfig emailConfig;
	

	public void sendSimpleEmail(String to, String content) {
        SimpleMailMessage message = new SimpleMailMessage(); 
        message.setFrom("thienan.nguyenhoang311@gmail.com");
        message.setTo(to); 
        message.setSubject("Sup Bitch"); 
        message.setText(content);
		// emailConfig.getJavaMailSender().send(message);
        emailConfig.getJavaMailSender().send(message);
	}
}
