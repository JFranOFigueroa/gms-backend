package com.gymback.modules.landing.interfaces;
import java.util.*;
/** Consulta de página por gimnasio, sin estados de publicar/despublicar. */
public interface ILandingPublicationRepository {
  record State(UUID gymId, String gymName, String slug, String status) {}
  Optional<State> state(UUID gymId, boolean lock);
  Map<String,Object> draft(UUID gymId);
  void slug(UUID gymId,String slug);
  void logo(UUID gymId,UUID assetId);
  Optional<Map<String,Object>> publicPage(String slug);
}
