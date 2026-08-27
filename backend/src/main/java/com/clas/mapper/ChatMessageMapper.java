package com.clas.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.clas.entity.ChatMessage;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    @Select("SELECT * FROM chat_message WHERE order_id = #{orderId} ORDER BY created_at ASC")
    List<ChatMessage> selectByOrderId(@Param("orderId") Long orderId);

    @Select("SELECT * FROM chat_message WHERE order_id = #{orderId} AND conversation_type = 'USER_RIDER' ORDER BY created_at ASC")
    List<ChatMessage> selectUserRiderByOrderId(@Param("orderId") Long orderId);

    @Select("SELECT cm.* FROM chat_message cm " +
        "INNER JOIN (SELECT merchant_id, MAX(created_at) AS max_time FROM chat_message " +
        "WHERE user_id = #{userId} GROUP BY merchant_id) latest " +
        "ON cm.merchant_id = latest.merchant_id AND cm.created_at = latest.max_time " +
        "WHERE cm.user_id = #{userId} ORDER BY cm.created_at DESC")
    List<ChatMessage> selectLatestByUserGroupedByMerchant(@Param("userId") String userId);

    @Select("SELECT cm.* FROM chat_message cm " +
        "INNER JOIN (SELECT user_id, MAX(created_at) AS max_time FROM chat_message " +
        "WHERE merchant_id = #{merchantId} GROUP BY user_id) latest " +
        "ON cm.user_id = latest.user_id AND cm.created_at = latest.max_time " +
        "WHERE cm.merchant_id = #{merchantId} ORDER BY cm.created_at DESC")
    List<ChatMessage> selectLatestByMerchantGroupedByUser(@Param("merchantId") Long merchantId);

    @Select("SELECT * FROM chat_message WHERE merchant_id = #{merchantId} AND user_id = #{userId} ORDER BY created_at ASC")
    List<ChatMessage> selectByMerchantAndUser(@Param("merchantId") Long merchantId, @Param("userId") String userId);

    @Select("SELECT DISTINCT merchant_id FROM chat_message ORDER BY merchant_id")
    List<Long> selectDistinctMerchantIds();

    @Select("SELECT DISTINCT user_id FROM chat_message WHERE merchant_id = #{merchantId} ORDER BY user_id")
    List<String> selectDistinctUserIdsByMerchant(@Param("merchantId") Long merchantId);
}
