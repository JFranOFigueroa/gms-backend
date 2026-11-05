package com.gymback.security;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
@Component
public class SchemaSafety {
  private final String schema;
  public SchemaSafety(@Value("${schema_db:${SCHEMA_DB:test}}")String schema){this.schema=schema;}
  @PostConstruct public void validate(){
    if(!schema.matches("[a-z_][a-z0-9_]{0,62}"))throw new IllegalStateException("SCHEMA_DB inválido");
  }
}
