package com.gymback.modules.staff.dto;
import com.gymback.modules.users.dto.UserCreateRequest;
public record StaffRegistrationRequest(UserCreateRequest user,String role){}
