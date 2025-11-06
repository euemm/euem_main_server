package com.euem.server.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class PostgreSQLConnectivityTest {

	@Autowired
	private DataSource dataSource;

	@Test
	void testPostgreSQLConnection() {
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
			fail("Failed to connect to PostgreSQL: " + e.getMessage(), e);
		}
	}

	@Test
	void testPostgreSQLConnectionWithSSL() {
		assertNotNull(dataSource, "DataSource should be configured");

		try (Connection connection = dataSource.getConnection()) {
			DatabaseMetaData metaData = connection.getMetaData();
			
			String url = metaData.getURL();
			assertTrue(url.contains("sslmode=require"), 
					"Connection URL should require SSL: " + url);

			System.out.println("Connection URL: " + url);
			System.out.println("SSL mode: require (verified)");

		} catch (SQLException e) {
			fail("Failed to verify SSL connection to PostgreSQL: " + e.getMessage(), e);
		}
	}
}

