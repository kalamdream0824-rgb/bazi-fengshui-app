package com.bazi.app;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ClassPathResource;

class DevDatasourcePersistenceTest {

  @Test
  void developmentAccountsSurviveBackendRestarts() throws IOException {
    String url = developmentUrl();

    assertTrue(url.startsWith("jdbc:h2:file:"),
        () -> "development datasource must be file-backed, but was: " + url);
    assertFalse(url.contains(":mem:"),
        () -> "in-memory H2 erases accounts whenever the backend restarts: " + url);
    assertFalse(url.contains("AUTO_SERVER=TRUE") && url.contains("DB_CLOSE_ON_EXIT=FALSE"),
        () -> "H2 rejects AUTO_SERVER=TRUE together with DB_CLOSE_ON_EXIT=FALSE: " + url);
  }

  @Test
  void configuredFileDatabaseCanReopenWithoutLosingRows(@TempDir Path tempDir)
      throws IOException, SQLException {
    String url = developmentUrl().replace(
        "./data/bazi-dev",
        tempDir.resolve("restart-check").toAbsolutePath().toString());

    try (var connection = DriverManager.getConnection(url, "sa", "");
         var statement = connection.createStatement()) {
      statement.execute("CREATE TABLE persistence_probe (id INT PRIMARY KEY)");
      statement.executeUpdate("INSERT INTO persistence_probe (id) VALUES (1)");
    }

    try (var connection = DriverManager.getConnection(url, "sa", "");
         var statement = connection.createStatement();
         var result = statement.executeQuery("SELECT COUNT(*) FROM persistence_probe")) {
      assertTrue(result.next());
      assertEquals(1, result.getInt(1));
    }
  }

  private String developmentUrl() throws IOException {
    var sources = new YamlPropertySourceLoader()
        .load("development", new ClassPathResource("application-dev.yml"));
    return String.valueOf(sources.get(0).getProperty("spring.datasource.url"));
  }
}
