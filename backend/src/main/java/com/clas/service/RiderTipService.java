package com.clas.service;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clas.common.BusinessException;
import com.clas.entity.Orders;
import com.clas.entity.RiderSettlement;
import com.clas.entity.RiderTip;
import com.clas.mapper.RiderSettlementMapper;
import com.clas.mapper.RiderTipMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
@Service public class RiderTipService {
 private final RiderTipMapper tips; private final RiderSettlementMapper settlements; private final NotificationService notifications;
 public RiderTipService(RiderTipMapper tips,RiderSettlementMapper settlements,NotificationService notifications){this.tips=tips;this.settlements=settlements;this.notifications=notifications;}
 @Transactional public RiderTip pay(Orders o,String user,Integer amount,String key){
  if(!"DELIVERED".equals(o.getDeliveryStatus())&&!"COMPLETED".equals(o.getStatus()))throw new BusinessException("订单送达后才可打赏"); if(o.getRiderId()==null)throw new BusinessException("订单暂无骑手");
  RiderTip old=tips.selectOne(new LambdaQueryWrapper<RiderTip>().eq(RiderTip::getUserId,user).eq(RiderTip::getIdempotencyKey,key)); if(old!=null)return old;
  if(tips.selectOne(new LambdaQueryWrapper<RiderTip>().eq(RiderTip::getOrderId,o.getId()))!=null)throw new BusinessException("每个订单只能打赏一次");
  RiderTip tip=new RiderTip();tip.setOrderId(o.getId());tip.setUserId(user);tip.setRiderId(o.getRiderId());tip.setAmount(amount);tip.setIdempotencyKey(key);tip.setStatus("PAID");tip.setPaidAt(LocalDateTime.now());tips.insert(tip);
  RiderSettlement s=new RiderSettlement();s.setRiderId(o.getRiderId());s.setOrderId(o.getId());s.setSourceType("TIP");s.setSourceId(String.valueOf(tip.getId()));s.setSettlementType("TIP");s.setAmount(amount);s.setBalanceType("PENDING");s.setCreatedAt(LocalDateTime.now());settlements.insert(s);notifications.send(new NotificationService.NotificationTarget(o.getRiderId(),"收到用户打赏","订单 " + o.getId() + " 收到一笔打赏，确认收货后可提现。","RIDER_TIP","ORDER",o.getId(),null,null,o.getId(),o.getMerchantId(),"/rider-workbench"));return tip;
 }
}
