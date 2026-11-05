package com.gymback.modules.pos.routes;
import com.gymback.modules.pos.services.ProductService;
import com.gymback.modules.pos.dto.ProductRequest;
import com.gymback.modules.pos.model.Product;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import java.util.*;
@RestController
@RequestMapping("/api/v1/gyms/{gymId}/products")
public class ProductController {
  private final ProductService service;
  public ProductController(ProductService service){this.service=service;}
  @GetMapping public List<Product> list(@PathVariable UUID gymId,@RequestParam(defaultValue="0") int offset,@RequestParam(defaultValue="50") int limit) {return service.all(gymId,offset,limit);}
  @GetMapping("/{id}")public Product get(@PathVariable UUID gymId,@PathVariable UUID id){return service.one(gymId,id);}
  @PostMapping public ResponseEntity<Product> create(@PathVariable UUID gymId,@RequestBody ProductRequest request){return ResponseEntity.status(201).body(service.add(gymId,request));}
  @PatchMapping("/{id}")public Product patch(@PathVariable UUID gymId,@PathVariable UUID id,@RequestBody ProductRequest request){return service.change(gymId,id,request);}
  @DeleteMapping("/{id}")@ResponseStatus(HttpStatus.NO_CONTENT)public void delete(@PathVariable UUID gymId,@PathVariable UUID id){service.remove(gymId,id);}
}
