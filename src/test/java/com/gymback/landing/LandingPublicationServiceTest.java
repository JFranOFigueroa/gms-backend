package com.gymback.landing;
import com.gymback.modules.landing.interfaces.ILandingPublicationRepository;
import com.gymback.modules.landing.services.LandingPublicationService;
import com.gymback.modules.gyms.interfaces.IGymRepository;
import com.gymback.security.*;
import com.gymback.shared.exceptions.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
/** La landing se sirve desde sus bloques guardados, sin publicar/despublicar. */
class LandingPublicationServiceTest {
 private final ILandingPublicationRepository repo=mock(ILandingPublicationRepository.class);
 private final IGymRepository gyms=mock(IGymRepository.class);
 private final AuthorizationService auth=mock(AuthorizationService.class);
 private final com.gymback.modules.media.interfaces.IMediaAssetRepository media=mock(com.gymback.modules.media.interfaces.IMediaAssetRepository.class);
 private final UUID gym=UUID.randomUUID(),owner=UUID.randomUUID();
 private LandingPublicationService service(){return new LandingPublicationService(repo,gyms,auth,media,"https://gimnasios.example");}
 private ILandingPublicationRepository.State state(){
   return new ILandingPublicationRepository.State(gym,"Iron Fitness","iron-fitness","active");
 }
 @Test void linkIsAvailableWithoutPublication(){
   when(auth.require(gym,"owner")).thenReturn(new Actor(owner,false));
   when(repo.state(gym,false)).thenReturn(Optional.of(state()));
   assertEquals("https://gimnasios.example/g/iron-fitness",service().link(gym).get("publicUrl"));
   verify(repo,never()).draft(any());
 }
 @Test void changeSlugDoesNotDependOnPublication(){
   when(auth.require(gym,"owner")).thenReturn(new Actor(owner,false));
   when(repo.state(gym,true)).thenReturn(Optional.of(state()));
   var result=service().changeSlug(gym,"Gimnasio Élite");
   assertEquals("gimnasio-elite",result.get("slug"));
   assertEquals("https://gimnasios.example/g/gimnasio-elite",result.get("publicUrl"));
   verify(repo).slug(gym,"gimnasio-elite");
 }
 @Test void rejectSlugAlreadyInUse(){
   when(auth.require(gym,"owner")).thenReturn(new Actor(owner,false));
   when(repo.state(gym,true)).thenReturn(Optional.of(state()));
   when(gyms.slugExists("iron-fitness",gym)).thenReturn(true);
   assertThrows(ConflictException.class,()->service().changeSlug(gym,"Iron Fitness"));
   verify(repo,never()).slug(any(),anyString());
 }
 @Test void logoMustUseAnExistingLocalImageInSameGym(){
   UUID assetId=UUID.randomUUID();
   when(media.get(assetId,gym)).thenReturn(Optional.empty());
   assertThrows(NotFoundException.class,()->service().changeLogo(gym,assetId));
   var asset=new com.gymback.modules.media.model.MediaAsset();
   asset.setMediaType("image");asset.setStorageKey(gym+"/"+UUID.randomUUID()+".png");
   when(media.get(assetId,gym)).thenReturn(Optional.of(asset));
   var result=service().changeLogo(gym,assetId);
   assertEquals("/api/v1/public/media/"+assetId,result.get("logoUrl"));
   verify(repo).logo(gym,assetId);
 }
 @Test void publicReadingIsAnonymousAndReadsCurrentPage(){
   var page=Map.<String,Object>of("gym",Map.of("name","Iron Fitness"),"blocks",List.of(Map.of("type","hero")));
   when(repo.publicPage("iron-fitness")).thenReturn(Optional.of(page));
   assertEquals(page,service().publicLanding("iron-fitness"));
   verifyNoInteractions(auth);
   when(repo.publicPage("not-found")).thenReturn(Optional.empty());
   assertThrows(NotFoundException.class,()->service().publicLanding("not-found"));
 }
}
