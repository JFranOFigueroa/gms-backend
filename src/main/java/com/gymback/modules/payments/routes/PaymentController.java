package com.gymback.modules.payments.routes;
import com.gymback.modules.payments.dto.PaymentRequest;
import com.gymback.modules.payments.services.PaymentService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import java.util.*;
@RestController @RequestMapping("/api/v1/gyms/{gymId}/payments")
public class PaymentController {
 private final PaymentService s;public PaymentController(PaymentService s){this.s=s;}
 @GetMapping public List<Map<String,Object>> list(@PathVariable UUID gymId,@RequestParam(required=false)UUID memberId,@RequestParam(defaultValue="50")int limit){return s.list(gymId,memberId,limit);}
 @PostMapping public ResponseEntity<Map<String,Object>> register(@PathVariable UUID gymId,@RequestBody PaymentRequest d,
   @RequestHeader(value="Idempotency-Key",required=false)String key){
   return ResponseEntity.status(201).body(s.register(gymId,d,key));
 }
}
