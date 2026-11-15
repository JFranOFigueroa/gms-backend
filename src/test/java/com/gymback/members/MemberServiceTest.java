package com.gymback.members;
import com.gymback.modules.members.dto.MemberRequest;
import com.gymback.modules.members.interfaces.IMemberRepository;
import com.gymback.modules.members.services.MemberService;
import com.gymback.modules.members.model.BranchMember;
import com.gymback.modules.members.repositories.MemberPhotoRepository;
import com.gymback.modules.files.services.LocalImageStorage;
import com.gymback.security.*;
import com.gymback.shared.services.AuditService;
import com.gymback.shared.exceptions.DomainException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.*;

class MemberServiceTest {
  private MemberRequest req(UUID user) {
    return new MemberRequest(user,"M-00001",null,null,null,null,null,null,true);
  }
  @Test void onlyAssociatesExistingActiveUserWithoutCreatingIdentity() {
    var repo=mock(IMemberRepository.class);var accounts=mock(IIdentityRepository.class);
    var authorization=mock(AuthorizationService.class);var audit=mock(AuditService.class);
    UUID branch=UUID.randomUUID(),user=UUID.randomUUID(),actor=UUID.randomUUID();
    when(authorization.require(eq(branch),any(String[].class))).thenReturn(new Actor(actor,false));
    when(authorization.actor()).thenReturn(new Actor(actor,false));
    when(accounts.findCredentialsById(user)).thenReturn(Optional.of(
      new IIdentityRepository.UserCredentials(user,"Socio","socio@example.test","hash","active",null)));
    when(repo.create(any(BranchMember.class))).thenAnswer(inv->inv.getArgument(0));
    when(repo.detail(eq(branch),any())).thenReturn(Map.of("ok",true));
    var service=new MemberService(repo,audit,authorization,accounts,mock(MemberPhotoRepository.class),mock(LocalImageStorage.class));
    assertEquals(true,service.add(branch,req(user)).get("ok"));
    verify(repo).create(argThat(m->user.equals(m.userId)&&branch.equals(m.branchId)));
    verify(accounts).setDefaultBranchIfMissing(user,branch);
  }
  @Test void rejectsUnknownUsersAndDoesNotCreateMember() {
    var repo=mock(IMemberRepository.class);var auth=mock(AuthorizationService.class);
    var accounts=mock(IIdentityRepository.class);
    var service=new MemberService(repo,mock(AuditService.class),auth,accounts,mock(MemberPhotoRepository.class),mock(LocalImageStorage.class));
    UUID branch=UUID.randomUUID(),user=UUID.randomUUID();
    assertThrows(DomainException.class,()->service.add(branch,req(user)));
    verify(repo,never()).create(any());
  }
  @Test void patchCannotReplaceUserId() {
    var service=new MemberService(mock(IMemberRepository.class),mock(AuditService.class),
      mock(AuthorizationService.class),mock(IIdentityRepository.class),mock(MemberPhotoRepository.class),mock(LocalImageStorage.class));
    assertThrows(DomainException.class,()->service.change(UUID.randomUUID(),UUID.randomUUID(),req(UUID.randomUUID())));
  }
}
