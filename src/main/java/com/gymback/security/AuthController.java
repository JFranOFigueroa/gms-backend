package com.gymback.security;
import com.gymback.security.dto.AuthSession;
import com.gymback.security.dto.LoginResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
  private final AuthService service;
  public AuthController(AuthService service) { this.service = service; }
  public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {}
  @PostMapping("/login")
  public LoginResponse login(@Valid @RequestBody LoginRequest request) {
    return service.login(request.email(), request.password());
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
