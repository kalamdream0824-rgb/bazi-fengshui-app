package com.bazi.app.report.relationship;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.bazi.app.dto.RelationshipContextRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class RelationshipStatusTest {

  private static Validator validator;

  @BeforeAll
  static void createValidator() {
    validator = Validation.buildDefaultValidatorFactory().getValidator();
  }

  @ParameterizedTest
  @CsvSource({
      "single,单身或尚未确定关系",
      "dating,已确认交往关系",
      "married,已婚或长期共同生活"
  })
  void parsesSupportedStatus(String code, String label) {
    RelationshipStatus status = RelationshipStatus.fromCode(code);

    assertEquals(code, status.code());
    assertEquals(label, status.label());
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "ambiguous", "SINGLE"})
  void rejectsUnsupportedStatus(String code) {
    assertThrows(IllegalArgumentException.class, () -> RelationshipStatus.fromCode(code));
  }

  @Test
  void requestConvertsTheSelectedStatusToDomain() {
    RelationshipContextRequest request = new RelationshipContextRequest("dating");

    assertEquals(RelationshipStatus.DATING, request.toDomain());
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "\t"})
  void requestValidationRejectsBlankStatus(String code) {
    RelationshipContextRequest request = new RelationshipContextRequest(code);

    assertEquals(1, validator.validate(request).size());
  }
}
