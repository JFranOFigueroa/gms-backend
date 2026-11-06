package com.gymback.security;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;
/** Autenticación de cookie HttpOnly exclusivamente para rutas /kiosk protegidas. */
@Component
public class DeviceAuthenticationFilter extends OncePerRequestFilter {
 public static final String COOKIE="GMS_KIOSK";
 private final DeviceAuthorizationService authorization;
 public DeviceAuthenticationFilter(DeviceAuthorizationService authorization){this.authorization=authorization;}
 @Override protected boolean shouldNotFilter(HttpServletRequest req){
  String path=req.getRequestURI();
  return "OPTIONS".equalsIgnoreCase(req.getMethod()) || !path.startsWith("/api/v1/kiosk/") || path.equals("/api/v1/kiosk/activate");
 }
 @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)
    throws ServletException,IOException {
  String raw=null;
  if(request.getCookies()!=null)for(Cookie cookie:request.getCookies())
    if(COOKIE.equals(cookie.getName())){raw=cookie.getValue();break;}
  KioskPrincipal principal;
  try {
   principal=authorization.authenticate(raw);
  }catch(org.springframework.security.access.AccessDeniedException ex){
   response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
   response.setHeader(HttpHeaders.CACHE_CONTROL,"no-store");response.setContentType("application/json");
   response.getWriter().write("{\"error\":\"kiosk_unauthorized\",\"message\":\"Sesión de dispositivo no disponible\"}");
   return;
  }
  try {
   SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal,null,List.of()));
   chain.doFilter(request,response);
  } finally {SecurityContextHolder.clearContext();}
 }
}
