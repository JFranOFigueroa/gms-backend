package com.gymback.staff;
import com.gymback.modules.staff.dto.StaffRequest;
import com.gymback.modules.staff.interfaces.IStaffRepository;
import com.gymback.modules.staff.services.StaffService;
import com.gymback.security.*;
import com.gymback.shared.exceptions.DomainException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.*;

class StaffServiceTest {
  @Test void createsRelationOnlyForExistingActiveUser() {
    var accounts=mock(IIdentityRepository.class);var auth=mock(AuthorizationService.class);
    var staff=mock(IStaffRepository.class);
    UUID user=UUID.randomUUID(),gym=UUID.randomUUID(),actor=UUID.randomUUID();
    when(auth.require(eq(gym),any(String[].class))).thenReturn(new Actor(actor,true));
    when(auth.actor()).thenReturn(new Actor(actor,true));
    when(accounts.findCredentialsById(user)).thenReturn(Optional.of(new IIdentityRepository.UserCredentials(
      user,"Entrenador","coach@example.test","hash","active",null)));
    when(staff.get(gym,user)).thenReturn(Map.of("id",user,"roles",List.of("trainer")));
    var service=new StaffService(auth,accounts,staff);
    assertEquals(user,service.add(gym,new StaffRequest(user,"trainer")).get("id"));
    verify(accounts).assignStaff(user,gym,"trainer",actor);
  }
  @Test void refusesAmbiguousMultiRoleChange() {
    var accounts=mock(IIdentityRepository.class);var auth=mock(AuthorizationService.class);
    UUID user=UUID.randomUUID(),gym=UUID.randomUUID();
    when(accounts.activeStaffRoles(user,gym)).thenReturn(Set.of("owner","trainer"));
    var service=new StaffService(auth,accounts,mock(IStaffRepository.class));
    assertThrows(DomainException.class,()->service.change(gym,user,new StaffRequest(null,"trainer")));
    verify(accounts,never()).deactivateStaff(any(),any());
  }
}
