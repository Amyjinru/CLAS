package com.clas.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.clas.entity.ChatMessage;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    @Select("SELECT * FROM chat_message WHERE order_id = #{orderId} ORDER BY created_at ASC")
    List<ChatMessage> selectByOrderId(Long orderId);

    @Select("SELECT * FROM chat_message WHERE merchant_id = #{merchantId} AND user_id = #{userId} ORDER BY created_at ASC")
    List<ChatMessage> selectByMerchantAndUser(Long merchantId, String userId);
}
