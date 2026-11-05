package com.gymback.modules.pos.dto;
import java.util.*;
public record CheckoutRequest(List<Line> items) {
  public record Line(UUID productId,int quantity) {}
}
