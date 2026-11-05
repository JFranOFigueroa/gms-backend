package com.gymback.shared.services;
import com.gymback.shared.persistence.repositories.AuditActorRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.UUID;
/** Entrega el actor de cada transacción a la persistencia sin SQL en la capa de servicio. */
@Component
public class AuditContext {
  private final AuditActorRepository repository;
  public AuditContext(AuditActorRepository repository){this.repository=repository;}
  public void setActor(UUID actor){
    if(actor!=null && TransactionSynchronizationManager.isActualTransactionActive())repository.bind(actor);
  }
}
