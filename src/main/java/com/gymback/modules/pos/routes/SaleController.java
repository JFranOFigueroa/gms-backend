package com.gymback.modules.pos.routes;
import com.gymback.modules.pos.dto.*;
import com.gymback.modules.pos.services.SaleService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import java.util.*;
@RestController
@RequestMapping("/api/v1/branches/{branchId}/sales")
public class SaleController {
  private final SaleService service;
  public SaleController(SaleService service){this.service=service;}
  @GetMapping public List<SaleView> list(@PathVariable UUID branchId,@RequestParam(defaultValue="50") int limit){
    return service.list(branchId,limit);
  }
  @PostMapping public ResponseEntity<SaleView> checkout(@PathVariable UUID branchId,@RequestBody CheckoutRequest request,
    @RequestHeader(value="Idempotency-Key",required=false)String key){
    return ResponseEntity.status(HttpStatus.CREATED).body(service.checkout(branchId,request,key));
  }
}
