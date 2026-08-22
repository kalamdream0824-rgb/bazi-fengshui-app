package com.bazi.app.report.rules;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

final class AnnualRuleCopy {

  private static final String RESOURCE = "/report/annual-copy.yml";
  private final Map<String, String> values;

  AnnualRuleCopy() {
    values = load();
  }

  String get(String key) {
    String value = values.get(key);
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("missing annual report copy: " + key);
    }
    return value;
  }

  private Map<String, String> load() {
    InputStream stream = AnnualRuleCopy.class.getResourceAsStream(RESOURCE);
    if (stream == null) throw new IllegalStateException("missing resource: " + RESOURCE);
    Map<String, String> loaded = new LinkedHashMap<>();
    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(stream, StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
        int separator = trimmed.indexOf(':');
        if (separator <= 0) throw new IllegalStateException("invalid annual copy line: " + line);
        String key = trimmed.substring(0, separator).trim();
        String value = trimmed.substring(separator + 1).trim();
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
          value = value.substring(1, value.length() - 1);
        }
        loaded.put(key, value);
      }
    } catch (IOException exception) {
      throw new IllegalStateException("cannot load annual copy", exception);
    }
    return Map.copyOf(loaded);
  }
}
