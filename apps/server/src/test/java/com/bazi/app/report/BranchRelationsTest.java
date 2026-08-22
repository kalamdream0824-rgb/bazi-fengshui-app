package com.bazi.app.report;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class BranchRelationsTest {

  @ParameterizedTest
  @CsvSource({
      "子,午,CLASH", "丑,未,CLASH", "寅,申,CLASH", "卯,酉,CLASH", "辰,戌,CLASH", "巳,亥,CLASH",
      "子,丑,HARMONY", "寅,亥,HARMONY", "卯,戌,HARMONY", "辰,酉,HARMONY", "巳,申,HARMONY", "午,未,HARMONY",
      "子,未,HARM", "丑,午,HARM", "寅,巳,HARM", "卯,辰,HARM", "申,亥,HARM", "酉,戌,HARM"
  })
  void detectsSymmetricRelations(String left, String right, BranchRelation expected) {
    assertTrue(BranchRelations.between(left, right).contains(expected));
    assertTrue(BranchRelations.between(right, left).contains(expected));
  }

  @ParameterizedTest
  @CsvSource({
      "子,酉", "丑,辰", "寅,亥", "卯,午", "巳,申", "未,戌"
  })
  void detectsSixBreaksSymmetrically(String left, String right) {
    assertTrue(BranchRelations.between(left, right).contains(BranchRelation.BREAK));
    assertTrue(BranchRelations.between(right, left).contains(BranchRelation.BREAK));
  }

  @ParameterizedTest
  @CsvSource({
      "子,卯", "寅,巳", "巳,申", "申,寅", "丑,戌", "戌,未", "未,丑"
  })
  void detectsDocumentedPunishmentPairsSymmetrically(String left, String right) {
    assertTrue(BranchRelations.between(left, right).contains(BranchRelation.PUNISHMENT));
    assertTrue(BranchRelations.between(right, left).contains(BranchRelation.PUNISHMENT));
  }

  @ParameterizedTest
  @CsvSource({"辰", "午", "酉", "亥"})
  void limitsSelfPunishmentToFourDocumentedBranches(String branch) {
    assertTrue(BranchRelations.between(branch, branch).contains(BranchRelation.PUNISHMENT));
  }

  @Test
  void doesNotTreatOtherIdenticalBranchesAsSelfPunishment() {
    for (String branch : new String[] {"子", "丑", "寅", "卯", "巳", "未", "申", "戌"}) {
      assertFalse(BranchRelations.between(branch, branch).contains(BranchRelation.PUNISHMENT));
    }
  }

  @Test
  void rejectsUnknownBranches() {
    assertThrows(IllegalArgumentException.class, () -> BranchRelations.between("甲", "子"));
  }
}
