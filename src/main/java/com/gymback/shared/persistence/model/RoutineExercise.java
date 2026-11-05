package com.gymback.shared.persistence.model;
import jakarta.persistence.*;
import java.util.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
@org.hibernate.annotations.SQLRestriction("deleted_at IS NULL")
@Entity @Table(name="routine_exercises")
public class RoutineExercise extends AuditedRecord {
  @Id @Column(name="id") public UUID id = UUID.randomUUID();
  @Column(name="gym_id") public UUID gymId;
  @Column(name="routine_day_id") public UUID routineDayId;
  @Column(name="exercise_order") public Integer exerciseOrder;
  @Column(name="exercise_name") public String exerciseName;
  @Column(name="sets_label") public String setsLabel;
  @Column(name="reps_label") public String repsLabel;
  @Column(name="rest_label") public String restLabel;
  @Column(name="notes") public String notes;
}
