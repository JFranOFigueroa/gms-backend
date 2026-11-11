package com.gymback.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/** Never trust X-Forwarded-For unless the direct peer is an explicitly trusted proxy. */
@Component
public class ClientIpResolver {
  private final Set<String> trusted;

  public ClientIpResolver(@Value("${TRUSTED_PROXY_IPS:}") String trustedProxies) {
    this.trusted=Arrays.stream(trustedProxies.split(","))
      .map(String::trim).filter(s->!s.isEmpty()).collect(Collectors.toUnmodifiableSet());
  }

  public String resolve(HttpServletRequest request) {
    String peer=request.getRemoteAddr();
    if(peer==null || !trusted.contains(peer))return peer==null?"unknown":peer;
    String forwarded=request.getHeader("X-Forwarded-For");
    if(forwarded==null || forwarded.isBlank())return peer;
    // The rightmost untrusted hop is the actual client (trusted proxies may append hops).
    String[] hops=forwarded.split(",",-1);
    for(int i=hops.length-1;i>=0;i--){
      String hop=hops[i].trim();
      if(!isIpLiteral(hop))return peer; // reject malformed/spoofed chains
      if(!trusted.contains(hop))return hop;
    }
    return peer;
  }

  private boolean isIpLiteral(String candidate) {
    // Avoid DNS lookups: forwarded values must be numeric IPv4/IPv6 literals.
    if(candidate.isEmpty() || candidate.length()>45 || !candidate.matches("[0-9a-fA-F:.]+")
        || (!candidate.contains(":") && !candidate.matches("(?:[0-9]{1,3}\\.){3}[0-9]{1,3}")))return false;
    if(!candidate.contains(":")){
      for(String part:candidate.split("\\.")){
        int octet=Integer.parseInt(part);
        if(octet>255)return false;
      }
      return true;
    }
    try {
      InetAddress address=InetAddress.getByName(candidate); // IPv6 literals contain ':', never DNS names
      return address.getHostAddress()!=null;
    } catch(Exception ignored){return false;}
  }
}
