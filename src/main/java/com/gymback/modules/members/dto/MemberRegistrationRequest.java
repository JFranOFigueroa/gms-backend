package com.gymback.modules.members.dto;
import com.gymback.modules.users.dto.UserCreateRequest;
import java.time.LocalDate;
import java.util.UUID;
public record MemberRegistrationRequest(UserCreateRequest user,MemberData member){
 public record MemberData(String code,LocalDate birthDate,String address,String emergencyContactName,
  String emergencyContactPhone,UUID photoAssetId,String notes,LocalDate startDate,Boolean active){}
}
