package com.clas.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clas.common.BusinessException;
import com.clas.common.MerchantStatusEnum;
import com.clas.common.PhoneValidator;
import com.clas.config.UserContext;
import com.clas.dto.BookingRequest;
import com.clas.entity.Merchant;
import com.clas.entity.ServiceBooking;
import com.clas.mapper.MerchantMapper;
import com.clas.mapper.ServiceBookingMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_CONFIRMED = "CONFIRMED";
    public static final String STATUS_CANCELED = "CANCELED";
    public static final String STATUS_COMPLETED = "COMPLETED";

    private static final Set<String> MERCHANT_STATUSES = Set.of(
        STATUS_PENDING,
        STATUS_CONFIRMED,
        STATUS_CANCELED,
        STATUS_COMPLETED
    );

    private final ServiceBookingMapper bookingMapper;
    private final MerchantMapper merchantMapper;
    private final MerchantService merchantService;
    private final NotificationService notificationService;
    private final PenaltyService penaltyService;

    public BookingService(
        ServiceBookingMapper bookingMapper,
        MerchantMapper merchantMapper,
        MerchantService merchantService,
        NotificationService notificationService,
        PenaltyService penaltyService
    ) {
        this.bookingMapper = bookingMapper;
        this.merchantMapper = merchantMapper;
        this.merchantService = merchantService;
        this.notificationService = notificationService;
        this.penaltyService = penaltyService;
    }

    @Transactional
    public ServiceBooking create(BookingRequest request) {
        penaltyService.assertCanUsePlatform(UserContext.getUserId());
        if (request.merchantId() == null) {
            throw new BusinessException("请选择预约商家");
        }
        Merchant merchant = merchantMapper.selectById(request.merchantId());
        if (merchant == null || merchant.getStatus() != MerchantStatusEnum.OPEN) {
            throw new BusinessException("商家不存在或未营业");
        }
        String serviceName = normalizeText(request.serviceName(), "请填写预约服务", 100);
        LocalDateTime appointmentTime = request.appointmentTime();
        if (appointmentTime == null || appointmentTime.isBefore(LocalDateTime.now().plusMinutes(10))) {
            throw new BusinessException("预约时间至少需要晚于当前时间 10 分钟");
        }

        ServiceBooking booking = new ServiceBooking();
        booking.setUserId(UserContext.getUserId());
        booking.setMerchantId(request.merchantId());
        booking.setServiceName(serviceName);
        booking.setAppointmentTime(appointmentTime);
        booking.setContactPhone(PhoneValidator.normalizeAndValidate(request.contactPhone()));
        booking.setNote(truncate(optionalText(request.note()), 255));
        booking.setStatus(STATUS_PENDING);
        bookingMapper.insert(booking);

        notificationService.send(new NotificationService.NotificationTarget(
            booking.getUserId(),
            "预约已提交",
            merchant.getMerchantName() + " 已收到你的预约申请，请等待确认",
            "BOOKING_STATUS",
            "BOOKING",
            booking.getId(),
            null,
            null,
            null,
            booking.getMerchantId(),
            "/bookings?bookingId=" + booking.getId()
        ));
        if (merchant.getUserId() != null) {
            notificationService.send(new NotificationService.NotificationTarget(
                merchant.getUserId(),
                "新的预约申请",
                booking.getServiceName() + " 预约待处理",
                "BOOKING_STATUS",
                "BOOKING",
                booking.getId(),
                null,
                null,
                null,
                booking.getMerchantId(),
                "/merchant/bookings?bookingId=" + booking.getId()
            ));
        }
        return booking;
    }

    public List<ServiceBooking> mine() {
        return bookingMapper.selectList(new LambdaQueryWrapper<ServiceBooking>()
            .eq(ServiceBooking::getUserId, UserContext.getUserId())
            .orderByDesc(ServiceBooking::getId));
    }

    public List<ServiceBooking> merchantMine() {
        Long merchantId = merchantService.getCurrentMerchantId();
        return bookingMapper.selectList(new LambdaQueryWrapper<ServiceBooking>()
            .eq(ServiceBooking::getMerchantId, merchantId)
            .orderByDesc(ServiceBooking::getAppointmentTime));
    }

    @Transactional
    public ServiceBooking cancelMine(Long id) {
        ServiceBooking booking = bookingMapper.selectById(id);
        if (booking == null || !UserContext.getUserId().equals(booking.getUserId())) {
            throw new BusinessException("预约不存在或无权操作");
        }
        if (STATUS_COMPLETED.equals(booking.getStatus())) {
            throw new BusinessException("已完成的预约不能取消");
        }
        booking.setStatus(STATUS_CANCELED);
        bookingMapper.updateById(booking);
        Merchant merchant = merchantMapper.selectById(booking.getMerchantId());
        if (merchant != null && merchant.getUserId() != null) {
            notificationService.send(new NotificationService.NotificationTarget(
                merchant.getUserId(),
                "预约已取消",
                booking.getServiceName() + " 预约已由用户取消",
                "BOOKING_STATUS",
                "BOOKING",
                booking.getId(),
                null,
                null,
                null,
                booking.getMerchantId(),
                "/merchant/bookings?bookingId=" + booking.getId()
            ));
        }
        return booking;
    }

    @Transactional
    public ServiceBooking updateStatus(Long id, String status) {
        String normalizedStatus = normalizeStatus(status);
        ServiceBooking booking = bookingMapper.selectById(id);
        Long merchantId = merchantService.getCurrentMerchantId();
        if (booking == null || !merchantId.equals(booking.getMerchantId())) {
            throw new BusinessException("预约不存在或无权操作");
        }
        booking.setStatus(normalizedStatus);
        bookingMapper.updateById(booking);
        notificationService.send(new NotificationService.NotificationTarget(
            booking.getUserId(),
            "预约状态更新",
            booking.getServiceName() + " 当前状态：" + normalizedStatus,
            "BOOKING_STATUS",
            "BOOKING",
            booking.getId(),
            null,
            null,
            null,
            booking.getMerchantId(),
            "/bookings?bookingId=" + booking.getId()
        ));
        return booking;
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new BusinessException("请选择预约状态");
        }
        String normalized = status.trim().toUpperCase();
        if (!MERCHANT_STATUSES.contains(normalized)) {
            throw new BusinessException("预约状态不合法");
        }
        return normalized;
    }

    private String normalizeText(String value, String message, int maxLength) {
        String normalized = optionalText(value);
        if (normalized == null) {
            throw new BusinessException(message);
        }
        return truncate(normalized, maxLength);
    }

    private String optionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
