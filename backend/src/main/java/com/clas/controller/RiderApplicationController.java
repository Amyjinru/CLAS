package com.clas.controller;
import com.clas.common.Result;
import com.clas.config.RequireRole;
import com.clas.dto.RiderApplicationRequest;
import com.clas.dto.RiderApplicationResponse;
import com.clas.dto.RiderAuditRequest;
import com.clas.config.UserContext;
import java.util.List;
import com.clas.dto.RiderProfileResponse;
import com.clas.service.RiderApplicationService;
import com.clas.service.RiderLocationService;
import com.clas.service.RiderDispatchService;
import com.clas.service.RiderDeliveryService;
import com.clas.dto.RiderLocationRequest;
import com.clas.dto.RiderOnlineRequest;
import com.clas.dto.RiderTaskResponse;
import com.clas.dto.RiderSequenceRequest;
import com.clas.dto.RiderAdminUpdateRequest;
import com.clas.dto.RiderIdentityRevealResponse;
import com.clas.dto.ChatMessageRequest;
import com.clas.dto.ChatMessageResponse;
import com.clas.dto.DeliveryCallSessionResponse;
import com.clas.service.RiderContactService;
import com.clas.service.RiderWithdrawalService;
import com.clas.dto.RiderWithdrawalRequest;
import com.clas.dto.RiderWithdrawalAuditRequest;
import com.clas.entity.RiderWithdrawal;
import com.clas.entity.RiderReview;
import com.clas.service.RiderReviewService;
import com.clas.service.RiderInfoService;
import com.clas.entity.RiderDailyMetric;
import com.clas.entity.RiderSettlement;
import com.clas.mapper.RiderDailyMetricMapper;
import com.clas.mapper.RiderSettlementMapper;
import com.clas.dto.RiderInfoResponse;
import com.clas.dto.RiderInfoUpdateRequest;
import com.clas.dto.RiderPhoneChangeRequest;
import com.clas.dto.RiderPhoneChangeAuditRequest;
import com.clas.dto.RiderPhoneChangeResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clas.entity.Orders;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/rider")
public class RiderApplicationController {
    private final RiderApplicationService service;
    private final RiderLocationService locationService;
    private final RiderDispatchService dispatchService;
    private final RiderDeliveryService deliveryService;
    private final RiderContactService contactService;
    private final RiderWithdrawalService withdrawalService;
    private final RiderReviewService riderReviewService;
    private final RiderDailyMetricMapper metrics;
    private final RiderSettlementMapper settlements;
    private final RiderInfoService riderInfoService;
    public RiderApplicationController(RiderApplicationService service, RiderLocationService locationService, RiderDispatchService dispatchService, RiderDeliveryService deliveryService, RiderContactService contactService, RiderWithdrawalService withdrawalService, RiderReviewService riderReviewService, RiderDailyMetricMapper metrics, RiderSettlementMapper settlements, RiderInfoService riderInfoService) { this.service = service; this.locationService = locationService; this.dispatchService = dispatchService; this.deliveryService = deliveryService; this.contactService = contactService; this.withdrawalService = withdrawalService; this.riderReviewService = riderReviewService; this.metrics = metrics; this.settlements = settlements; this.riderInfoService = riderInfoService; }
    @PostMapping("/applications") @RequireRole({"USER", "MERCHANT", "RIDER"})
    public Result<RiderApplicationResponse> apply(@Valid @RequestBody RiderApplicationRequest request) { return Result.ok(service.apply(request)); }
    @GetMapping("/application") @RequireRole({"USER", "MERCHANT", "RIDER"})
    public Result<RiderApplicationResponse> mine() { return Result.ok(service.mine()); }
    @GetMapping("/profile") @RequireRole("RIDER")
    public Result<RiderProfileResponse> profile() { return Result.ok(service.profile()); }
    @GetMapping("/info") @RequireRole("RIDER")
    public Result<RiderInfoResponse> info() { return Result.ok(riderInfoService.mine()); }
    @PutMapping("/info") @RequireRole("RIDER")
    public Result<RiderInfoResponse> updateInfo(@Valid @RequestBody RiderInfoUpdateRequest request) { return Result.ok(riderInfoService.updateMine(request)); }
    @PostMapping("/info/service-phone-change") @RequireRole("RIDER")
    public Result<RiderPhoneChangeResponse> requestServicePhoneChange(@Valid @RequestBody RiderPhoneChangeRequest request) { return Result.ok(riderInfoService.requestServicePhoneChange(request)); }
    @PatchMapping("/online") @RequireRole("RIDER")
    public Result<RiderProfileResponse> online(@Valid @RequestBody RiderOnlineRequest request) { return Result.ok(locationService.setOnline(request.online())); }
    @PostMapping("/work/start") @RequireRole("RIDER")
    public Result<RiderProfileResponse> startWork() { return Result.ok(locationService.setAcceptingOrders(true)); }
    @PostMapping("/work/end") @RequireRole("RIDER")
    public Result<RiderProfileResponse> endWork() { return Result.ok(locationService.setAcceptingOrders(false)); }
    @PutMapping("/location") @RequireRole("RIDER")
    public Result<RiderProfileResponse> location(@Valid @RequestBody RiderLocationRequest request) { return Result.ok(locationService.reportLocation(request)); }
    @GetMapping("/tasks") @RequireRole("RIDER")
    public Result<List<RiderTaskResponse>> tasks(@RequestParam(defaultValue = "SMART") String sort) { return Result.ok(dispatchService.nearbyTasks(sort)); }
    @PostMapping("/tasks/{orderId}/claim") @RequireRole("RIDER")
    public Result<Orders> claim(@PathVariable Long orderId) { return Result.ok(dispatchService.claim(orderId)); }
    @GetMapping("/deliveries") @RequireRole("RIDER")
    public Result<List<Orders>> deliveries() { return Result.ok(dispatchService.activeDeliveries()); }
    @PutMapping("/deliveries/sequence") @RequireRole("RIDER")
    public Result<List<Orders>> sequence(@Valid @RequestBody RiderSequenceRequest request) { return Result.ok(dispatchService.reorder(request)); }
    @PostMapping("/deliveries/{orderId}/pickup") @RequireRole("RIDER")
    public Result<Orders> pickup(@PathVariable Long orderId) { return Result.ok(deliveryService.pickup(orderId)); }
    @PostMapping("/deliveries/{orderId}/complete") @RequireRole("RIDER")
    public Result<Orders> deliver(@PathVariable Long orderId) { return Result.ok(deliveryService.deliver(orderId)); }
    @PostMapping("/deliveries/{orderId}/abandon") @RequireRole("RIDER")
    public Result<Orders> abandon(@PathVariable Long orderId, @RequestParam String reason) { return Result.ok(deliveryService.abandonBeforePickup(orderId, reason)); }
    @GetMapping("/deliveries/{orderId}/messages") @RequireRole({"USER", "RIDER"})
    public Result<List<ChatMessageResponse>> riderMessages(@PathVariable Long orderId) { return Result.ok(contactService.messages(orderId, UserContext.getUserId(), UserContext.getRole())); }
    @PostMapping("/deliveries/{orderId}/messages") @RequireRole({"USER", "RIDER"})
    public Result<ChatMessageResponse> sendRiderMessage(@PathVariable Long orderId, @Valid @RequestBody ChatMessageRequest request) { return Result.ok(contactService.send(orderId, UserContext.getUserId(), UserContext.getRole(), request.content())); }
    @PostMapping("/deliveries/{orderId}/call-session") @RequireRole("RIDER")
    public Result<DeliveryCallSessionResponse> call(@PathVariable Long orderId) { return Result.ok(contactService.createCall(orderId, UserContext.getUserId())); }
    @PostMapping("/withdrawals") @RequireRole("RIDER") public Result<RiderWithdrawal> withdraw(@Valid @RequestBody RiderWithdrawalRequest request) { return Result.ok(withdrawalService.apply(UserContext.getUserId(),request.bankCardId(),request.amount())); }
    @GetMapping("/withdrawals") @RequireRole("RIDER") public Result<List<RiderWithdrawal>> withdrawals() { return Result.ok(withdrawalService.mine(UserContext.getUserId())); }
    @GetMapping("/reviews") @RequireRole("RIDER") public Result<List<RiderReview>> reviews() { return Result.ok(riderReviewService.mine(UserContext.getUserId())); }
    @GetMapping("/metrics") @RequireRole("RIDER") public Result<List<RiderDailyMetric>> metrics() { return Result.ok(metrics.selectList(new LambdaQueryWrapper<RiderDailyMetric>().eq(RiderDailyMetric::getRiderId, UserContext.getUserId()).orderByDesc(RiderDailyMetric::getMetricDate))); }
    @GetMapping("/settlements") @RequireRole("RIDER") public Result<List<RiderSettlement>> settlements() { return Result.ok(settlements.selectList(new LambdaQueryWrapper<RiderSettlement>().eq(RiderSettlement::getRiderId, UserContext.getUserId()).orderByDesc(RiderSettlement::getCreatedAt))); }
    @GetMapping("/admin/withdrawals") @RequireRole("ADMIN") public Result<List<RiderWithdrawal>> pendingWithdrawals() { return Result.ok(withdrawalService.pending()); }
    @PatchMapping("/admin/withdrawals/{id}") @RequireRole("ADMIN") public Result<RiderWithdrawal> auditWithdrawal(@PathVariable Long id,@Valid @RequestBody RiderWithdrawalAuditRequest request) { return Result.ok(withdrawalService.audit(id,request.approved(),request.reason(),UserContext.getUserId())); }
    @GetMapping("/admin/applications") @RequireRole("ADMIN")
    public Result<List<RiderApplicationResponse>> pending() { return Result.ok(service.pending()); }
    @GetMapping("/admin/info-change-requests") @RequireRole("ADMIN")
    public Result<List<RiderPhoneChangeResponse>> pendingInfoChanges() { return Result.ok(riderInfoService.pendingChanges()); }
    @PatchMapping("/admin/info-change-requests/{id}") @RequireRole("ADMIN")
    public Result<RiderPhoneChangeResponse> auditInfoChange(@PathVariable Long id, @Valid @RequestBody RiderPhoneChangeAuditRequest request) { return Result.ok(riderInfoService.auditChange(id, request, UserContext.getUserId())); }
    @PatchMapping("/admin/applications/{id}") @RequireRole("ADMIN")
    public Result<RiderApplicationResponse> audit(@PathVariable Long id, @Valid @RequestBody RiderAuditRequest request) { return Result.ok(service.audit(id, request.decision(), request.reason(), request.maxActiveOrders(), UserContext.getUserId())); }
    @GetMapping("/admin/riders/{riderId}") @RequireRole("ADMIN")
    public Result<RiderProfileResponse> adminProfile(@PathVariable String riderId) { return Result.ok(service.adminProfile(riderId)); }
    @PatchMapping("/admin/riders/{riderId}") @RequireRole("ADMIN")
    public Result<RiderProfileResponse> adminUpdate(@PathVariable String riderId, @Valid @RequestBody RiderAdminUpdateRequest request) { return Result.ok(service.adminUpdate(riderId, request, UserContext.getUserId())); }
    @GetMapping("/admin/riders/{riderId}/identity") @RequireRole("ADMIN")
    public Result<RiderIdentityRevealResponse> reveal(@PathVariable String riderId, @RequestParam String purpose) { return Result.ok(service.revealIdentity(riderId, purpose, UserContext.getUserId())); }
}
