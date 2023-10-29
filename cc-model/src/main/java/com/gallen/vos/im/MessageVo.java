package com.gallen.vos.im;



import com.gallen.common.annotation.IdEncrypt;
import com.gallen.vos.user.SafetyUser;
import lombok.Data;

import java.util.Date;

@Data
public class MessageVo{
    @IdEncrypt
    private Long id;
    private String message;

    /**
     * 消息状态：0(未读)、1(已读)
     */
    private Integer state;

    private Date sendDate;
    private Date readDate;
    private SafetyUser fromUser;
    private SafetyUser toUser;
}
