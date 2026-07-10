package com.agenthub.domain.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RerankServiceTest {

    private final RerankService rerankService = new RerankService();

    @Test
    void keywordAndOverlapImproveScore() {
        double matched = rerankService.rerank("报销 发票", "员工报销需要提交发票和审批单", 0.2, 1.0);
        double unrelated = rerankService.rerank("报销 发票", "会议室预约需要提前一天提交", 0.2, 0.0);

        assertThat(matched).isGreaterThan(unrelated);
        assertThat(rerankService.keywordScore("报销 发票", "员工报销需要提交发票")).isGreaterThan(0);
    }
}
