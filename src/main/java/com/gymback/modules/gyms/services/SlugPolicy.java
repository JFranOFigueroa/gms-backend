package com.gymback.modules.gyms.services;
import com.gymback.shared.exceptions.DomainException;
import java.text.Normalizer;
import java.util.*;
public final class SlugPolicy {
  private SlugPolicy(){}
  private static final Set<String> RESERVED=Set.of("admin","api","auth","login","dashboard","checkin","g","public","root","www");
  public static String normalize(String value){
    if(value==null || value.isBlank())throw new DomainException("El nombre del gimnasio es obligatorio");
    String n=Normalizer.normalize(value,Normalizer.Form.NFD).replaceAll("\\p{M}+","")
      .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+","-").replaceAll("^-+|-+$","");
    if(n.length()<3 || n.length()>80 || RESERVED.contains(n))throw new DomainException("El enlace elegido no es válido (3 a 80 caracteres)");
    return n;
  }
}
