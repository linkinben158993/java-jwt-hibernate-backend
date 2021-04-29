package io.linkinben.springbootsecurityjwt.configs;

import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Configuration
public class EmailConfig {

	@Bean
	public JavaMailSender getJavaMailSender() {
		JavaMailSenderImpl javaMailSenderImpl = new JavaMailSenderImpl();
		javaMailSenderImpl.setHost("smtp.gmail.com");
		javaMailSenderImpl.setPort(465);

		Properties props = javaMailSenderImpl.getJavaMailProperties();

		props.put("mail.imap.ssl.enable", "true"); // required for Gmail
		props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "465");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.socketFactory.port", "465");
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
		Session session = Session.getInstance(props);
		Store store;
		try {
			store = session.getStore("imap");
			try {
				store.connect("imap.gmail.com", "thienan.nguyenhoang.411@gmail.com", "Thienanvip@4321");
			} catch (MessagingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (NoSuchProviderException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		javaMailSenderImpl.setJavaMailProperties(props);
		return javaMailSenderImpl;

	}

	@Bean
	public SimpleMailMessage emailTemplate() {
		SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
		simpleMailMessage.setFrom("peterpans2025@gmail.com");
		return simpleMailMessage;
	}

}
