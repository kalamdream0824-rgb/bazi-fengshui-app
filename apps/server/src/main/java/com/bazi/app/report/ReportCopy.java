package com.bazi.app.report;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

final class ReportCopy {

  private static final Map<String, String> VALUES = load();

  private ReportCopy() {}

  static String get(String key) {
    String value = VALUES.get(key);
    if (value == null || value.isBlank()) {
      throw new IllegalStateException("Missing report copy: " + key);
    }
    return value;
  }

  private static Map<String, String> load() {
    InputStream input = ReportCopy.class.getResourceAsStream("/report/report-copy.yml");
    if (input == null) throw new IllegalStateException("Missing report-copy.yml");
    Map<String, String> values = new LinkedHashMap<>();
    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(input, StandardCharsets.UTF_8))) {
      for (String line; (line = reader.readLine()) != null;) {
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
        int separator = trimmed.indexOf(':');
        if (separator <= 0) throw new IllegalStateException("Invalid report copy line: " + line);
        String value = trimmed.substring(separator + 1).trim();
        if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
          value = value.substring(1, value.length() - 1);
        }
        values.put(trimmed.substring(0, separator).trim(), value);
      }
    } catch (IOException error) {
      throw new IllegalStateException("Cannot load report copy", error);
    }
    return Map.copyOf(values);
  }
}
