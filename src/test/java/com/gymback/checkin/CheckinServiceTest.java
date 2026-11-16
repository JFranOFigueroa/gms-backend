package com.gymback.checkin;
import com.gymback.modules.checkin.interfaces.*;
import com.gymback.modules.checkin.services.*;
import com.gymback.modules.checkin.dto.CheckinRequest;
import com.gymback.security.*;
import com.gymback.shared.exceptions.ConflictException;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
class CheckinServiceTest {
 private final ICheckinRepository repo=mock(ICheckinRepository.class);
 private final AuthorizationService auth=mock(AuthorizationService.class);
 private final MemberAccessPolicy policy=mock(MemberAccessPolicy.class);
 private final CheckinQrService qr=mock(CheckinQrService.class);
 private final CheckinCrypto crypto=new CheckinCrypto("secret-member-qr-very-long-and-distinct-00001","secret-kiosk-csrf-very-long-and-distinct-00002");
 private final ICheckinContextRepository context=mock(ICheckinContextRepository.class);
 private final ICheckinAccessCodeRepository codes=mock(ICheckinAccessCodeRepository.class);
 private final CheckinService service=new CheckinService(repo,auth,policy,qr,crypto,context,codes);
 @Test void manualContractRemainsUnchanged(){
  UUID branch=UUID.randomUUID(),member=UUID.randomUUID(),user=UUID.randomUUID(),event=UUID.randomUUID();
  when(auth.requireModule(eq(branch),eq("checkin"),any(String[].class))).thenReturn(new Actor(user,false));
  when(auth.actor()).thenReturn(new Actor(user,false));
  when(policy.evaluate(branch,member)).thenReturn(new MemberAccessPolicy.Decision(LocalDate.of(2026,11,5),true,"granted"));
  when(repo.record(eq(branch),eq(member),any(LocalDate.class),eq(true),eq("allowed"),eq(user))).thenReturn(event);
  var result=service.manual(branch,new CheckinRequest(member));
  assertEquals(event,result.get("eventId"));assertEquals("allowed",result.get("reason"));
  assertEquals(true,result.get("allowed"));
 }
 @Test void branchQrRetryReturnsPriorResultWithoutNewEvent(){
  UUID branch=UUID.randomUUID(),member=UUID.randomUUID(),user=UUID.randomUUID(),device=UUID.randomUUID();
  when(auth.requireModule(eq(branch),eq("checkin"),any(String[].class))).thenReturn(new Actor(user,false));
  when(repo.memberIdForUser(branch,user)).thenReturn(Optional.of(member));
  when(qr.validateBranch("sample-opaque",branch)).thenReturn(new CheckinQrService.BranchTokenState(true,"granted",device));
  when(policy.evaluate(branch,member)).thenReturn(new MemberAccessPolicy.Decision(LocalDate.of(2026,11,5),true,"granted"));
  when(repo.priorAttempt(eq(branch),anyString())).thenReturn(Optional.empty());
  when(repo.recordQr(eq(branch),eq(member),eq(device),eq("kiosk_qr"),any(LocalDate.class),eq(true),eq("granted"),eq(user),anyString(),anyString()))
    .thenReturn(new ICheckinRepository.Attempt(true,"granted",LocalDate.of(2026,11,5),OffsetDateTime.now(),"fingerprint"));
  service.scanBranch(branch,"sample-opaque",UUID.randomUUID().toString());
  verify(repo).recordQr(eq(branch),eq(member),eq(device),eq("kiosk_qr"),any(LocalDate.class),eq(true),eq("granted"),eq(user),anyString(),anyString());
 }
 @Test void reusedIdempotencyKeyDifferentPayloadIsConflict(){
  UUID branch=UUID.randomUUID(),member=UUID.randomUUID(),user=UUID.randomUUID();
  when(auth.requireModule(eq(branch),eq("checkin"),any(String[].class))).thenReturn(new Actor(user,false));
  when(repo.memberIdForUser(branch,user)).thenReturn(Optional.of(member));
  when(qr.validateBranch(anyString(),eq(branch))).thenReturn(new CheckinQrService.BranchTokenState(false,"invalid_token",null));
  when(repo.priorAttempt(eq(branch),anyString())).thenReturn(Optional.of(new ICheckinRepository.Attempt(true,"granted",LocalDate.now(),OffsetDateTime.now(),"different-body")));
  assertThrows(ConflictException.class,()->service.scanBranch(branch,"another-value",UUID.randomUUID().toString()));
  verify(repo,never()).recordQr(any(),any(),any(),any(),any(),anyBoolean(),any(),any(),any(),any());
 }
}
