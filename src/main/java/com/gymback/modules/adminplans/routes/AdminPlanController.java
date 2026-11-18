package com.gymback.modules.adminplans.routes;

import com.gymback.modules.adminplans.dto.AdminPlanRequest;
import com.gymback.modules.adminplans.dto.AdminPlanResponse;
import com.gymback.modules.adminplans.services.AdminPlanService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/branches/{branchId}/admin-plans")
public class AdminPlanController {
  private final AdminPlanService service;

  public AdminPlanController(AdminPlanService service) { this.service = service; }

  @GetMapping
  public List<AdminPlanResponse> list(@PathVariable UUID branchId,
      @RequestParam(defaultValue = "0") int offset,
      @RequestParam(defaultValue = "50") int limit) {
    return service.all(branchId, offset, limit).stream().map(AdminPlanResponse::from).toList();
  }

  @GetMapping("/{id}")
  public AdminPlanResponse get(@PathVariable UUID branchId, @PathVariable UUID id) {
    return AdminPlanResponse.from(service.one(branchId, id));
  }

  @PostMapping
  public ResponseEntity<AdminPlanResponse> create(@PathVariable UUID branchId, @RequestBody AdminPlanRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(AdminPlanResponse.from(service.add(branchId, request)));
  }

  @PatchMapping("/{id}")
  public AdminPlanResponse patch(@PathVariable UUID branchId, @PathVariable UUID id,
      @RequestBody AdminPlanRequest request) {
    return AdminPlanResponse.from(service.change(branchId, id, request));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID branchId, @PathVariable UUID id) {
    service.remove(branchId, id);
  }
}
