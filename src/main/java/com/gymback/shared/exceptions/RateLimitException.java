package com.gymback.shared.exceptions;
/** Cuota agotada; el cliente puede reintentar después de retryAfterSeconds. */
public class RateLimitException extends RuntimeException {
 private final int retryAfterSeconds;
 public RateLimitException(int retryAfterSeconds){super("Demasiados intentos. Intenta más tarde.");this.retryAfterSeconds=retryAfterSeconds;}
 public int getRetryAfterSeconds(){return retryAfterSeconds;}
}
