package com.euem.server.integration;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
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
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PostgreSQLConnectivityTest {

	@Autowired
	private DataSource dataSource;

	@Value("${spring.datasource.url}")
	private String datasourceUrl;

	@Value("${spring.datasource.username}")
	private String datasourceUsername;

	@Test
	@Order(1)
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
	@Order(2)
	void testPostgreSQLConnectionWithoutSSL() {
		System.out.println("=== PostgreSQL Non-SSL Connection Test ===");
		System.out.println("Datasource URL: " + datasourceUrl);
		System.out.println("Datasource Username: " + datasourceUsername);
		
		assertNotNull(dataSource, "DataSource should be configured");

		try (Connection connection = dataSource.getConnection()) {
			DatabaseMetaData metaData = connection.getMetaData();
			
			String url = metaData.getURL();
			// Verify that connection URL does not require SSL (database doesn't support SSL)
			assertFalse(url.contains("sslmode=require"), 
					"Connection URL should not require SSL (database doesn't support it): " + url);

			// Verify connection is working without SSL
			assertNotNull(connection, "Connection should not be null");
			assertFalse(connection.isClosed(), "Connection should be open");

			System.out.println("Connection URL: " + url);
			System.out.println("SSL: Not required (database doesn't support SSL)");

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
}

