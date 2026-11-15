package com.gymback.modules.dashboard.interfaces;
import java.time.LocalDate;
import java.util.*;
public interface IDashboardRepository {
 Map<String,Object> branch(UUID id);
 Map<String,Object> stats(UUID branch,LocalDate from,LocalDate to);
}
