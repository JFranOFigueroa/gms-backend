package com.gymback.shared.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.SharedEntityManagerCreator;

/**
 * Expone un {@link EntityManager} transaccional compartido para los repositorios
 * que lo reciben por constructor. Es el equivalente a {@code @PersistenceContext}:
 * el proxy delega en el EntityManager de la transacción actual.
 */
@Configuration
public class JpaConfig {

  @Bean
  public EntityManager entityManager(EntityManagerFactory entityManagerFactory) {
    return SharedEntityManagerCreator.createSharedEntityManager(entityManagerFactory);
  }
}
