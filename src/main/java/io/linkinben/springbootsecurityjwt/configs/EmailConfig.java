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
		props.put("mail.imap.auth.mechanisms", "XOAUTH2");
		props.put("mail.smtp.proxy.host", "rb-proxy-apac.bosch.com");
		props.put("mail.smtp.proxy.port", "8080");
		Session session = Session.getInstance(props);
		Store store;
		try {
			store = session.getStore("imap");
			try {
				store.connect("imap.gmail.com", "peterpans2025@gmail.com",
						"ya29.a0AfH6SMBcYmWCgENd_e8i_HjAEVQ-yD-qQqeUiM6y3RdTUpd7hq-oZEWL7zvdKG0O4C6H50GsJ5lFwD-xn6ZG5wDVP4E5ocp_cVVn0TEbtjy9B7ZZwl0Yo1X8rVMjVionJffnLpp1Sj-zY5ywcnuuamwyefUy");
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
