package com.gymback.modules.calendar.model;
import java.util.Set;
/** Catálogo canónico del backend y de los CHECK de PostgreSQL (V2). */
public final class ClassCategories {
  private ClassCategories() {}
  private static final Set<String> ALLOWED=Set.of("normal","functional","special","overlay_special","event");
  public static boolean allowed(String value){return value!=null && ALLOWED.contains(value);}
  /** Functional es clase regular para la regla de máximo una reserva normal al día. */
  public static boolean countsAsRegular(String value){return "normal".equals(value) || "functional".equals(value);}
}
