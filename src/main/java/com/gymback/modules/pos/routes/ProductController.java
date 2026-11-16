package com.gymback.modules.pos.routes;
import com.gymback.modules.pos.services.ProductService;
import com.gymback.modules.pos.dto.ProductRequest;
import com.gymback.modules.pos.model.Product;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import java.util.*;
@RestController
@RequestMapping("/api/v1/branches/{branchId}/products")
public class ProductController {
  private final ProductService service;
  public ProductController(ProductService service){this.service=service;}
  @GetMapping public List<Product> list(@PathVariable UUID branchId,@RequestParam(defaultValue="0") int offset,@RequestParam(defaultValue="50") int limit) {return service.all(branchId,offset,limit);}
  @GetMapping("/{id}")public Product get(@PathVariable UUID branchId,@PathVariable UUID id){return service.one(branchId,id);}
  @PostMapping public ResponseEntity<Product> create(@PathVariable UUID branchId,@RequestBody ProductRequest request){return ResponseEntity.status(201).body(service.add(branchId,request));}
  @PatchMapping("/{id}")public Product patch(@PathVariable UUID branchId,@PathVariable UUID id,@RequestBody ProductRequest request){return service.change(branchId,id,request);}
  @DeleteMapping("/{id}")@ResponseStatus(HttpStatus.NO_CONTENT)public void delete(@PathVariable UUID branchId,@PathVariable UUID id){service.remove(branchId,id);}
}
