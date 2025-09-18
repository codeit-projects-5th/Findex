package com.codeit.findex.dto.request;

import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

public record IndexDataSearchCondition(
    Long indexInfoId,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
    String cursor,
    String sortField,
    String sortDirection,
    Integer size
) {
    public IndexDataSearchCondition {
        if (sortField == null || sortField.isBlank()) {
            sortField = "baseDate";
        }
        if (sortDirection == null || sortDirection.isBlank()) {
            sortDirection = "desc";
        }
        if (size == null) {
            size = 10;
        }
    }

    /**
     * 커서에서 정렬 값을 추출
     * 커서 형태: "sortValue_id" (예: "2850.75_20", "2023-12-31_15")
     */
    public Object getLastSortValue() {
        if (cursor == null || cursor.isBlank() || !cursor.contains("_")) {
            return null;
        }
        
        try {
            String sortValue = cursor.substring(0, cursor.lastIndexOf("_"));
            
            return switch (sortField) {
                case "baseDate" -> LocalDate.parse(sortValue);
                case "marketPrice", "closingPrice", "highPrice", "lowPrice", 
                     "versus", "fluctuationRate" -> new BigDecimal(sortValue);
                case "tradingQuantity", "tradingPrice", "marketTotalAmount" -> Long.parseLong(sortValue);
                default -> null;
            };
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 커서에서 마지막 ID를 추출
     */
    public Long getLastId() {
        if (cursor == null || cursor.isBlank() || !cursor.contains("_")) {
            return null;
        }
        
        try {
            String idPart = cursor.substring(cursor.lastIndexOf("_") + 1);
            return Long.parseLong(idPart);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 정렬 방향이 내림차순인지 확인
     */
    public boolean isDescending() {
        return "desc".equalsIgnoreCase(sortDirection);
    }
}