package com.gymback.settings;

import com.gymback.modules.settings.dto.ModulePatch;
import com.gymback.modules.settings.interfaces.ISettingsRepository;
import com.gymback.modules.settings.services.SettingsService;
import com.gymback.modules.site.model.SiteSettings;
import com.gymback.modules.site.repositories.SiteSettingsRepository;
import com.gymback.security.Actor;
import com.gymback.security.AuthorizationService;
import com.gymback.shared.exceptions.DomainException;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SettingsServiceTest {
  @Test void usesGlobalModulesWhenSiteIsGlobal() {
    var repo = mock(ISettingsRepository.class);
    var siteRepo = mock(SiteSettingsRepository.class);
    var auth = mock(AuthorizationService.class);
    UUID branch = UUID.randomUUID();
    SiteSettings site = new SiteSettings();
    site.setModulesGlobal(true);
    Map<String, Object> modules = new LinkedHashMap<>();
    modules.put("calendar", true);
    modules.put("routines", false);
    modules.put("checkin", true);
    modules.put("pos", false);
    site.setGlobalModules(modules);
    when(siteRepo.find()).thenReturn(Optional.of(site));
    when(auth.require(eq(branch), any(String[].class))).thenReturn(new Actor(UUID.randomUUID(), true));

    var service = new SettingsService(repo, siteRepo, auth);
    List<Map<String, Object>> result = service.modules(branch);

    assertEquals(4, result.size());
    assertEquals(true, result.stream().filter(m -> "calendar".equals(m.get("module_key"))).findFirst().orElseThrow().get("enabled"));
    assertEquals(false, result.stream().filter(m -> "pos".equals(m.get("module_key"))).findFirst().orElseThrow().get("enabled"));
    verify(repo, never()).modules(any());
  }

  @Test void refusesBranchToggleWhenSiteIsGlobal() {
    var repo = mock(ISettingsRepository.class);
    var siteRepo = mock(SiteSettingsRepository.class);
    var auth = mock(AuthorizationService.class);
    UUID branch = UUID.randomUUID();
    SiteSettings site = new SiteSettings();
    site.setModulesGlobal(true);
    when(siteRepo.find()).thenReturn(Optional.of(site));
    when(auth.platformAdmin()).thenReturn(new Actor(UUID.randomUUID(), true));

    var service = new SettingsService(repo, siteRepo, auth);

    assertThrows(DomainException.class, () -> service.toggle(branch, "calendar", new ModulePatch(true)));
    verify(repo, never()).toggle(any(), anyString(), anyBoolean(), any());
  }
}
