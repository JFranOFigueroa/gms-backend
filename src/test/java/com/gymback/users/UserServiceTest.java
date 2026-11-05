package com.gymback.users;
import com.gymback.modules.users.interfaces.IUserRepository;
import com.gymback.modules.users.services.UserService;
import com.gymback.modules.users.dto.UserCreateRequest;
import com.gymback.security.AuthorizationService;
import com.gymback.shared.exceptions.ConflictException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.access.AccessDeniedException;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class UserServiceTest {
 @Test void featureFlagPreservesSqlOnlyProvisioningByDefault(){
   var service=new UserService(mock(IUserRepository.class),mock(AuthorizationService.class),mock(PasswordEncoder.class),false);
   assertThrows(AccessDeniedException.class,()->service.create(new UserCreateRequest("A","a@example.com",null,"password-12345",null)));
 }
 @Test void requiresUniqueEmailAndHashesPassword(){
   var repo=mock(IUserRepository.class);var auth=mock(AuthorizationService.class);var encoder=mock(PasswordEncoder.class);
   when(repo.byEmail("ana@example.com")).thenReturn(Optional.empty());
   when(encoder.encode("password-12345")).thenReturn("$2a$hash");
   UUID id=UUID.randomUUID();when(repo.create(anyString(),anyString(),any(),anyString(),anyString())).thenReturn(Map.of("id",id));
   var service=new UserService(repo,auth,encoder,true);
   assertEquals(id,service.create(new UserCreateRequest("Ana","ANA@EXAMPLE.COM",null,"password-12345",null)).get("id"));
   verify(repo).create("Ana","ana@example.com",null,"$2a$hash","active");
 }
 @Test void lastPlatformAdminCannotBeDisabled(){
   var repo=mock(IUserRepository.class);var id=UUID.randomUUID();
   when(repo.byId(id)).thenReturn(Optional.of(Map.of("id",id)));
   when(repo.isPlatformAdmin(id)).thenReturn(true);when(repo.activeAdmins()).thenReturn(1L);
   var service=new UserService(repo,mock(AuthorizationService.class),mock(PasswordEncoder.class),true);
   assertThrows(ConflictException.class,()->service.status(id,"disabled"));
   verify(repo,never()).status(any(),anyString());
 }
}
