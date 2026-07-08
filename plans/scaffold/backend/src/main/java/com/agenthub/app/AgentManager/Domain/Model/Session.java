package com.agenthub.app.AgentManager.Domain.Model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("session")
public class Session {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long agentId;
    
    private String userId;
    
    private String context; // JSON 格式的会话上下文
    
    private String status; // ACTIVE, ENDED
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    @TableLogic
    private Integer deleted;
}