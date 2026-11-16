package com.gymback.branches;
import com.gymback.modules.branches.services.BranchCodePolicy;
import com.gymback.shared.exceptions.DomainException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class BranchCodePolicyTest {
 @Test void normalizesAccentAndWhitespace(){
   assertEquals("sucursal-elite",BranchCodePolicy.normalize("Sucursal Élite"));
   assertEquals("iron-fitness",BranchCodePolicy.normalize(" Iron   Fitness! "));
 }
 @Test void rejectsReservedAndSmall(){
   assertThrows(DomainException.class,()->BranchCodePolicy.normalize("api"));
   assertThrows(DomainException.class,()->BranchCodePolicy.normalize("a"));
 }
}
