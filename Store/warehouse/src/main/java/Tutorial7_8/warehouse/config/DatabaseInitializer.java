package Tutorial7_8.warehouse.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

@Slf4j
public class DatabaseInitializer implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    private final String databaseName;

    // Default constructor for warehouse1
    public DatabaseInitializer() {
        this.databaseName = null; // Will be detected from properties
    }

    // Constructor with explicit database name
    public DatabaseInitializer(String databaseName) {
        this.databaseName = databaseName;
    }

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        ConfigurableEnvironment environment = event.getEnvironment();
        
        try {
            String dbName;
            String datasourceUrl;
            String username = environment.getProperty("spring.datasource.username", "admin");
            String password = environment.getProperty("spring.datasource.password", "admin");

            // If database name was provided via constructor, use it
            if (databaseName != null && !databaseName.isEmpty()) {
                dbName = databaseName;
            } else {
                // Try to read from environment (may not be loaded yet)
                datasourceUrl = environment.getProperty("spring.datasource.url");
                
                if (datasourceUrl != null && !datasourceUrl.isEmpty()) {
                    dbName = extractDatabaseName(datasourceUrl);
                } else {
                    // Fallback: try to read from properties files directly
                    dbName = detectDatabaseNameFromProperties();
                }
            }

            // Get base URL (host:port) - try from environment or use default
            String baseUrl;
            datasourceUrl = environment.getProperty("spring.datasource.url");
            if (datasourceUrl != null && !datasourceUrl.isEmpty()) {
                baseUrl = extractBaseUrl(datasourceUrl);
            } else {
                // Read from properties file or use default
                baseUrl = detectBaseUrlFromProperties();
                if (baseUrl == null || baseUrl.isEmpty()) {
                    baseUrl = "jdbc:postgresql://localhost:5433";
                }
            }

            log.info("Checking if database '{}' exists...", dbName);

            // Connect to default 'postgres' database to check/create target database
            String defaultDbUrl = baseUrl + "/postgres";
            
            try (Connection conn = DriverManager.getConnection(defaultDbUrl, username, password)) {
                // Ensure auto-commit is enabled (CREATE DATABASE cannot be in transaction)
                conn.setAutoCommit(true);
                
                try (Statement stmt = conn.createStatement()) {
                    // Check if database exists
                    // Note: Using parameterized query would be ideal, but pg_database doesn't support it for datname
                    // Since dbName comes from config file, it should be safe
                    ResultSet rs = stmt.executeQuery(
                            "SELECT 1 FROM pg_database WHERE datname = '" + dbName.replace("'", "''") + "'"
                    );

                    if (!rs.next()) {
                        // Database doesn't exist, create it
                        log.info("Database '{}' does not exist. Creating...", dbName);
                        // CREATE DATABASE cannot be executed within a transaction block
                        // Escaping database name to prevent SQL injection
                        String escapedDbName = dbName.replace("\"", "\"\"");
                        stmt.executeUpdate("CREATE DATABASE \"" + escapedDbName + "\"");
                        log.info("Database '{}' created successfully", dbName);
                    } else {
                        log.info("Database '{}' already exists", dbName);
                    }
                }
            }

        } catch (Exception e) {
            // Log but don't fail startup - database might already exist or connection might fail
            log.warn("Could not verify/create database: {}. Continuing startup...", e.getMessage());
            log.debug("Database initialization error", e);
        }
    }

    private String extractDatabaseName(String url) {
        // Extract dbname from jdbc:postgresql://host:port/dbname
        int lastSlash = url.lastIndexOf('/');
        int questionMark = url.indexOf('?', lastSlash);
        if (questionMark > 0) {
            return url.substring(lastSlash + 1, questionMark);
        }
        return url.substring(lastSlash + 1);
    }

    private String extractBaseUrl(String url) {
        // Extract base URL (protocol://host:port) from jdbc:postgresql://host:port/dbname
        int lastSlash = url.lastIndexOf('/');
        return url.substring(0, lastSlash);
    }

    private String detectDatabaseNameFromProperties() {
        // Try to read from warehouse2 first (less likely to be default)
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("application-warehouse2.properties")) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                String url = props.getProperty("spring.datasource.url");
                if (url != null) {
                    String dbName = extractDatabaseName(url);
                    if (dbName.contains("warehouse2")) {
                        return "warehouse2";
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Could not read warehouse2 properties: {}", e.getMessage());
        }
        
        // Check warehouse1
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("application-warehouse1.properties")) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                String url = props.getProperty("spring.datasource.url");
                if (url != null) {
                    String dbName = extractDatabaseName(url);
                    if (dbName.contains("warehouse1")) {
                        return "warehouse1";
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Could not read warehouse1 properties: {}", e.getMessage());
        }
        
        // Default fallback
        log.warn("Could not determine database name from properties, defaulting to warehouse1");
        return "warehouse1";
    }

    private String detectBaseUrlFromProperties() {
        // Try to read from either properties file to get base URL
        String[] files = {"application-warehouse2.properties", "application-warehouse1.properties"};
        for (String fileName : files) {
            try (InputStream is = getClass().getClassLoader().getResourceAsStream(fileName)) {
                if (is != null) {
                    Properties props = new Properties();
                    props.load(is);
                    String url = props.getProperty("spring.datasource.url");
                    if (url != null && !url.isEmpty()) {
                        return extractBaseUrl(url);
                    }
                }
            } catch (Exception e) {
                // Continue to next file
            }
        }
        return "jdbc:postgresql://localhost:5433";
    }
}

