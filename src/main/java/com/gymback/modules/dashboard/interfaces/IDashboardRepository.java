package com.gymback.modules.dashboard.interfaces;
import java.time.LocalDate;
import java.util.*;
public interface IDashboardRepository {
 Map<String,Object> gym(UUID id);
 Map<String,Object> stats(UUID gym,LocalDate from,LocalDate to);
}
