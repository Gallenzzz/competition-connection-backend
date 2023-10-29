package com.gallen.vos.im;

import com.gallen.vos.user.SafetyUser;
import lombok.Data;

import java.util.Date;

@Data
public class ConversationVo {
    private SafetyUser user;
    private Date sendDate;
    private String message;
    private Integer unreadNum;
}
