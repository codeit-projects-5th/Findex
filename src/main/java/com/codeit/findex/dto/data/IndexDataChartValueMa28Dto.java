package com.codeit.findex.dto.data;

import lombok.Builder;

@Builder
public record IndexDataChartValueMa28Dto(
        String date,
        String value
) {}