package com.bazi.app.report.relationship;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

final class RelationshipPlainCopy {

  private static final Pattern UNRESOLVED_TOKEN = Pattern.compile("\\{[a-z_]+}");
  private static final Map<String, String> VALUES = load();

  private RelationshipPlainCopy() {}

  static String get(String key) {
    String value = VALUES.get(key);
    if (value == null || value.isBlank()) {
      throw new IllegalStateException("Missing relationship plain copy: " + key);
    }
    return value;
  }

  static String format(String key, Map<String, String> variables) {
    String value = get(key);
    for (Map.Entry<String, String> variable : variables.entrySet()) {
      value = value.replace("{" + variable.getKey() + "}", variable.getValue());
    }
    if (UNRESOLVED_TOKEN.matcher(value).find()) {
      throw new IllegalStateException("Unresolved relationship plain copy token: " + key);
    }
    return value;
  }

  private static Map<String, String> load() {
    InputStream input = RelationshipPlainCopy.class.getResourceAsStream(
        "/report-copy/relationship-plain-v1.yml");
    if (input == null) {
      throw new IllegalStateException("Missing relationship-plain-v1.yml");
    }
    Map<String, String> values = new LinkedHashMap<>();
    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(input, StandardCharsets.UTF_8))) {
      for (String line; (line = reader.readLine()) != null;) {
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
        int separator = trimmed.indexOf(':');
        if (separator <= 0) {
          throw new IllegalStateException("Invalid relationship copy line: " + line);
        }
        String key = trimmed.substring(0, separator).trim();
        String value = trimmed.substring(separator + 1).trim();
        if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
          value = value.substring(1, value.length() - 1);
        }
        if (values.putIfAbsent(key, value) != null) {
          throw new IllegalStateException("Duplicate relationship copy key: " + key);
        }
      }
    } catch (IOException error) {
      throw new IllegalStateException("Cannot load relationship plain copy", error);
    }
    return Map.copyOf(values);
  }
}
