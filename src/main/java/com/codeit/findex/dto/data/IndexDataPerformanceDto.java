package com.codeit.findex.dto.data;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.Objects;

@Builder
public final class IndexDataPerformanceDto {
    private final int indexInfoId;
    private final String indexClassification;
    private final String indexName;
    private final BigDecimal versus;
    private final BigDecimal fluctuationRate;
    private final BigDecimal currentPrice;
    private final BigDecimal beforePrice;

    public IndexDataPerformanceDto(
            int indexInfoId,
            String indexClassification,
            String indexName,
            BigDecimal versus,
            BigDecimal fluctuationRate,
            BigDecimal currentPrice,
            BigDecimal beforePrice
    ) {
        this.indexInfoId = indexInfoId;
        this.indexClassification = indexClassification;
        this.indexName = indexName;
        this.versus = versus;
        this.fluctuationRate = fluctuationRate;
        this.currentPrice = currentPrice;
        this.beforePrice = beforePrice;
    }

    public int indexInfoId() {
        return indexInfoId;
    }

    public String indexClassification() {
        return indexClassification;
    }

    public String indexName() {
        return indexName;
    }

    public BigDecimal versus() {
        return versus;
    }

    public BigDecimal fluctuationRate() {
        return fluctuationRate;
    }

    public BigDecimal currentPrice() {
        return currentPrice;
    }

    public BigDecimal beforePrice() {
        return beforePrice;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (IndexDataPerformanceDto) obj;
        return this.indexInfoId == that.indexInfoId &&
                Objects.equals(this.indexClassification, that.indexClassification) &&
                Objects.equals(this.indexName, that.indexName) &&
                Objects.equals(this.versus, that.versus) &&
                Objects.equals(this.fluctuationRate, that.fluctuationRate) &&
                Objects.equals(this.currentPrice, that.currentPrice) &&
                Objects.equals(this.beforePrice, that.beforePrice);
    }

}