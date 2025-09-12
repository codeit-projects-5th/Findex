package com.codeit.findex.dto.data;

import lombok.Builder;

@Builder
public record IndexDataChartValueMa5Dto(
        String date,
        String value
) {}