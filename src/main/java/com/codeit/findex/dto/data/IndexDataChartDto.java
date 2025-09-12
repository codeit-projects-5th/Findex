package com.codeit.findex.dto.data;

import com.codeit.findex.entity.SourceType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import lombok.Builder;

@Builder
public record IndexDataChartDto(
        Long indexInfoId,
        String indexClassification,
        String indexName,
        String periodType,
        List<IndexDataChartValueDto> dataPoints,
        List<IndexDataChartValueMa5Dto> dataPointsMa5,
        List<IndexDataChartValueMa28Dto> dataPointsMa28

) {}