package com.gymback.modules.gyms.model;
import com.gymback.shared.domain.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
@org.hibernate.annotations.SQLRestriction("deleted_at IS NULL")
@Entity
@Table(name="gym")
public class Gym implements BaseEntity {
  @Id @Column(name="id",nullable=false) private UUID id = UUID.randomUUID();

    @Column(name="name")
  private String name;
  @Column(name="short_name")
  private String shortName;
  @Column(name = "slug", nullable = false, columnDefinition = "citext")
  private String slug;
  @Column(name="address")
  private String address;
  @Column(name="phone")
  private String phone;
  @Column(name="logo_url")
  private String logoUrl;
  @Column(name="timezone")
  private String timezone = "America/Mexico_City";
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "currency", length = 3, columnDefinition = "char(3)")
  private String currency = "MXN";
  @Column(name="status")
  private String status = "active";
  @Column(name="created_by")
  private UUID createdBy;
  @Column(name="created_at")
  private OffsetDateTime createdAt = OffsetDateTime.now();
  @Column(name="updated_at")
  private OffsetDateTime updatedAt = OffsetDateTime.now();
  @Override public UUID getId() {return id;}
  public void setId(UUID id){this.id=id;}

    public String getName() {return name;}
  public void setName(String value) {this.name=value;}
  public String getShortName() {return shortName;}
  public void setShortName(String value) {this.shortName=value;}
  public String getSlug() {return slug;}
  public void setSlug(String value) {this.slug=value;}
  public String getAddress() {return address;}
  public void setAddress(String value) {this.address=value;}
  public String getPhone() {return phone;}
  public void setPhone(String value) {this.phone=value;}
  public String getLogoUrl() {return logoUrl;}
  public void setLogoUrl(String value) {this.logoUrl=value;}
  public String getTimezone() {return timezone;}
  public void setTimezone(String value) {this.timezone=value;}
  public String getCurrency() {return currency;}
  public void setCurrency(String value) {this.currency=value;}
  public String getStatus() {return status;}
  public void setStatus(String value) {this.status=value;}
  public UUID getCreatedBy() {return createdBy;}
  public void setCreatedBy(UUID value) {this.createdBy=value;}
  public OffsetDateTime getCreatedAt() {return createdAt;}
  public void setCreatedAt(OffsetDateTime value) {this.createdAt=value;}
  public OffsetDateTime getUpdatedAt() {return updatedAt;}
  public void setUpdatedAt(OffsetDateTime value) {this.updatedAt=value;}

  @Column(name="updated_by") private UUID updatedBy;
  @Column(name="deleted_at") private OffsetDateTime deletedAt;
  @Column(name="deleted_by") private UUID deletedBy;
  public UUID getUpdatedBy() {return updatedBy;}
  public void setUpdatedBy(UUID value) {this.updatedBy=value;}
  public OffsetDateTime getDeletedAt() {return deletedAt;}
  public void setDeletedAt(OffsetDateTime value) {this.deletedAt=value;}
  public UUID getDeletedBy() {return deletedBy;}
  public void setDeletedBy(UUID value) {this.deletedBy=value;}
}
