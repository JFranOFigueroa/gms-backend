package com.gymback.shared.domain;
import java.util.UUID;
/** Contrato mínimo de entidades; getGymId es null solamente para agregados globales. */
public interface BaseEntity {
  UUID getId();
  default UUID getGymId() { return null; }
}
