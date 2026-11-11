package com.gymback.security;
import com.gymback.security.dto.AuthSession;
import com.gymback.security.dto.LoginResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.authentication.BadCredentialsException;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
  private final AuthService service;
  private final LoginAttemptLimiter attempts;
  private final ClientIpResolver clients;
  public AuthController(AuthService service,LoginAttemptLimiter attempts,ClientIpResolver clients) {
    this.service=service;this.attempts=attempts;this.clients=clients;
  }
  public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {}
  @PostMapping("/login")
  public LoginResponse login(@Valid @RequestBody LoginRequest request,HttpServletRequest http) {
    String ip=clients.resolve(http);
    attempts.check(request.email(),ip);
    try {
      LoginResponse response=service.login(request.email(),request.password());
      attempts.succeeded(request.email(),ip);
      return response;
    }catch(BadCredentialsException invalid){
      attempts.failed(request.email(),ip);
      throw invalid;
    }catch(RuntimeException unexpected){
      attempts.aborted(request.email(),ip);
      throw unexpected;
    }
  }
  public record RefreshRequest(@NotBlank String refreshToken) {}
  @PostMapping("/refresh")
  public LoginResponse refresh(@Valid @RequestBody RefreshRequest request){return service.refresh(request.refreshToken());}
  @PostMapping("/logout")
  @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
  public void logout(@Valid @RequestBody RefreshRequest request){service.logout(request.refreshToken());}
  @GetMapping("/me")
  public AuthSession me() { return service.me(); }
}
