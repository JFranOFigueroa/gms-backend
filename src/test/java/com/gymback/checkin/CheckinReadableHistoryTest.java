package com.gymback.checkin;

import com.gymback.modules.checkin.repositories.CheckinRepository;
import com.gymback.shared.persistence.model.CheckinEntry;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CheckinReadableHistoryTest {
 @SuppressWarnings("unchecked")
 @Test void privateHistoryAddsMemberNameWithoutChangingOriginalFields(){
  EntityManager em=mock(EntityManager.class);
  TypedQuery<CheckinEntry> events=mock(TypedQuery.class);
  TypedQuery<Object[]> names=mock(TypedQuery.class);
  UUID gym=UUID.randomUUID(),member=UUID.randomUUID();
  CheckinEntry e=new CheckinEntry();e.gymId=gym;e.memberId=member;e.method="kiosk_code";
  e.allowed=true;e.reasonCode="granted";e.visitDate=LocalDate.of(2026,11,5);e.occurredAt=OffsetDateTime.now();
  when(em.createQuery(contains("select e from CheckinEntry"),eq(CheckinEntry.class))).thenReturn(events);
  when(events.setParameter(eq("gym"),eq(gym))).thenReturn(events);
  when(events.setMaxResults(50)).thenReturn(events);
  when(events.getResultList()).thenReturn(List.of(e));
  when(em.createQuery(contains("select m.id,u.fullName"),eq(Object[].class))).thenReturn(names);
  when(names.setParameter(anyString(),any())).thenReturn(names);
  when(names.getResultList()).thenReturn(Collections.singletonList(new Object[]{member,"Miembro Ejemplo"}));
  var result=new CheckinRepository(em).list(gym,50);
  assertEquals(1,result.size());
  var item=result.get(0);
  assertEquals(member,item.get("member_id"));
  assertEquals("kiosk_code",item.get("method"));
  assertEquals("granted",item.get("reason_code"));
  assertEquals("Miembro Ejemplo",item.get("member_name"));
  assertEquals("Miembro Ejemplo",item.get("display_name"));
  assertTrue(item.containsKey("created_by"));
  assertTrue(item.containsKey("device_name"));
 }
}
