package com.gymback;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.orm.jpa.SharedEntityManagerCreator;

/** EntityManager transaccional para los repositorios propios en el arranque local. */
@Configuration(proxyBeanMethods = false)
@Profile("local")
public class LocalPersistenceConfig {
  @Bean
  @ConditionalOnMissingBean(EntityManager.class)
  EntityManager entityManager(EntityManagerFactory entityManagerFactory) {
    return SharedEntityManagerCreator.createSharedEntityManager(entityManagerFactory);
  }

  // IdempotencyService conserva Jackson 2; el JSON HTTP de Spring Boot usa Jackson 3.
  @Bean
  @ConditionalOnMissingBean(com.fasterxml.jackson.databind.ObjectMapper.class)
  com.fasterxml.jackson.databind.ObjectMapper idempotencyObjectMapper() {
    return com.fasterxml.jackson.databind.json.JsonMapper.builder().findAndAddModules().build();
  }
}
