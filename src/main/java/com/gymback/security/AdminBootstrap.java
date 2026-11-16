package com.gymback.security;

import com.gymback.modules.branches.model.Branch;
import com.gymback.shared.persistence.model.UserAccount;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/**
 * Alta inicial del administrador del sitio.
 * No usa SQL con contraseñas: la cuenta se crea aquí con BCrypt.
 * Si existe la "Sucursal Demo" del seed, el administrador también queda como owner.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {
  private final IIdentityRepository accounts;
  private final EntityManager em;
  private final PasswordEncoder encoder;
  private final String email;
  private final String password;

  public AdminBootstrap(IIdentityRepository accounts, EntityManager em, PasswordEncoder encoder,
      @Value("${BOOTSTRAP_ADMIN_EMAIL:admin@gms.local}") String email,
      @Value("${BOOTSTRAP_ADMIN_PASSWORD:}") String password) {
    this.accounts = accounts; this.em = em; this.encoder = encoder;
    this.email = email; this.password = password;
  }

  @Override @Transactional public void run(ApplicationArguments args) {
    if (password == null || password.isBlank()) return;
    if (email == null || email.isBlank() || password.length() < 12)
      throw new IllegalStateException("Bootstrap admin requiere email y contraseña >=12 caracteres");

    UserAccount user = accounts.findCredentialsByEmail(email).map(c -> em.find(UserAccount.class, c.id())).orElse(null);
    if (user == null) {
      user = new UserAccount();
      user.fullName = "Administrador";
      user.email = email.toLowerCase(Locale.ROOT);
      user.passwordHash = encoder.encode(password);
      user.status = "active";
      em.persist(user);
      em.flush();
    }
    if (!accounts.isPlatformAdmin(user.id)) accounts.insertPlatformAdmin(user.id);

    Branch demo = em.createQuery(
        "select b from Branch b where b.code='sucursal-demo' and b.deletedAt is null",
        Branch.class).setMaxResults(1).getResultStream().findFirst().orElse(null);
    if (demo != null) {
      accounts.assignStaff(user.id, demo.getId(), "owner", user.id);
      accounts.setDefaultBranchIfMissing(user.id, demo.getId());
    }
  }
}
