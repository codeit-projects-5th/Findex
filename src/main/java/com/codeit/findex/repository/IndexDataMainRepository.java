package com.codeit.findex.repository;

import com.codeit.findex.dto.data.*;

import com.codeit.findex.entity.AutoSync;
import com.codeit.findex.entity.IndexDataMain;
import com.codeit.findex.repository.custom.IndexDataRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface IndexDataMainRepository extends JpaRepository<IndexDataMain, Long> {

    // 현재 기준 데이터
    @Query(
            value = """
                    SELECT  a.base_date     as date 
                           ,a.closing_price as value 
                      FROM  index_data a
                """,
            nativeQuery = true
    )
    List<IndexDataChartValueDto> searchIndexDataChart(IndexDataChartDto chartDto);

    // 5일 평균 데이터
    @Query(
            value = """
                    SELECT 
                        date,
                        ROUND(AVG(close_price) OVER (
                            PARTITION BY symbol ORDER BY trade_date
                            ROWS BETWEEN 4 PRECEDING AND CURRENT ROW
                        ), 2) AS value -- ma5,
                    FROM stock_prices
                    WHERE symbol = :indexInfoId
                    ORDER BY trade_date
                """,
            nativeQuery = true
    )
    List<IndexDataChartValueMa5Dto> searchIndexDataChartMa5 (IndexDataChartDto chartDto);

    // 28일 평균 데이터
    @Query(
            value = """
                    SELECT 
                        date,
                        ROUND(AVG(close_price) OVER (
                            PARTITION BY symbol ORDER BY trade_date
                            ROWS BETWEEN 27 PRECEDING AND CURRENT ROW
                        ), 2) AS value -- ma28 
                    FROM stock_prices
                    WHERE symbol = :indexInfoId
                    ORDER BY trade_date
                """,
            nativeQuery = true
    )
    List<IndexDataChartValueMa28Dto> searchIndexDataChartMa28 (IndexDataChartDto chartDto);

//    boolean existsByIndexInfoIdAndBaseDate(Long indexInfoId, LocalDate baseDate);


    // 성과 목록 조회
    @Query(
            value = """
                    SELECT  TA.*
                      FROM  (
                           SELECT  T.*
                                  ,(T.CLOSING_PRICE - T.VALUE_AFTER_30) AS VERSUS
                                  ,COALESCE(ROUND((T.CLOSING_PRICE/T.VALUE_AFTER_30)*100,2) - 100,0) AS FLUCTUATION_RATE
                             FROM  (
                                   SELECT  X.*
                                          ,LEAD(X.closing_price, 29) OVER(
                                                 ORDER BY X.index_classification, X.index_name, X.base_date DESC
                                           )
                                           AS value_after_30
                                     FROM  (
                                               SELECT  A.ID -- as indexInfoId
                                                      ,A.INDEX_CLASSIFICATION -- as indexClassification
                                                      ,A.INDEX_NAME -- as indexName
                                                      ,b.closing_PRICE -- as currentPrice
                                                      ,B.base_date
                                               FROM INDEX_INFOS A
                                               JOIN INDEX_DATA B ON A.ID = B.INDEX_INFO_ID
                                               WHERE 1=1
                                                 AND  A.INDEX_CLASSIFICATION = '테마지수'
                                                 AND  A.INDEX_NAME = '코스피 200 기후변화지수'
                                           )  X
                                    WHERE  1=1
                                   ) T
                            WHERE  1=1
                           ) TA
                     WHERE  1=1
                     ORDER  BY TA.FLUCTUATION_RATE DESC
                """,
                    nativeQuery = true)
    List<IndexDataPerformanceDto> searchIndexDataRank(IndexDataRankDto rankDto);




}