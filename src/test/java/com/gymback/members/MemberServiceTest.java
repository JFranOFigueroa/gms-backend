package com.gymback.members;
import com.gymback.modules.members.dto.MemberRequest;
import com.gymback.modules.members.interfaces.IMemberRepository;
import com.gymback.modules.members.services.MemberService;
import com.gymback.modules.members.model.GymMember;
import com.gymback.security.*;
import com.gymback.shared.services.AuditService;
import com.gymback.shared.exceptions.DomainException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.*;

class MemberServiceTest {
  private MemberRequest req(UUID user) {
    return new MemberRequest(user,"M-00001",null,null,null,null,null,null,null,true);
  }
  @Test void onlyAssociatesExistingActiveUserWithoutCreatingIdentity() {
    var repo=mock(IMemberRepository.class);var accounts=mock(IIdentityRepository.class);
    var authorization=mock(AuthorizationService.class);var audit=mock(AuditService.class);
    UUID gym=UUID.randomUUID(),user=UUID.randomUUID(),actor=UUID.randomUUID();
    when(authorization.require(eq(gym),any(String[].class))).thenReturn(new Actor(actor,false));
    when(authorization.actor()).thenReturn(new Actor(actor,false));
    when(accounts.findCredentialsById(user)).thenReturn(Optional.of(
      new IIdentityRepository.UserCredentials(user,"Socio","socio@example.test","hash","active",null)));
    when(repo.create(any(GymMember.class))).thenAnswer(inv->inv.getArgument(0));
    when(repo.detail(eq(gym),any())).thenReturn(Map.of("ok",true));
    var service=new MemberService(repo,audit,authorization,accounts);
    assertEquals(true,service.add(gym,req(user)).get("ok"));
    verify(repo).create(argThat(m->user.equals(m.userId)&&gym.equals(m.gymId)));
    verify(accounts).setDefaultGymIfMissing(user,gym);
  }
  @Test void rejectsUnknownUsersAndDoesNotCreateMember() {
    var repo=mock(IMemberRepository.class);var auth=mock(AuthorizationService.class);
    var accounts=mock(IIdentityRepository.class);
    var service=new MemberService(repo,mock(AuditService.class),auth,accounts);
    UUID gym=UUID.randomUUID(),user=UUID.randomUUID();
    assertThrows(DomainException.class,()->service.add(gym,req(user)));
    verify(repo,never()).create(any());
  }
  @Test void patchCannotReplaceUserId() {
    var service=new MemberService(mock(IMemberRepository.class),mock(AuditService.class),
      mock(AuthorizationService.class),mock(IIdentityRepository.class));
    assertThrows(DomainException.class,()->service.change(UUID.randomUUID(),UUID.randomUUID(),req(UUID.randomUUID())));
  }
}
