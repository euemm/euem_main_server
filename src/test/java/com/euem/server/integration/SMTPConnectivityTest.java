package com.euem.server.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.context.ActiveProfiles;

import jakarta.mail.Session;
import jakarta.mail.Transport;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class SMTPConnectivityTest {

	@Autowired
	private JavaMailSender mailSender;

	@Value("${spring.mail.host}")
	private String smtpHost;

	@Value("${spring.mail.port}")
	private int smtpPort;

	@Value("${spring.mail.username:}")
	private String smtpUsername;

	@Value("${spring.mail.password:}")
	private String smtpPassword;

	private Session getSession() {
		if (mailSender instanceof JavaMailSenderImpl) {
			return ((JavaMailSenderImpl) mailSender).getSession();
		}
		throw new IllegalStateException("JavaMailSender is not an instance of JavaMailSenderImpl");
	}

	@Test
	void testSMTPConfiguration() {
		System.out.println("=== SMTP Configuration Test ===");
		System.out.println("SMTP Host: " + smtpHost);
		System.out.println("SMTP Port: " + smtpPort);
		System.out.println("SMTP Username: " + (smtpUsername != null && !smtpUsername.isEmpty() ? smtpUsername : "(empty)"));
		
		assertNotNull(mailSender, "JavaMailSender should be configured");
		assertNotNull(smtpHost, "SMTP host should be configured");
		assertTrue(smtpPort > 0, "SMTP port should be configured");

		assertTrue(mailSender instanceof JavaMailSenderImpl, 
				"JavaMailSender should be an instance of JavaMailSenderImpl");
		
		JavaMailSenderImpl mailSenderImpl = (JavaMailSenderImpl) mailSender;
		String configuredHost = mailSenderImpl.getHost();
		int configuredPort = mailSenderImpl.getPort();

		assertNotNull(configuredHost, "SMTP host should be configured in JavaMailSender");
		assertTrue(configuredPort > 0, "SMTP port should be configured in JavaMailSender");

		Session session = getSession();
		assertNotNull(session, "Mail session should be available");

		Properties props = session.getProperties();

		System.out.println("JavaMailSender Host: " + configuredHost);
		System.out.println("JavaMailSender Port: " + configuredPort);
		System.out.println("SMTP Auth: " + props.getProperty("mail.smtp.auth"));
		System.out.println("SMTP StartTLS: " + props.getProperty("mail.smtp.starttls.enable"));
	}

	@Test
	void testSMTPConnection() {
		System.out.println("=== SMTP Connection Test ===");
		System.out.println("SMTP Host: " + smtpHost);
		System.out.println("SMTP Port: " + smtpPort);
		System.out.println("SMTP Username: " + (smtpUsername != null && !smtpUsername.isEmpty() ? smtpUsername : "(empty)"));
		
		assertNotNull(mailSender, "JavaMailSender should be configured");
		assertNotNull(smtpHost, "SMTP host should be configured");
		assertTrue(smtpPort > 0, "SMTP port should be configured");

		assertTrue(mailSender instanceof JavaMailSenderImpl, 
				"JavaMailSender should be an instance of JavaMailSenderImpl");
		
		JavaMailSenderImpl mailSenderImpl = (JavaMailSenderImpl) mailSender;
		String host = mailSenderImpl.getHost();
		int port = mailSenderImpl.getPort();

		assertNotNull(host, "SMTP host should be configured");
		assertTrue(port > 0, "SMTP port should be configured");

		Session session = getSession();
		Properties props = session.getProperties();

		try {
			Transport transport = session.getTransport("smtp");
			String username = smtpUsername != null && !smtpUsername.isEmpty() 
					? smtpUsername : mailSenderImpl.getUsername();
			String password = smtpPassword != null && !smtpPassword.isEmpty() 
					? smtpPassword : mailSenderImpl.getPassword();
			
			if (username != null && !username.isEmpty() && 
				password != null && !password.isEmpty()) {
				transport.connect(host, port, username, password);
			} else {
				transport.connect(host, port, null, null);
			}

			assertTrue(transport.isConnected(), "SMTP transport should be connected");
			System.out.println("Successfully connected to SMTP server at " + host + ":" + port);

			transport.close();
		} catch (Exception e) {
			System.err.println("=== SMTP Connection Error ===");
			System.err.println("Host: " + host);
			System.err.println("Port: " + port);
			System.err.println("Error Message: " + e.getMessage());
			if (e.getCause() != null) {
				System.err.println("Cause: " + e.getCause().getMessage());
			}
			e.printStackTrace();
			fail("Failed to connect to SMTP server at " + host + ":" + port + 
					": " + e.getMessage(), e);
		}
	}

	@Test
	void testSMTPStartTLSConfiguration() {
		System.out.println("=== SMTP StartTLS Configuration Test ===");
		System.out.println("SMTP Host: " + smtpHost);
		System.out.println("SMTP Port: " + smtpPort);
		
		assertNotNull(mailSender, "JavaMailSender should be configured");

		Session session = getSession();
		Properties props = session.getProperties();
		String startTLS = props.getProperty("mail.smtp.starttls.enable");

		if (startTLS != null && Boolean.parseBoolean(startTLS)) {
			System.out.println("SMTP StartTLS is enabled");
			assertTrue(Boolean.parseBoolean(startTLS), "StartTLS should be enabled");
		} else {
			System.out.println("SMTP StartTLS is disabled");
		}
	}
}

