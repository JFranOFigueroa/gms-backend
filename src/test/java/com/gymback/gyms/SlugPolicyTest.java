package com.gymback.gyms;
import com.gymback.modules.gyms.services.SlugPolicy;
import com.gymback.shared.exceptions.DomainException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SlugPolicyTest {
 @Test void normalizesAccentAndWhitespace(){
   assertEquals("gimnasio-elite",SlugPolicy.normalize("Gimnasio Élite"));
   assertEquals("iron-fitness",SlugPolicy.normalize(" Iron   Fitness! "));
 }
 @Test void rejectsReservedAndSmall(){
   assertThrows(DomainException.class,()->SlugPolicy.normalize("api"));
   assertThrows(DomainException.class,()->SlugPolicy.normalize("a"));
 }
}
