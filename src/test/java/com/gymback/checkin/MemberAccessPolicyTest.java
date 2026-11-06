package com.gymback.checkin;
import com.gymback.modules.checkin.services.MemberAccessPolicy;
import com.gymback.modules.checkin.interfaces.ICheckinRepository;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class MemberAccessPolicyTest {
 @Test void usesSingleBusinessRuleForAllCheckinMethods(){
  ICheckinRepository repo=mock(ICheckinRepository.class);UUID gym=UUID.randomUUID(),member=UUID.randomUUID();
  when(repo.lockMember(gym,member)).thenReturn(true);
  when(repo.evaluate(gym,member)).thenReturn(new ICheckinRepository.Access("no_payment","America/Mexico_City"));
  var policy=new MemberAccessPolicy(repo);
  assertEquals("no_payment",policy.evaluate(gym,member).reason());
  when(repo.evaluate(gym,member)).thenReturn(new ICheckinRepository.Access("unknown_validity","America/Mexico_City"));
  assertFalse(policy.evaluate(gym,member).allowed());
  when(repo.evaluate(gym,member)).thenReturn(new ICheckinRepository.Access("allowed","America/Mexico_City"));
  when(repo.alreadyAllowed(eq(gym),eq(member),any(LocalDate.class))).thenReturn(true);
  assertEquals("duplicate",policy.evaluate(gym,member).reason());
  when(repo.alreadyAllowed(eq(gym),eq(member),any(LocalDate.class))).thenReturn(false);
  assertTrue(policy.evaluate(gym,member).allowed());
 }
 @Test void memberOfOtherGymIsNeverAllowed(){
  ICheckinRepository repo=mock(ICheckinRepository.class);UUID gym=UUID.randomUUID(),member=UUID.randomUUID();
  assertEquals("not_found",new MemberAccessPolicy(repo).evaluate(gym,member).reason());
  verify(repo,never()).evaluate(gym,member);
 }
}
