package com.gymback.security;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ClientIpResolverTest {
  @Test void spoofedHeaderFromUntrustedPeerIsIgnored(){
    var request=mock(HttpServletRequest.class);
    when(request.getRemoteAddr()).thenReturn("203.0.113.10");
    when(request.getHeader("X-Forwarded-For")).thenReturn("127.0.0.1");
    assertEquals("203.0.113.10",new ClientIpResolver("10.0.0.1").resolve(request));
  }

  @Test void trustedProxyUsesClosestUntrustedForwardedHop(){
    var request=mock(HttpServletRequest.class);
    when(request.getRemoteAddr()).thenReturn("10.0.0.1");
    when(request.getHeader("X-Forwarded-For")).thenReturn("127.0.0.1, 198.51.100.5");
    assertEquals("198.51.100.5",new ClientIpResolver("10.0.0.1").resolve(request));
  }
}
