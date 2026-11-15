package com.gymback.checkin;

import com.gymback.modules.checkin.dto.CheckinQrDtos.*;
import com.gymback.modules.checkin.interfaces.*;
import com.gymback.modules.checkin.model.CheckinAccessCode;
import com.gymback.modules.checkin.services.*;
import com.gymback.security.*;
import com.gymback.shared.exceptions.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CheckinManualCodeTest {
 private final ICheckinRepository repo=mock(ICheckinRepository.class);
 private final ICheckinAccessCodeRepository codes=mock(ICheckinAccessCodeRepository.class);
 private final AuthorizationService auth=mock(AuthorizationService.class);
 private final MemberAccessPolicy policy=mock(MemberAccessPolicy.class);
 private final CheckinQrService qr=mock(CheckinQrService.class);
 private final ICheckinContextRepository context=mock(ICheckinContextRepository.class);
 private final CheckinCrypto crypto=new CheckinCrypto("secret-member-qr-very-long-and-distinct-00001","secret-kiosk-csrf-very-long-and-distinct-00002");
 private final CheckinService service=new CheckinService(repo,auth,policy,qr,crypto,context,codes);
 private final UUID branch=UUID.randomUUID(),member=UUID.randomUUID(),user=UUID.randomUUID(),device=UUID.randomUUID();
 private KioskPrincipal kiosk(){return new KioskPrincipal(branch,device,UUID.randomUUID(),"Recepción",OffsetDateTime.now().plusHours(1),crypto.newToken());}
 private String raw(){return "ABCD-EFGH-JKLM";}
 private String hash(){return crypto.hash("checkin-access-code:v1|"+branch+"|ABCDEFGHJKLM");}
 @Test void issueIsShortLivedHashedScopedAndRevokesPrevious(){
  when(auth.requireModule(eq(branch),eq("checkin"),any(String[].class))).thenReturn(new Actor(user,false));
  when(repo.memberIdForUser(branch,user)).thenReturn(Optional.of(member));
  when(repo.lockMember(branch,member)).thenReturn(true);
  IssuedCode issued=service.issueManualCode(branch);
  assertTrue(issued.accessCode().matches("[23456789ABCDEFGHJKLMNPQRSTUVWXYZ]{4}(-[23456789ABCDEFGHJKLMNPQRSTUVWXYZ]{4}){2}"));
  assertEquals(120,Duration.between(issued.serverTime(),issued.expiresAt()).toSeconds());
  var captor=ArgumentCaptor.forClass(CheckinAccessCode.class);
  verify(codes).save(captor.capture());
  assertEquals(branch,captor.getValue().branchId);assertEquals(member,captor.getValue().memberId);
  assertFalse(captor.getValue().codeHash.contains(issued.accessCode()));
  assertEquals(crypto.hash("checkin-access-code:v1|"+branch+"|"+issued.accessCode().replace("-","")),captor.getValue().codeHash);
  verify(codes).revokeOutstanding(eq(branch),eq(member),any(OffsetDateTime.class));
 }
 @Test void issueIsRateLimited(){
  when(auth.requireModule(eq(branch),eq("checkin"),any(String[].class))).thenReturn(new Actor(user,false));
  when(repo.memberIdForUser(branch,user)).thenReturn(Optional.of(member));
  when(repo.lockMember(branch,member)).thenReturn(true);
  when(codes.issuedSince(eq(branch),eq(member),any(OffsetDateTime.class))).thenReturn(3L);
  assertThrows(RateLimitException.class,()->service.issueManualCode(branch));
  verify(codes,never()).save(any());
 }
 @Test void validManualCodeConsumesAndUsesSharedPolicy(){
  String key=UUID.randomUUID().toString();
  var code=new CheckinAccessCode();code.branchId=branch;code.memberId=member;code.expiresAt=OffsetDateTime.now().plusSeconds(100);
  when(repo.lockMember(branch,member)).thenReturn(true);
  when(codes.byHash(branch,hash())).thenReturn(Optional.of(code));
  when(codes.lockByHash(branch,hash())).thenReturn(Optional.of(code));
  when(policy.evaluate(branch,member)).thenReturn(new MemberAccessPolicy.Decision(LocalDate.of(2026,11,5),true,"granted"));
  when(repo.recordQr(eq(branch),eq(member),eq(device),eq("kiosk_code"),any(LocalDate.class),eq(true),eq("granted"),isNull(),anyString(),anyString()))
   .thenReturn(new ICheckinRepository.Attempt(true,"granted",LocalDate.of(2026,11,5),OffsetDateTime.now(),"fp"));
  CheckinResult result=service.validateCode(kiosk(),raw(),key);
  assertTrue(result.allowed());assertEquals("granted",result.reasonCode());assertNotNull(code.consumedAt);
  verify(repo).recordQr(eq(branch),eq(member),eq(device),eq("kiosk_code"),any(),eq(true),eq("granted"),isNull(),anyString(),anyString());
 }
 @Test void unknownCodeIsDeniedAndRecordedWithoutMemberIdentity(){
  when(repo.recordQr(eq(branch),isNull(),eq(device),eq("kiosk_code"),any(LocalDate.class),eq(false),eq("invalid_code"),isNull(),anyString(),anyString()))
   .thenReturn(new ICheckinRepository.Attempt(false,"invalid_code",LocalDate.now(),OffsetDateTime.now(),"fp"));
  CheckinResult result=service.validateCode(kiosk(),"ABC-INVALID",UUID.randomUUID().toString());
  assertFalse(result.allowed());assertEquals("invalid_code",result.reasonCode());
  verify(repo).recordQr(eq(branch),isNull(),eq(device),eq("kiosk_code"),any(),eq(false),eq("invalid_code"),isNull(),anyString(),anyString());
 }
 @Test void usedCodeCannotBeReusedWithNewKey(){
  var old=new CheckinAccessCode();old.memberId=member;old.branchId=branch;old.expiresAt=OffsetDateTime.now().plusSeconds(100);old.consumedAt=OffsetDateTime.now().minusSeconds(1);
  when(codes.byHash(branch,hash())).thenReturn(Optional.of(old));
  when(codes.lockByHash(branch,hash())).thenReturn(Optional.of(old));
  when(repo.lockMember(branch,member)).thenReturn(true);
  when(repo.recordQr(eq(branch),isNull(),eq(device),eq("kiosk_code"),any(),eq(false),eq("invalid_code"),isNull(),anyString(),anyString()))
   .thenReturn(new ICheckinRepository.Attempt(false,"invalid_code",LocalDate.now(),OffsetDateTime.now(),"fp"));
  assertFalse(service.validateCode(kiosk(),raw(),UUID.randomUUID().toString()).allowed());
  verify(repo,never()).recordQr(eq(branch),eq(member),any(),any(),any(),anyBoolean(),any(),any(),any(),any());
 }
 @Test void idempotentRetryReturnsOriginalResultEvenAfterCodeConsumed(){
  String key=UUID.randomUUID().toString();
  when(repo.priorAttempt(eq(branch),anyString())).thenReturn(Optional.of(new ICheckinRepository.Attempt(true,"granted",LocalDate.of(2026,11,5),OffsetDateTime.now(),
   crypto.hash("manual-code|"+branch+"|"+crypto.hash("ABCDEFGHJKLM")))));
  assertTrue(service.validateCode(kiosk(),raw(),key).allowed());
  verify(codes,never()).lockByHash(any(),anyString());
  verify(repo,never()).recordQr(any(),any(),any(),any(),any(),anyBoolean(),any(),any(),any(),any());
 }
 @Test void kioskCodeAttemptsAreRateLimited(){
  when(repo.countRecentCodeAttempts(eq(branch),eq(device),any(OffsetDateTime.class))).thenReturn(10L);
  assertThrows(RateLimitException.class,()->service.validateCode(kiosk(),raw(),UUID.randomUUID().toString()));
  verify(codes,never()).lockByHash(any(),anyString());
 }
}
