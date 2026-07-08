package com.agenthub.app.AgentManager.Domain.Repository;

import com.agenthub.app.AgentManager.Domain.Model.Session;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SessionRepository extends BaseMapper<Session> {
}