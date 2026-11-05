package com.gymback.security;
import com.gymback.shared.persistence.model.UserAccount;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
/** Alta inicial opcional; no introduce cuentas de prueba ni usa SQL en el arranque. */
@Component
public class AdminBootstrap implements ApplicationRunner {
  private final IIdentityRepository accounts;private final EntityManager em;private final PasswordEncoder encoder;
  private final String email,password;
  public AdminBootstrap(IIdentityRepository accounts,EntityManager em,PasswordEncoder encoder,
    @Value("${BOOTSTRAP_ADMIN_EMAIL:}")String email,@Value("${BOOTSTRAP_ADMIN_PASSWORD:}")String password){
    this.accounts=accounts;this.em=em;this.encoder=encoder;this.email=email;this.password=password;
  }
  @Override @Transactional public void run(ApplicationArguments args){
    if(email.isBlank() && password.isBlank())return;
    if(email.isBlank() || password.length()<12)throw new IllegalStateException("Bootstrap admin requiere email y contraseña >=12 caracteres");
    if(accounts.findCredentialsByEmail(email).isPresent())return;
    UserAccount user=new UserAccount();user.fullName="Administrador";user.email=email.toLowerCase(Locale.ROOT);
    user.passwordHash=encoder.encode(password);user.status="active";em.persist(user);em.flush();
    accounts.insertPlatformAdmin(user.id);
  }
}
