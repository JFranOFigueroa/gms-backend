package com.gymback.modules.pos.routes;
import com.gymback.modules.pos.dto.*;
import com.gymback.modules.pos.services.SaleService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import java.util.*;
@RestController
@RequestMapping("/api/v1/gyms/{gymId}/sales")
public class SaleController {
  private final SaleService service;
  public SaleController(SaleService service){this.service=service;}
  @GetMapping public List<SaleView> list(@PathVariable UUID gymId,@RequestParam(defaultValue="50") int limit){
    return service.list(gymId,limit);
  }
  @PostMapping public ResponseEntity<SaleView> checkout(@PathVariable UUID gymId,@RequestBody CheckoutRequest request){
    return ResponseEntity.status(HttpStatus.CREATED).body(service.checkout(gymId,request));
  }
}
