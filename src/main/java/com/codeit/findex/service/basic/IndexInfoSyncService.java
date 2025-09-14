package com.codeit.findex.service.basic;

import com.codeit.findex.client.MarketIndexApiClient;
import com.codeit.findex.dto.data.IndexInfoUnique;
import com.codeit.findex.dto.response.MarketIndexApiResponse;
import com.codeit.findex.entity.*;
import com.codeit.findex.repository.IndexInfoRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * OpenAPI에서 가져온 데이터를 가공해서 IndexInfo 테이블에 저장
 */
@Service
@RequiredArgsConstructor
public class IndexInfoSyncService {

    private final IndexInfoRepository indexInfoRepository;
    private final MarketIndexApiClient marketIndexApiClient;

    /** OpenApi에서 받아온 데이터로 Index_infos 값에 매핑 후 DB에 저장 */
    @Transactional
    public void createIndexInfos(String workerId) {
        int pageNo = 1;
        int pageSize = 200;
        String lastSyncedDate = "20250901"; // 현재 하드코딩이지만 SyncJobRepository에서 가져오기

        List<IndexInfo> indexInfoRegistry = new ArrayList<>(); // IndexInfo 테이블에 최종적으로 저장되는 데이터 목록
        List<SyncJob> syncJobRegistry = new ArrayList<>(); // SyncJob 테이블에 최종적으로 저장되는 데이터 목록

        // 이미 디비에 존재하는 index infos를 조회해서 IndexInfoUnique로 변환 ex) IndexInfoUnique[indexClassification=테마지수, indexName=KRX/S&P 탄소효율 그린뉴딜지수]
        Set<IndexInfoUnique> existIndexInfos = indexInfoRepository.findAll().stream()
                .map(item -> IndexInfoUnique.builder()
                        .indexClassification(item.getIndexClassification())
                        .indexName(item.getIndexName())
                        .build())
                .collect(Collectors.toSet());

        // 5. OpenApi에서 가져온 baseDate를 LocalDate로 변환
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");


        while (true) {
            // 1. OpenAPI에서 가져온 순수 응답데이터
            List<MarketIndexApiResponse.Item> fetchedIndexInfos = marketIndexApiClient.getFromOpenApiByPage(pageNo, pageSize, lastSyncedDate).getResponse().getBody().getItems().getItem();

            if(fetchedIndexInfos.isEmpty())  break; // 비어있으면 루프 중단

            for (MarketIndexApiResponse.Item item : fetchedIndexInfos) {
                IndexInfoUnique uniqueKey = IndexInfoUnique.builder()
                        .indexClassification(item.getIndexClassification())
                        .indexName(item.getIndexName())
                        .build();

                if (existIndexInfos.contains(uniqueKey)) {
                    continue;
                }

                IndexInfo newIndexInfo = IndexInfo.builder()
                        .indexClassification(item.getIndexClassification()) // 지수 분류 명
                        .indexName(item.getIndexName()) // 지수명
                        .employedItemsCount(Integer.valueOf(item.getEmployedItemsCount())) //채용 종목 수
                        .basePointInTime (LocalDate.parse(item.getBasePointInTime(), formatter))
                        .baseIndex(Double.valueOf(item.getBaseIndex()))
                        .sourceType(SourceType.OPEN_API)
                        .favorite(false)
                        .build();

                SyncJob newSyncJob = SyncJob.builder()
                        .indexInfo(newIndexInfo)
                        .jobType(JobType.INDEX_INFO)
                        .jobTime(LocalDateTime.now()) // 작업 일시
                        .targetDate(LocalDate.now()) // 연동한 날짜(대상 날짜)
                        .worker(workerId)
                        .result(ResultType.SUCCESS.toBoolean())
                        .build();

                indexInfoRegistry.add(newIndexInfo);
                syncJobRegistry.add(newSyncJob);
                existIndexInfos.add(uniqueKey);
            }

            pageNo++;
        }

        indexInfoRepository.saveAllInBatch(indexInfoRegistry);
    }
}
