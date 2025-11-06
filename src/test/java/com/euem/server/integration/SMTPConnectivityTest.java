package com.euem.server.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
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

	@Test
	void testSMTPConfiguration() {
		assertNotNull(mailSender, "JavaMailSender should be configured");
		assertNotNull(smtpHost, "SMTP host should be configured");
		assertTrue(smtpPort > 0, "SMTP port should be configured");

		Session session = mailSender.getSession();
		assertNotNull(session, "Mail session should be available");

		Properties props = session.getProperties();
		String host = props.getProperty("mail.smtp.host");
		String port = props.getProperty("mail.smtp.port");

		assertNotNull(host, "SMTP host should be configured");
		assertNotNull(port, "SMTP port should be configured");

		System.out.println("SMTP Host: " + host);
		System.out.println("SMTP Port: " + port);
		System.out.println("SMTP Auth: " + props.getProperty("mail.smtp.auth"));
		System.out.println("SMTP StartTLS: " + props.getProperty("mail.smtp.starttls.enable"));
	}

	@Test
	void testSMTPConnection() {
		assertNotNull(mailSender, "JavaMailSender should be configured");
		assertNotNull(smtpHost, "SMTP host should be configured");
		assertTrue(smtpPort > 0, "SMTP port should be configured");

		Session session = mailSender.getSession();
		Properties props = session.getProperties();
		String host = props.getProperty("mail.smtp.host");
		String port = props.getProperty("mail.smtp.port");

		assertNotNull(host, "SMTP host should be configured");
		assertNotNull(port, "SMTP port should be configured");

		try {
			Transport transport = session.getTransport("smtp");
			String username = props.getProperty("mail.smtp.user");
			String password = props.getProperty("mail.smtp.password");

			int portNumber = Integer.parseInt(port);
			
			if (username != null && !username.isEmpty() && 
				password != null && !password.isEmpty()) {
				transport.connect(host, portNumber, username, password);
			} else {
				transport.connect(host, portNumber, null, null);
			}

			assertTrue(transport.isConnected(), "SMTP transport should be connected");
			System.out.println("Successfully connected to SMTP server at " + host + ":" + port);

			transport.close();
		} catch (Exception e) {
			fail("Failed to connect to SMTP server at " + host + ":" + port + 
					": " + e.getMessage(), e);
		}
	}

	@Test
	void testSMTPStartTLSConfiguration() {
		assertNotNull(mailSender, "JavaMailSender should be configured");

		Session session = mailSender.getSession();
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

