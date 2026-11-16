package com.gymback.modules.payments.routes;
import com.gymback.modules.payments.dto.PaymentRequest;
import com.gymback.modules.payments.services.PaymentService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import java.util.*;
@RestController @RequestMapping("/api/v1/branches/{branchId}/payments")
public class PaymentController {
 private final PaymentService s;public PaymentController(PaymentService s){this.s=s;}
 @GetMapping public List<Map<String,Object>> list(@PathVariable UUID branchId,@RequestParam(required=false)UUID memberId,@RequestParam(defaultValue="50")int limit){return s.list(branchId,memberId,limit);}
 @PostMapping public ResponseEntity<Map<String,Object>> register(@PathVariable UUID branchId,@RequestBody PaymentRequest d,
   @RequestHeader(value="Idempotency-Key",required=false)String key){
   return ResponseEntity.status(201).body(s.register(branchId,d,key));
 }
}
