package com.gymback.shared.domain;
import java.util.UUID;
/** Contrato mínimo de entidades; getBranchId es null solamente para agregados globales. */
public interface BaseEntity {
  UUID getId();
  default UUID getBranchId() { return null; }
}
