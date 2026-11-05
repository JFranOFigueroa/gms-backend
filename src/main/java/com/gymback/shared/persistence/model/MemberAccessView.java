package com.gymback.shared.persistence.model;
import jakarta.persistence.*;
import java.util.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.Immutable;
@Immutable
@Entity @Table(name="v_member_access")
public class MemberAccessView  {
  @Id @Column(name="member_id") public UUID memberId;
  @Column(name="gym_id") public UUID gymId;
  @Column(name="user_id") public UUID userId;
  @Column(name="member_code") public String memberCode;
  @Column(name="next_due_date") public LocalDate nextDueDate;
  @Column(name="access_status") public String accessStatus;
}
