package com.gymback.security;

import com.gymback.shared.exceptions.RateLimitException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LoginAttemptLimiterTest {
  @Test void fiveFailuresWithinFifteenMinutesBlockNextAttemptAndReturnRetryAfter(){
    var limiter=new LoginAttemptLimiter();
    for(int n=0;n<5;n++){
      limiter.check("TEST@example.test","203.0.113.1");
      limiter.failed("test@example.test","203.0.113.1");
    }
    RateLimitException blocked=assertThrows(RateLimitException.class,
      ()->limiter.check("test@example.test","198.51.100.2"));
    assertTrue(blocked.getRetryAfterSeconds()>=1);
    assertTrue(blocked.getRetryAfterSeconds()<=900);
    assertEquals(5,limiter.failedCount());
    assertEquals(1,limiter.blockedCount());
  }

  @Test void successfulLoginResetsAccountFailuresButNotIpFailureHistory(){
    var limiter=new LoginAttemptLimiter();
    for(int n=0;n<4;n++){
      limiter.check("member@example.test","203.0.113.9");
      limiter.failed("member@example.test","203.0.113.9");
    }
    limiter.check("member@example.test","203.0.113.9");
    limiter.succeeded("member@example.test","203.0.113.9");
    for(int n=0;n<4;n++){
      limiter.check("MEMBER@example.test","203.0.113.9");
      limiter.failed("member@example.test","203.0.113.9");
    }
    limiter.check("member@example.test","203.0.113.9");
    limiter.aborted("member@example.test","203.0.113.9");
  }
}
