package com.codeit.findex.repository.custom;

import com.codeit.findex.dto.request.IndexDataSearchCondition;
import com.codeit.findex.entity.IndexData;
import com.codeit.findex.entity.QIndexData;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.ComparableExpressionBase;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class IndexDataRepositoryImpl implements IndexDataRepositoryCustom {

    private final EntityManager em;
    private final JPAQueryFactory queryFactory;

    /**
     * 정렬 가능 필드 화이트리스트
     */
    private static final List<String> ALLOWED_SORT_FIELDS = Arrays.asList(
        "baseDate", "marketPrice", "closingPrice", "highPrice", "lowPrice",
        "versus", "fluctuationRate", "tradingQuantity", "tradingPrice", "marketTotalAmount"
    );

    @Override
    public Slice<IndexData> findSlice(IndexDataSearchCondition condition) {
        QIndexData indexData = QIndexData.indexData;
        
        // 기본 필터 조건
        BooleanBuilder where = buildBasicConditions(condition, indexData);
        
        // 커서 기반 조건 추가
        buildCursorCondition(condition, indexData, where);
        
        // 정렬 조건 생성
        OrderSpecifier<?> orderSpecifier = buildOrderSpecifier(condition, indexData);
        
        // 메인 쿼리 실행 (size + 1개 조회)
        List<IndexData> content = queryFactory
            .selectFrom(indexData)
            .where(where)
            .orderBy(orderSpecifier, indexData.id.asc()) // 2차 정렬로 일관성 보장
            .limit(condition.size() + 1)
            .fetch();
        
        // hasNext 판단
        boolean hasNext = content.size() > condition.size();
        if (hasNext) {
            content.remove(content.size() - 1); // 초과분 제거
        }
        
        log.debug("Slice query executed - size: {}, hasNext: {}, sortField: {}", 
                 content.size(), hasNext, condition.sortField());
        
        return new SliceImpl<>(content, createPageable(condition), hasNext);
    }

    /**
     * 기본 필터 조건 생성
     */
    private BooleanBuilder buildBasicConditions(IndexDataSearchCondition condition, QIndexData indexData) {
        BooleanBuilder where = new BooleanBuilder();
        
        if (condition.indexInfoId() != null) {
            where.and(indexData.indexInfo.id.eq(condition.indexInfoId()));
        }
        if (condition.startDate() != null) {
            where.and(indexData.baseDate.goe(condition.startDate()));
        }
        if (condition.endDate() != null) {
            where.and(indexData.baseDate.loe(condition.endDate()));
        }
        
        return where;
    }

    /**
     * 커서 기반 조건 생성 (정렬 필드별 최적화)
     */
    private void buildCursorCondition(IndexDataSearchCondition condition, QIndexData indexData, BooleanBuilder where) {
        Object lastSortValue = condition.getLastSortValue();
        Long lastId = condition.getLastId();
        
        if (lastSortValue == null || lastId == null) {
            return; // 커서가 없으면 첫 페이지
        }
        
        String sortField = condition.sortField();
        boolean isDesc = condition.isDescending();
        
        log.debug("Building cursor condition - sortField: {}, lastValue: {}, lastId: {}, isDesc: {}", 
                 sortField, lastSortValue, lastId, isDesc);
        
        BooleanBuilder cursorCondition = new BooleanBuilder();
        
        switch (sortField) {
            case "baseDate" -> {
                LocalDate dateValue = (LocalDate) lastSortValue;
                if (isDesc) {
                    cursorCondition.or(indexData.baseDate.lt(dateValue))
                                  .or(indexData.baseDate.eq(dateValue).and(indexData.id.gt(lastId)));
                } else {
                    cursorCondition.or(indexData.baseDate.gt(dateValue))
                                  .or(indexData.baseDate.eq(dateValue).and(indexData.id.gt(lastId)));
                }
            }
            case "marketPrice" -> {
                BigDecimal value = (BigDecimal) lastSortValue;
                if (isDesc) {
                    cursorCondition.or(indexData.marketPrice.lt(value))
                                  .or(indexData.marketPrice.eq(value).and(indexData.id.gt(lastId)));
                } else {
                    cursorCondition.or(indexData.marketPrice.gt(value))
                                  .or(indexData.marketPrice.eq(value).and(indexData.id.gt(lastId)));
                }
            }
            case "closingPrice" -> {
                BigDecimal value = (BigDecimal) lastSortValue;
                if (isDesc) {
                    cursorCondition.or(indexData.closingPrice.lt(value))
                                  .or(indexData.closingPrice.eq(value).and(indexData.id.gt(lastId)));
                } else {
                    cursorCondition.or(indexData.closingPrice.gt(value))
                                  .or(indexData.closingPrice.eq(value).and(indexData.id.gt(lastId)));
                }
            }
            case "highPrice" -> {
                BigDecimal value = (BigDecimal) lastSortValue;
                if (isDesc) {
                    cursorCondition.or(indexData.highPrice.lt(value))
                                  .or(indexData.highPrice.eq(value).and(indexData.id.gt(lastId)));
                } else {
                    cursorCondition.or(indexData.highPrice.gt(value))
                                  .or(indexData.highPrice.eq(value).and(indexData.id.gt(lastId)));
                }
            }
            case "lowPrice" -> {
                BigDecimal value = (BigDecimal) lastSortValue;
                if (isDesc) {
                    cursorCondition.or(indexData.lowPrice.lt(value))
                                  .or(indexData.lowPrice.eq(value).and(indexData.id.gt(lastId)));
                } else {
                    cursorCondition.or(indexData.lowPrice.gt(value))
                                  .or(indexData.lowPrice.eq(value).and(indexData.id.gt(lastId)));
                }
            }
            case "versus" -> {
                BigDecimal value = (BigDecimal) lastSortValue;
                if (isDesc) {
                    cursorCondition.or(indexData.versus.lt(value))
                                  .or(indexData.versus.eq(value).and(indexData.id.gt(lastId)));
                } else {
                    cursorCondition.or(indexData.versus.gt(value))
                                  .or(indexData.versus.eq(value).and(indexData.id.gt(lastId)));
                }
            }
            case "fluctuationRate" -> {
                BigDecimal value = (BigDecimal) lastSortValue;
                if (isDesc) {
                    cursorCondition.or(indexData.fluctuationRate.lt(value))
                                  .or(indexData.fluctuationRate.eq(value).and(indexData.id.gt(lastId)));
                } else {
                    cursorCondition.or(indexData.fluctuationRate.gt(value))
                                  .or(indexData.fluctuationRate.eq(value).and(indexData.id.gt(lastId)));
                }
            }
            case "tradingQuantity" -> {
                Long value = (Long) lastSortValue;
                if (isDesc) {
                    cursorCondition.or(indexData.tradingQuantity.lt(value))
                                  .or(indexData.tradingQuantity.eq(value).and(indexData.id.gt(lastId)));
                } else {
                    cursorCondition.or(indexData.tradingQuantity.gt(value))
                                  .or(indexData.tradingQuantity.eq(value).and(indexData.id.gt(lastId)));
                }
            }
            case "tradingPrice" -> {
                Long value = (Long) lastSortValue;
                if (isDesc) {
                    cursorCondition.or(indexData.tradingPrice.lt(value))
                                  .or(indexData.tradingPrice.eq(value).and(indexData.id.gt(lastId)));
                } else {
                    cursorCondition.or(indexData.tradingPrice.gt(value))
                                  .or(indexData.tradingPrice.eq(value).and(indexData.id.gt(lastId)));
                }
            }
            case "marketTotalAmount" -> {
                Long value = (Long) lastSortValue;
                if (isDesc) {
                    cursorCondition.or(indexData.marketTotalAmount.lt(value))
                                  .or(indexData.marketTotalAmount.eq(value).and(indexData.id.gt(lastId)));
                } else {
                    cursorCondition.or(indexData.marketTotalAmount.gt(value))
                                  .or(indexData.marketTotalAmount.eq(value).and(indexData.id.gt(lastId)));
                }
            }
        }
        
        where.and(cursorCondition);
    }

    /**
     * 정렬 조건 생성
     */
    private OrderSpecifier<?> buildOrderSpecifier(IndexDataSearchCondition condition, QIndexData indexData) {
        String sortField = condition.sortField();
        
        // 화이트리스트 검증
        if (!ALLOWED_SORT_FIELDS.contains(sortField)) {
            throw new IllegalArgumentException("허용되지 않은 정렬 필드입니다: " + sortField);
        }
        
        Order order = condition.isDescending() ? Order.DESC : Order.ASC;
        
        return switch (sortField) {
            case "baseDate" -> new OrderSpecifier<>(order, indexData.baseDate);
            case "marketPrice" -> new OrderSpecifier<>(order, indexData.marketPrice);
            case "closingPrice" -> new OrderSpecifier<>(order, indexData.closingPrice);
            case "highPrice" -> new OrderSpecifier<>(order, indexData.highPrice);
            case "lowPrice" -> new OrderSpecifier<>(order, indexData.lowPrice);
            case "versus" -> new OrderSpecifier<>(order, indexData.versus);
            case "fluctuationRate" -> new OrderSpecifier<>(order, indexData.fluctuationRate);
            case "tradingQuantity" -> new OrderSpecifier<>(order, indexData.tradingQuantity);
            case "tradingPrice" -> new OrderSpecifier<>(order, indexData.tradingPrice);
            case "marketTotalAmount" -> new OrderSpecifier<>(order, indexData.marketTotalAmount);
            default -> new OrderSpecifier<>(Order.DESC, indexData.baseDate); // fallback
        };
    }

    /**
     * Pageable 객체 생성
     */
    private Pageable createPageable(IndexDataSearchCondition condition) {
        return Pageable.ofSize(condition.size());
    }

    // =============================================================================
    // 기존 메서드들 (호환성 유지)
    // =============================================================================

    @Override
    public List<IndexData> search(IndexDataSearchCondition condition) {
        // 기존 구현 유지 (필요시 사용)
        Slice<IndexData> slice = findSlice(condition);
        return slice.getContent();
    }

    @Override
    public long count(IndexDataSearchCondition condition) {
        QIndexData indexData = QIndexData.indexData;
        BooleanBuilder where = buildBasicConditions(condition, indexData);
        
        return Optional.ofNullable(queryFactory
                .select(indexData.id.countDistinct())
                .from(indexData)
                .where(where)
                .fetchOne()).orElse(0L);
    }

    @Override
    public List<IndexData> findAllByCondition(IndexDataSearchCondition condition) {
        StringBuilder jpqlBuilder = new StringBuilder("SELECT d FROM IndexData d WHERE 1=1");

        if (condition.indexInfoId() != null) {
            jpqlBuilder.append(" AND d.indexInfo.id = :indexInfoId");
        }
        if (condition.startDate() != null) {
            jpqlBuilder.append(" AND d.baseDate >= :startDate");
        }
        if (condition.endDate() != null) {
            jpqlBuilder.append(" AND d.baseDate <= :endDate");
        }

        String sortBy = condition.sortField();
        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new IllegalArgumentException("허용되지 않은 정렬 필드입니다: " + sortBy);
        }
        String sortDirection = condition.sortDirection().equalsIgnoreCase("DESC") ? "DESC" : "ASC";
        jpqlBuilder.append(" ORDER BY d.").append(sortBy).append(" ").append(sortDirection);

        TypedQuery<IndexData> query = em.createQuery(jpqlBuilder.toString(), IndexData.class);

        if (condition.indexInfoId() != null) {
            query.setParameter("indexInfoId", condition.indexInfoId());
        }
        if (condition.startDate() != null) {
            query.setParameter("startDate", condition.startDate());
        }
        if (condition.endDate() != null) {
            query.setParameter("endDate", condition.endDate());
        }

        return query.getResultList();
    }

    @Override
    public void saveAllInBatch(List<IndexData> indexDataList, Long indexInfoId) {
        StringBuilder query = new StringBuilder();

        query.append("INSERT INTO index_data ")
            .append("(index_info_id, base_date, source_type, market_price, closing_price, high_price, " +
                    "low_price, versus, fluctuation_rate, trading_quantity, trading_price, market_total_amount) ")
            .append("VALUES ");

        for (int i = 0; i < indexDataList.size(); i++) {
            IndexData data = indexDataList.get(i);

            query.append("(")
                .append("'").append(indexInfoId).append("', ")
                .append("'").append(data.getBaseDate()).append("', ")
                .append("'").append(data.getSourceType()).append("', ")
                .append(data.getMarketPrice()).append(", ")
                .append(data.getClosingPrice()).append(", ")
                .append(data.getHighPrice()).append(", ")
                .append(data.getLowPrice()).append(", ")
                .append(data.getVersus()).append(", ")
                .append(data.getFluctuationRate()).append(", ")
                .append(data.getTradingQuantity()).append(", ")
                .append(data.getTradingPrice()).append(", ")
                .append(data.getMarketTotalAmount())
                .append(")");

            if (i < indexDataList.size() - 1) {
                query.append(", ");
            }
        }

        em.createNativeQuery(query.toString()).executeUpdate();
    }
}