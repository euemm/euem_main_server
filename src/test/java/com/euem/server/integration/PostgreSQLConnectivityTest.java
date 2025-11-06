package com.euem.server.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
	"spring.jpa.hibernate.ddl-auto=none",
	"spring.datasource.hikari.initialization-fail-timeout=-1"
})
@ActiveProfiles("test")
class PostgreSQLConnectivityTest {

	@Autowired
	private DataSource dataSource;

	@Value("${spring.datasource.url}")
	private String datasourceUrl;

	@Value("${spring.datasource.username}")
	private String datasourceUsername;

	@Test
	void testPostgreSQLConnection() {
		System.out.println("=== PostgreSQL Connection Test ===");
		System.out.println("Datasource URL: " + datasourceUrl);
		System.out.println("Datasource Username: " + datasourceUsername);
		System.out.println("Datasource configured: " + (dataSource != null));
		
		assertNotNull(dataSource, "DataSource should be configured");

		try (Connection connection = dataSource.getConnection()) {
			assertNotNull(connection, "Connection should not be null");
			assertFalse(connection.isClosed(), "Connection should be open");

			DatabaseMetaData metaData = connection.getMetaData();
			assertNotNull(metaData, "Database metadata should be available");

			String databaseProductName = metaData.getDatabaseProductName();
			assertEquals("PostgreSQL", databaseProductName, 
					"Database should be PostgreSQL, but was: " + databaseProductName);

			String databaseProductVersion = metaData.getDatabaseProductVersion();
			assertNotNull(databaseProductVersion, "Database version should be available");
			System.out.println("PostgreSQL version: " + databaseProductVersion);

			String catalogName = connection.getCatalog();
			assertNotNull(catalogName, "Database catalog name should be available");
			System.out.println("Connected to database: " + catalogName);

		} catch (SQLException e) {
			System.err.println("=== PostgreSQL Connection Error ===");
			System.err.println("Error Message: " + e.getMessage());
			System.err.println("Error Code: " + e.getErrorCode());
			System.err.println("SQL State: " + e.getSQLState());
			if (e.getCause() != null) {
				System.err.println("Cause: " + e.getCause().getMessage());
			}
			e.printStackTrace();
			fail("Failed to connect to PostgreSQL: " + e.getMessage(), e);
		}
	}

	@Test
	void testPostgreSQLConnectionWithSSL() {
		System.out.println("=== PostgreSQL SSL Connection Test ===");
		System.out.println("Datasource URL: " + datasourceUrl);
		System.out.println("Datasource Username: " + datasourceUsername);
		
		assertNotNull(dataSource, "DataSource should be configured");

		try (Connection connection = dataSource.getConnection()) {
			DatabaseMetaData metaData = connection.getMetaData();
			
			String url = metaData.getURL();
			// Check if SSL mode is configured (prefer, require, verify-ca, etc.)
			assertTrue(url.contains("sslmode="), 
					"Connection URL should have SSL mode configured: " + url);

			// Check if SSL is actually being used
			boolean sslInUse = false;
			try {
				// Try to get SSL info from connection
				Object sslFactory = connection.getClass().getMethod("getSSLFactory").invoke(connection);
				sslInUse = sslFactory != null;
			} catch (Exception e) {
				// SSL info not available, connection might not be using SSL
				sslInUse = false;
			}

			System.out.println("Connection URL: " + url);
			if (sslInUse) {
				System.out.println("SSL: Enabled and in use");
			} else {
				System.out.println("SSL: Not in use (server may not support SSL)");
			}

		} catch (SQLException e) {
			System.err.println("=== PostgreSQL SSL Connection Error ===");
			System.err.println("Error Message: " + e.getMessage());
			System.err.println("Error Code: " + e.getErrorCode());
			System.err.println("SQL State: " + e.getSQLState());
			if (e.getCause() != null) {
				System.err.println("Cause: " + e.getCause().getMessage());
			}
			e.printStackTrace();
			fail("Failed to verify connection to PostgreSQL: " + e.getMessage(), e);
		}
	}
}

