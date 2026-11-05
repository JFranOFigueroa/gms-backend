package com.gymback.shared.api;
import com.gymback.shared.exceptions.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.dao.DataIntegrityViolationException;
import java.util.*;
@RestControllerAdvice
public class ApiExceptionHandler {
  @ExceptionHandler(org.springframework.security.authentication.BadCredentialsException.class)
  ResponseEntity<Map<String,String>> badCredentials(Exception e){return ResponseEntity.status(401).body(Map.of("error","unauthorized","message","Credenciales inválidas"));}
  @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
  ResponseEntity<Map<String,String>> fileTooLarge(Exception e){return ResponseEntity.status(413).body(Map.of("error","file_too_large","message","La imagen supera el tamaño permitido"));}
  @ExceptionHandler(NotFoundException.class)
  ResponseEntity<Map<String,String>> notFound(NotFoundException e){return ResponseEntity.status(404).body(Map.of("error","not_found","message",e.getMessage()));}
  @ExceptionHandler(ConflictException.class)
  ResponseEntity<Map<String,String>> businessConflict(ConflictException e){return ResponseEntity.status(409).body(Map.of("error",e.getCode(),"message",e.getMessage()));}
  @ExceptionHandler(UnprocessableEntityException.class)
  ResponseEntity<Map<String,String>> semantic(UnprocessableEntityException e){return ResponseEntity.status(422).body(Map.of("error",e.getCode(),"message",e.getMessage()));}
  @ExceptionHandler(DomainException.class)
  ResponseEntity<Map<String,String>> domain(DomainException e){return ResponseEntity.badRequest().body(Map.of("error","business_rule","message",e.getMessage()));}
  @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
  ResponseEntity<Map<String,String>> forbidden(Exception e){return ResponseEntity.status(403).body(Map.of("error","forbidden","message","Acceso denegado"));}
  @ExceptionHandler({MethodArgumentNotValidException.class,MethodArgumentTypeMismatchException.class,IllegalArgumentException.class})
  ResponseEntity<Map<String,String>> invalid(Exception e){return ResponseEntity.badRequest().body(Map.of("error","invalid_request","message","Datos inválidos"));}
  @ExceptionHandler(DataIntegrityViolationException.class)
  ResponseEntity<Map<String,String>> conflict(Exception e){return ResponseEntity.status(409).body(Map.of("error","constraint_violation","message","Conflicto de integridad"));}
}
