package com.gymback.checkin;
import com.gymback.modules.checkin.services.CheckinCrypto;
import com.gymback.shared.exceptions.DomainException;
import org.junit.jupiter.api.Test;
import java.time.OffsetDateTime;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
class CheckinCryptoTest {
 private final CheckinCrypto crypto=new CheckinCrypto("secret-member-qr-very-long-and-distinct-00001","secret-kiosk-csrf-very-long-and-distinct-00002");
 @Test void signedMemberQrHasGymPurposeAndThirtySecondExpiry(){
  UUID gym=UUID.randomUUID(),member=UUID.randomUUID();OffsetDateTime issued=OffsetDateTime.now();
  String qr=crypto.memberQr(gym,member,issued);
  var claims=crypto.verifyMemberQr(qr,issued.plusSeconds(2));
  assertEquals(gym,claims.gym());assertEquals(member,claims.member());
  assertThrows(DomainException.class,()->crypto.verifyMemberQr(qr,issued.plusSeconds(31)));
 }
 @Test void rejectsTamperingAndStaticMemberCodes(){
  String qr=crypto.memberQr(UUID.randomUUID(),UUID.randomUUID(),OffsetDateTime.now());
  int dot=qr.indexOf('.');String tampered=qr.substring(0,dot+1)+(qr.charAt(dot+1)=='A'?'B':'A')+qr.substring(dot+2);
  assertThrows(DomainException.class,()->crypto.verifyMemberQr(tampered,OffsetDateTime.now()));
  assertThrows(DomainException.class,()->crypto.verifyMemberQr("GMS-MEMBER:ABC123",OffsetDateTime.now()));
 }
 @Test void manualCodesAreHumanReadableAndHighEntropy(){
  String code=crypto.newAccessCode();
  assertTrue(code.matches("[23456789ABCDEFGHJKLMNPQRSTUVWXYZ]{4}(-[23456789ABCDEFGHJKLMNPQRSTUVWXYZ]{4}){2}"));
  assertEquals(14,code.length());
  assertNotEquals(code,crypto.newAccessCode());
 }
 @Test void csrfIsBoundToCookieSecret(){
  String secret=crypto.newToken();String csrf=crypto.csrf(secret);
  assertTrue(crypto.matchesCsrf(secret,csrf));
  assertFalse(crypto.matchesCsrf(crypto.newToken(),csrf));
  assertFalse(crypto.matchesCsrf(secret,"invalid"));
 }
}
