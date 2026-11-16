package com.gymback.modules.site.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.OffsetDateTime;
import java.util.*;

/** Configuración única de la marca/sitio. No pertenece a ninguna sucursal. */
@Entity
@Table(name = "site_settings")
public class SiteSettings {
  public static final UUID SINGLETON_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
  public static final List<String> MODULE_KEYS = List.of("calendar", "routines", "checkin", "pos");

  public static Map<String, Object> defaultGlobalModules() {
    Map<String, Object> modules = new LinkedHashMap<>();
    modules.put("calendar", false);
    modules.put("routines", true);
    modules.put("checkin", true);
    modules.put("pos", true);
    return modules;
  }

  @Id @Column(name = "id", nullable = false)
  private UUID id = SINGLETON_ID;
  @Column(name = "site_name", nullable = false)
  private String siteName = "GymBack";
  @Column(name = "short_name")
  private String shortName;
  @Column(name = "logo_asset_id")
  private UUID logoAssetId;
  @Column(name = "contact_email", columnDefinition = "citext")
  private String contactEmail;
  @Column(name = "contact_phone")
  private String contactPhone;
  @Column(name = "address")
  private String address;
  @Column(name = "timezone")
  private String timezone = "America/Mexico_City";
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "currency", length = 3, columnDefinition = "char(3)")
  private String currency = "MXN";
  @Column(name = "theme_preset")
  private String themePreset = "crossfit-dark";
  @JdbcTypeCode(SqlTypes.JSON) @Column(name = "theme_overrides")
  private Map<String, Object> themeOverrides = new LinkedHashMap<>();
  @Column(name = "modules_global", nullable = false)
  private boolean modulesGlobal = false;
  @JdbcTypeCode(SqlTypes.JSON) @Column(name = "global_modules", nullable = false)
  private Map<String, Object> globalModules = defaultGlobalModules();
  @Column(name = "created_at") private OffsetDateTime createdAt = OffsetDateTime.now();
  @Column(name = "updated_at") private OffsetDateTime updatedAt = OffsetDateTime.now();
  @Column(name = "created_by") private UUID createdBy;
  @Column(name = "updated_by") private UUID updatedBy;
  @Column(name = "deleted_at") private OffsetDateTime deletedAt;
  @Column(name = "deleted_by") private UUID deletedBy;

  public UUID getId() { return id; }
  public String getSiteName() { return siteName; }
  public void setSiteName(String value) { this.siteName = value; }
  public String getShortName() { return shortName; }
  public void setShortName(String value) { this.shortName = value; }
  public UUID getLogoAssetId() { return logoAssetId; }
  public void setLogoAssetId(UUID value) { this.logoAssetId = value; }
  public String getContactEmail() { return contactEmail; }
  public void setContactEmail(String value) { this.contactEmail = value; }
  public String getContactPhone() { return contactPhone; }
  public void setContactPhone(String value) { this.contactPhone = value; }
  public String getAddress() { return address; }
  public void setAddress(String value) { this.address = value; }
  public String getTimezone() { return timezone; }
  public void setTimezone(String value) { this.timezone = value; }
  public String getCurrency() { return currency; }
  public void setCurrency(String value) { this.currency = value; }
  public String getThemePreset() { return themePreset; }
  public void setThemePreset(String value) { this.themePreset = value; }
  public Map<String, Object> getThemeOverrides() { return themeOverrides; }
  public void setThemeOverrides(Map<String, Object> value) { this.themeOverrides = value; }
  public boolean isModulesGlobal() { return modulesGlobal; }
  public void setModulesGlobal(boolean value) { this.modulesGlobal = value; }
  public Map<String, Object> getGlobalModules() { return globalModules; }
  public void setGlobalModules(Map<String, Object> value) { this.globalModules = value; }
  public OffsetDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(OffsetDateTime value) { this.createdAt = value; }
  public OffsetDateTime getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(OffsetDateTime value) { this.updatedAt = value; }
  public UUID getCreatedBy() { return createdBy; }
  public void setCreatedBy(UUID value) { this.createdBy = value; }
  public UUID getUpdatedBy() { return updatedBy; }
  public void setUpdatedBy(UUID value) { this.updatedBy = value; }
  public OffsetDateTime getDeletedAt() { return deletedAt; }
  public void setDeletedAt(OffsetDateTime value) { this.deletedAt = value; }
  public UUID getDeletedBy() { return deletedBy; }
  public void setDeletedBy(UUID value) { this.deletedBy = value; }
}
