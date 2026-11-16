package com.gymback.shared.factories;
import org.springframework.context.annotation.*;
import com.gymback.shared.services.AuditService;
import com.gymback.security.AuthorizationService;
@Configuration
public class ServiceFactory {
  @Bean public com.gymback.modules.branches.services.BranchService branchService(
    com.gymback.modules.branches.interfaces.IBranchRepository repo,AuditService audit,AuthorizationService auth){
      return new com.gymback.modules.branches.services.BranchService(repo,audit,auth);
  }
  @Bean public com.gymback.modules.plans.services.MembershipPlanService membershipPlanService(
    com.gymback.modules.plans.interfaces.IMembershipPlanRepository repo,AuditService audit,AuthorizationService auth){
      return new com.gymback.modules.plans.services.MembershipPlanService(repo,audit,auth);
  }
  @Bean public com.gymback.modules.calendar.services.ClassTemplateService classTemplateService(
    com.gymback.modules.calendar.interfaces.IClassTemplateRepository repo,AuditService audit,AuthorizationService auth){
      return new com.gymback.modules.calendar.services.ClassTemplateService(repo,audit,auth);
  }
  @Bean public com.gymback.modules.calendar.services.BranchClassService branchClassService(
    com.gymback.modules.calendar.interfaces.IBranchClassRepository repo,AuditService audit,AuthorizationService auth){
      return new com.gymback.modules.calendar.services.BranchClassService(repo,audit,auth);
  }
  @Bean public com.gymback.modules.routines.services.RoutineService routineService(
    com.gymback.modules.routines.interfaces.IRoutineRepository repo,AuditService audit,AuthorizationService auth,com.gymback.modules.routines.interfaces.IRoutineStructureRepository structure){
      return new com.gymback.modules.routines.services.RoutineService(repo,audit,auth,structure);
  }
  @Bean public com.gymback.modules.pos.services.ProductService productService(
    com.gymback.modules.pos.interfaces.IProductRepository repo,AuditService audit,AuthorizationService auth){
      return new com.gymback.modules.pos.services.ProductService(repo,audit,auth);
  }
  @Bean public com.gymback.modules.site.services.SiteSettingsService siteSettingsService(
    com.gymback.modules.site.repositories.SiteSettingsRepository repo,
    com.gymback.modules.site.repositories.SiteMediaAssetRepository media,
    AuditService audit, AuthorizationService auth){
      return new com.gymback.modules.site.services.SiteSettingsService(repo,media,audit,auth);
  }
  @Bean public com.gymback.modules.site.services.SiteLandingBlockService siteLandingBlockService(
    com.gymback.modules.site.repositories.SiteLandingBlockRepository repo,
    AuditService audit, AuthorizationService auth){
      return new com.gymback.modules.site.services.SiteLandingBlockService(repo,audit,auth);
  }
  @Bean public com.gymback.modules.site.services.SiteMediaService siteMediaService(
    com.gymback.modules.site.repositories.SiteMediaAssetRepository repo,
    AuditService audit, AuthorizationService auth,
    com.gymback.modules.files.services.LocalImageStorage files){
      return new com.gymback.modules.site.services.SiteMediaService(repo,audit,auth,files);
  }
  @Bean public com.gymback.modules.site.services.PublicSiteService publicSiteService(
    com.gymback.modules.site.repositories.SiteSettingsRepository settings,
    com.gymback.modules.site.repositories.SiteLandingBlockRepository blocks,
    com.gymback.modules.site.repositories.SiteMediaAssetRepository media,
    com.gymback.modules.branches.interfaces.IBranchRepository branches){
      return new com.gymback.modules.site.services.PublicSiteService(settings,blocks,media,branches);
  }

 @Bean public com.gymback.modules.members.services.MemberService memberService(
   com.gymback.modules.members.interfaces.IMemberRepository repo,AuditService audit,
   AuthorizationService auth,com.gymback.security.IIdentityRepository accounts,
   com.gymback.modules.members.repositories.MemberPhotoRepository photos,
   com.gymback.modules.files.services.LocalImageStorage files){
     return new com.gymback.modules.members.services.MemberService(repo,audit,auth,accounts,photos,files);
 }
 @Bean public com.gymback.modules.staff.services.StaffService staffService(AuthorizationService auth,com.gymback.security.IIdentityRepository accounts,com.gymback.modules.staff.interfaces.IStaffRepository repo){return new com.gymback.modules.staff.services.StaffService(auth,accounts,repo);}

 @Bean public com.gymback.modules.payments.services.PaymentService paymentService(com.gymback.modules.payments.interfaces.IPaymentRepository repo,AuthorizationService auth,com.gymback.shared.services.IdempotencyService idempotency){return new com.gymback.modules.payments.services.PaymentService(repo,auth,idempotency);}
 @Bean public com.gymback.modules.pos.services.SaleService saleService(com.gymback.modules.pos.interfaces.ISaleRepository repo,AuthorizationService auth,com.gymback.shared.services.IdempotencyService idempotency){return new com.gymback.modules.pos.services.SaleService(repo,auth,idempotency);}
 @Bean public com.gymback.modules.checkin.services.MemberAccessPolicy memberAccessPolicy(com.gymback.modules.checkin.interfaces.ICheckinRepository repo){
  return new com.gymback.modules.checkin.services.MemberAccessPolicy(repo);
 }
 @Bean public com.gymback.modules.checkin.services.CheckinQrService checkinQrService(
  com.gymback.modules.checkin.interfaces.ICheckinQrTokenRepository tokens,
  com.gymback.modules.checkin.interfaces.ICheckinDeviceRepository devices,
  com.gymback.modules.checkin.interfaces.ICheckinContextRepository context,
  com.gymback.modules.checkin.services.CheckinCrypto crypto){
  return new com.gymback.modules.checkin.services.CheckinQrService(tokens,devices,context,crypto);
 }
 @Bean public com.gymback.modules.checkin.services.KioskDeviceService kioskDeviceService(
  com.gymback.modules.checkin.interfaces.ICheckinDeviceRepository devices,
  com.gymback.modules.checkin.interfaces.ICheckinDeviceSessionRepository sessions,
  com.gymback.modules.checkin.interfaces.ICheckinQrTokenRepository tokens,
  com.gymback.modules.checkin.interfaces.ICheckinContextRepository context,
  com.gymback.security.IIdentityRepository identity,AuthorizationService auth,
  com.gymback.modules.checkin.services.CheckinCrypto crypto,
  com.gymback.modules.checkin.services.ActivationAbuseService abuse){
  return new com.gymback.modules.checkin.services.KioskDeviceService(devices,sessions,tokens,context,identity,auth,crypto,abuse);
 }
 @Bean public com.gymback.modules.checkin.services.CheckinService checkinService(
  com.gymback.modules.checkin.interfaces.ICheckinRepository repo,AuthorizationService auth,
  com.gymback.modules.checkin.services.MemberAccessPolicy policy,
  com.gymback.modules.checkin.services.CheckinQrService qr,
  com.gymback.modules.checkin.services.CheckinCrypto crypto,
  com.gymback.modules.checkin.interfaces.ICheckinContextRepository context,
  com.gymback.modules.checkin.interfaces.ICheckinAccessCodeRepository accessCodes){
  return new com.gymback.modules.checkin.services.CheckinService(repo,auth,policy,qr,crypto,context,accessCodes);
 }
 @Bean public com.gymback.modules.settings.services.SettingsService settingsService(com.gymback.modules.settings.interfaces.ISettingsRepository repo,com.gymback.modules.site.repositories.SiteSettingsRepository siteSettings,AuthorizationService auth){return new com.gymback.modules.settings.services.SettingsService(repo,siteSettings,auth);}

 @Bean public com.gymback.modules.calendar.services.ReservationService reservationService(com.gymback.modules.calendar.interfaces.IReservationRepository repo,AuthorizationService auth){return new com.gymback.modules.calendar.services.ReservationService(repo,auth);}
 // Nuevos casos de uso integrados mediante la misma factoría que los services existentes.
 @Bean public com.gymback.modules.users.services.UserService userService(
   com.gymback.modules.users.interfaces.IUserRepository repo,AuthorizationService auth,
   org.springframework.security.crypto.password.PasswordEncoder encoder,
   @org.springframework.beans.factory.annotation.Value("${app.user-api-enabled:false}")boolean enabled){
     return new com.gymback.modules.users.services.UserService(repo,auth,encoder,enabled);
 }
 @Bean public com.gymback.modules.members.services.MemberRegistrationService memberRegistrationService(
   AuthorizationService auth,com.gymback.modules.users.services.UserService users,
   com.gymback.modules.members.services.MemberService members){
     return new com.gymback.modules.members.services.MemberRegistrationService(auth,users,members);
 }
 @Bean public com.gymback.modules.staff.services.StaffRegistrationService staffRegistrationService(
   AuthorizationService auth,com.gymback.modules.users.services.UserService users,
   com.gymback.modules.staff.services.StaffService staff){
     return new com.gymback.modules.staff.services.StaffRegistrationService(auth,users,staff);
 }
 @Bean public com.gymback.modules.audit.services.AuditEventService auditEventService(
   com.gymback.modules.audit.interfaces.IAuditEventRepository repo,AuthorizationService auth){
     return new com.gymback.modules.audit.services.AuditEventService(repo,auth);
 }
 @Bean public com.gymback.modules.dashboard.services.DashboardService dashboardService(
   com.gymback.modules.dashboard.interfaces.IDashboardRepository repo,AuthorizationService auth){
     return new com.gymback.modules.dashboard.services.DashboardService(repo,auth);
 }

}
