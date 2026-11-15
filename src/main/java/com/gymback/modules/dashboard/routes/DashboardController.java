package com.gymback.modules.dashboard.routes;
import com.gymback.modules.dashboard.services.DashboardService;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.*;
@RestController @RequestMapping("/api/v1/branches/{branchId}/dashboard")
public class DashboardController {
 private final DashboardService s;public DashboardController(DashboardService s){this.s=s;}
 @GetMapping("/summary")public Map<String,Object> summary(@PathVariable UUID branchId,
   @RequestParam(required=false)LocalDate from,@RequestParam(required=false)LocalDate to){return s.summary(branchId,from,to);}
}
