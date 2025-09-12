package com.codeit.findex.dto.data;

import lombok.Builder;

import java.util.List;

@Builder
public record IndexDataChartValueDto(
        String date,
        String value
) {}