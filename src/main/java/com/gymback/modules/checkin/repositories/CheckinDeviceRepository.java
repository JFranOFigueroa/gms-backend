package com.gymback.modules.checkin.repositories;
import com.gymback.modules.checkin.model.CheckinDevice;
import com.gymback.modules.checkin.interfaces.ICheckinDeviceRepository;
import com.gymback.shared.repositories.BaseCRUD;
import jakarta.persistence.*;
import org.springframework.stereotype.Repository;
import java.util.*;
@Repository
public class CheckinDeviceRepository extends BaseCRUD<CheckinDevice> implements ICheckinDeviceRepository {
 public CheckinDeviceRepository(EntityManager em){super(em,CheckinDevice.class,true);}
 public List<CheckinDevice> list(UUID gym){return super.list(gym,0,100);}
 public Optional<CheckinDevice> findInGym(UUID gym,UUID id){return super.get(id,gym);}
 public Optional<CheckinDevice> lockInGym(UUID gym,UUID id){
  CheckinDevice d=em.find(CheckinDevice.class,id,LockModeType.PESSIMISTIC_WRITE);
  return d==null||!gym.equals(d.gymId)?Optional.empty():Optional.of(d);
 }
 public Optional<CheckinDevice> byActivationHash(String hash,boolean lock){
  var q=em.createQuery("select d from CheckinDevice d where d.deviceSecretHash=:hash",CheckinDevice.class)
   .setParameter("hash",hash).setMaxResults(1);
  if(lock)q.setLockMode(LockModeType.PESSIMISTIC_WRITE);
  return q.getResultStream().findFirst();
 }
 public Optional<CheckinDevice> byName(UUID gym,String name){
  return em.createQuery("select d from CheckinDevice d where d.gymId=:gym and lower(d.deviceName)=lower(:name)",CheckinDevice.class)
    .setParameter("gym",gym).setParameter("name",name).getResultStream().findFirst();
 }
 public CheckinDevice save(CheckinDevice d){return em.contains(d)?update(d):create(d);}
}
