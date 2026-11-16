package com.gymback.routines;
import com.gymback.modules.routines.model.Routine;
import com.gymback.modules.routines.interfaces.*;
import com.gymback.modules.routines.services.RoutineService;
import com.gymback.security.*;
import com.gymback.shared.services.AuditService;
import com.gymback.shared.exceptions.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.*;

class RoutineVisibilityTest {
  @Test void membersUsePublishedDatabaseFilterBeforePagination() {
    var repo=mock(IRoutineRepository.class);var structure=mock(IRoutineStructureRepository.class);
    var auth=mock(AuthorizationService.class);UUID branch=UUID.randomUUID(),user=UUID.randomUUID();
    when(auth.require(eq(branch),any(String[].class))).thenReturn(new Actor(user,false));
    var service=new RoutineService(repo,mock(AuditService.class),auth,structure);
    assertTrue(service.all(branch,0,20).isEmpty());
    verify(repo).getWhere(eq(branch),any(Specification.class),eq(0),eq(20));
    verify(repo,never()).list(eq(branch),anyInt(),anyInt());
  }
  @Test void membersCannotReadUnpublishedRoutineById() {
    var repo=mock(IRoutineRepository.class);var auth=mock(AuthorizationService.class);
    UUID branch=UUID.randomUUID(),id=UUID.randomUUID();
    var routine=new Routine();routine.setId(id);routine.setBranchId(branch);routine.setPublished(false);
    when(repo.get(id,branch)).thenReturn(Optional.of(routine));
    when(auth.require(eq(branch),any(String[].class))).thenReturn(new Actor(UUID.randomUUID(),false));
    var structure=mock(IRoutineStructureRepository.class);
    var service=new RoutineService(repo,mock(AuditService.class),auth,structure);
    assertThrows(NotFoundException.class,()->service.one(branch,id));
    verifyNoInteractions(structure);
  }
}
