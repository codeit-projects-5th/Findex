package com.codeit.findex.dto.data;

import lombok.Builder;
import java.math.BigDecimal;

@Builder
public record IndexDataRankDto(
        IndexDataPerformanceDto performance,
        int rank
) {}