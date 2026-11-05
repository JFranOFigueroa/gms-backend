package com.gymback.calendar;
import com.gymback.modules.calendar.model.ClassCategories;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ClassCategoriesTest {
  @Test void functionalIsAcceptedAndCountsAgainstRegularDailyLimit() {
    assertTrue(ClassCategories.allowed("functional"));
    assertTrue(ClassCategories.countsAsRegular("functional"));
    assertTrue(ClassCategories.allowed("normal"));
    assertFalse(ClassCategories.countsAsRegular("event"));
    assertFalse(ClassCategories.allowed("unknown"));
  }
}
