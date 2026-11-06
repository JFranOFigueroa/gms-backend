package com.gymback.modules.checkin.services;
import com.gymback.shared.exceptions.DomainException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
/** Datos opacos de alta entropía; la firma de QR nunca equivale a un JWT de usuario. */
@Component
public class CheckinCrypto {
 private final SecureRandom random=new SecureRandom();
 private final byte[] memberKey,csrfKey;
 public CheckinCrypto(@Value("${app.checkin.member-qr-secret}") String memberSecret,
                      @Value("${app.checkin.kiosk-csrf-secret}")String csrfSecret){
  this.memberKey=key(memberSecret,"gms/member-checkin/v1");
  this.csrfKey=key(csrfSecret,"gms/kiosk-csrf/v1");
 }
 private byte[] key(String secret,String purpose){
  if(secret==null||secret.getBytes(StandardCharsets.UTF_8).length<32)
   throw new IllegalStateException("El secreto criptográfico de check-in debe tener al menos 32 bytes");
  return hmac(secret.getBytes(StandardCharsets.UTF_8),purpose.getBytes(StandardCharsets.UTF_8));
 }
 private static byte[] hmac(byte[] key,byte[] input){
  try {Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(key,"HmacSHA256"));return mac.doFinal(input);}
  catch(GeneralSecurityException e){throw new IllegalStateException(e);}
 }
 private static final String MANUAL_ALPHABET="23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
 public String newAccessCode(){
  StringBuilder raw=new StringBuilder(14);
  for(int i=0;i<12;i++){
   if(i==4||i==8)raw.append('-');
   raw.append(MANUAL_ALPHABET.charAt(random.nextInt(MANUAL_ALPHABET.length())));
  }
  return raw.toString();
 }
 public String newToken(){byte[] bytes=new byte[32];random.nextBytes(bytes);return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);}
 public String hash(String text){
  try {byte[] bytes=MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));return HexFormat.of().formatHex(bytes);}
  catch(GeneralSecurityException e){throw new IllegalStateException(e);}
 }
 public String csrf(String sessionSecret){return Base64.getUrlEncoder().withoutPadding().encodeToString(hmac(csrfKey,("csrf:"+sessionSecret).getBytes(StandardCharsets.UTF_8)));}
 public boolean matchesCsrf(String sessionSecret,String provided){
  return provided!=null && MessageDigest.isEqual(csrf(sessionSecret).getBytes(StandardCharsets.US_ASCII),provided.getBytes(StandardCharsets.US_ASCII));
 }
 public String memberQr(UUID gym,UUID member,OffsetDateTime now){
  long issued=now.toEpochSecond();long expires=issued+30;
  String claim=String.join("|","1","gms","kiosk","member-checkin",gym.toString(),member.toString(),
    Long.toString(issued),Long.toString(expires),UUID.randomUUID().toString());
  String payload=Base64.getUrlEncoder().withoutPadding().encodeToString(claim.getBytes(StandardCharsets.UTF_8));
  String signature=Base64.getUrlEncoder().withoutPadding().encodeToString(hmac(memberKey,payload.getBytes(StandardCharsets.US_ASCII)));
  return payload+"."+signature;
 }
 public record MemberClaims(UUID gym,UUID member,OffsetDateTime expiresAt){}
 public MemberClaims verifyMemberQr(String input,OffsetDateTime now){
  if(input==null||input.length()>2048||input.length()<40)throw new DomainException("Credencial inválida");
  String[] chunks=input.split("\\.",-1);if(chunks.length!=2)throw new DomainException("Credencial inválida");
  try {
   byte[] signature=Base64.getUrlDecoder().decode(chunks[1]);
   byte[] expected=hmac(memberKey,chunks[0].getBytes(StandardCharsets.US_ASCII));
   if(!MessageDigest.isEqual(signature,expected))throw new DomainException("Credencial inválida");
   String payload=new String(Base64.getUrlDecoder().decode(chunks[0]),StandardCharsets.UTF_8);
   String[] p=payload.split("\\|",-1);
   if(p.length!=9||!p[0].equals("1")||!p[1].equals("gms")||!p[2].equals("kiosk")||!p[3].equals("member-checkin"))
     throw new DomainException("Credencial inválida");
   long issued=Long.parseLong(p[6]),expires=Long.parseLong(p[7]),nowSeconds=now.toEpochSecond();
   if(expires-issued!=30||issued>nowSeconds+2||expires<=nowSeconds)throw new DomainException("Credencial expirada");
   UUID.fromString(p[8]);
   return new MemberClaims(UUID.fromString(p[4]),UUID.fromString(p[5]),OffsetDateTime.ofInstant(Instant.ofEpochSecond(expires),ZoneOffset.UTC));
  }catch(IllegalArgumentException e){throw new DomainException("Credencial inválida");}
 }
}
