package com.bazi.app.report.wealth;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

final class WealthPlainCopy {

  private static final Map<String, String> VALUES = load();

  private WealthPlainCopy() {}

  static String get(String key) {
    String value = VALUES.get(key);
    if (value == null || value.isBlank()) {
      throw new IllegalStateException("Missing wealth plain copy: " + key);
    }
    return value;
  }

  private static Map<String, String> load() {
    InputStream input = WealthPlainCopy.class.getResourceAsStream("/report/wealth-plain-v2.yml");
    if (input == null) throw new IllegalStateException("Missing wealth-plain-v2.yml");
    Map<String, String> values = new LinkedHashMap<>();
    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(input, StandardCharsets.UTF_8))) {
      for (String line; (line = reader.readLine()) != null;) {
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
        int separator = trimmed.indexOf(':');
        if (separator <= 0) throw new IllegalStateException("Invalid wealth copy line: " + line);
        String value = trimmed.substring(separator + 1).trim();
        if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
          value = value.substring(1, value.length() - 1);
        }
        values.put(trimmed.substring(0, separator).trim(), value);
      }
    } catch (IOException error) {
      throw new IllegalStateException("Cannot load wealth plain copy", error);
    }
    return Map.copyOf(values);
  }
}
