package com.codeit.findex.service.basic;

import com.codeit.findex.dto.data.CursorPageResponseSyncJobDto;
import com.codeit.findex.dto.data.SyncJobDto;
import com.codeit.findex.dto.request.IndexDataSyncRequest;
import com.codeit.findex.dto.response.MarketIndexApiResponse;
import com.codeit.findex.entity.*;
import com.codeit.findex.mapper.SyncJobMapper;
import com.codeit.findex.repository.IndexInfoRepository;
import com.codeit.findex.repository.SyncJobRepository;
import com.codeit.findex.service.SyncJobService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class BasicSyncJobService implements SyncJobService {

    @Value("${external.finance.service-key}")
    private String serviceKey;

    private final WebClient financeWebClient;
    private final IndexInfoRepository indexInfoRepository;
    private final SyncJobRepository syncJobRepository;
    private final SyncJobMapper syncJobMapper;

    @Transactional
    @Override
    public List<SyncJobDto> createSyncJob(String workerId) {
        // 1. 지수 정보 DB에 저장
        createIndexInfos();

        List<SyncJob> syncJobList = indexInfoRepository.findAll().stream()
                .map(indexInfo -> {
                    return SyncJob.builder()
                            .indexInfo(indexInfo)
                            .jobType(JobType.INDEX_INFO)
                            .jobTime(Instant.now()) // 작업 일시
                            .targetDate(LocalDate.now()) // 연동한 날짜(대상 날짜)
                            .worker(workerId)
                            .result(true)
                            .build();
                }).toList();

        return syncJobRepository.saveAll(syncJobList).stream()
                .map(syncJobMapper::toDto).toList();
    }

    @Override
    public void createSyncIndexData(IndexDataSyncRequest request) {

        String beginDate = request.baseDateFrom().replace("-", "");
        String endDate = request.baseDateTo().replace("-", "");

        List<IndexInfo> indexInfoList = indexInfoRepository.findAllById(request.indexInfoIds());

        if (indexInfoList.size() != request.indexInfoIds().size()) {
            throw new IllegalArgumentException("존재하지 않는 지수정보가 포함되어 있습니다.");
        }

        Set<String> indexNames = indexInfoList.stream().map(IndexInfo::getIndexName).collect(Collectors.toSet());

        List<IndexData> indexDataList = getFromOpenApiByBaseDate(beginDate, endDate).getResponse().getBody().getItems().getItem().stream()
                .filter(item -> indexNames.contains(item.getIndexName()))
                .map(item -> {
                    IndexInfo matchedInfo = indexInfoList.stream()
                            .filter(info -> info.getIndexName().equals(item.getIndexName()))
                            .findFirst()
                            .orElseThrow(() -> new IllegalArgumentException("IndexInfo not found for " + item.getIndexName()));

                    return IndexData.builder()
                            .indexInfo(matchedInfo)   // 여기 넣기
                            .baseDate(item.getBaseDate())
                            .sourceType(SourceType.OPEN_API)
                            .marketPrice(item.getMarketPrice())
                            .closingPrice(item.getClosingPrice())
                            .highPrice(item.getHighPrice())
                            .lowPrice(item.getLowPrice())
                            .versus(item.getVersus())
                            .fluctuationRate(item.getFluctuationRate())
                            .tradingPrice(item.getTradingPrice())
                            .tradingQuantity(item.getTradingQuantity())
                            .marketTotalAmount(item.getMarketTotalAmount())
                            .build();
                })
                .toList();
    }

    /** OpenApi에서 받아온 데이터로 Index_infos 값에 매핑 후 DB에 저장 */
    public void createIndexInfos() {
        int pageNo = 1;
        int pageSize = 100;

        Set<String> seen = new HashSet<>();

        // 1. OpenAPI 호출
        while (true) {
            // 데이터 1000개
            List<IndexInfo> newIndexInfoList = getFromOpenApiByPage(pageNo, pageSize).getResponse().getBody().getItems().getItem().stream()
                    .filter(item -> seen.add(item.getIndexClassification() + ":" + item.getIndexName())) // 지수분류명 + 지수명으로 중복 제거
                    // DB에 이미 존재하는 지수 정보는 제외
                    .filter(item -> !indexInfoRepository.existsByIndexClassificationAndIndexName(
                            item.getIndexClassification(),
                            item.getIndexName()
                    ))
                    .map(item -> IndexInfo.builder()
                            .indexClassification(item.getIndexClassification()) // 지수 분류 명
                            .indexName(item.getIndexName()) // 지수명
                            .employedItemsCount(Integer.valueOf(item.getEmployedItemsCount())) //채용 종목 수
                            .basePointInTime(item.getBasePointInTime())
                            .baseIndex(Double.valueOf(item.getBaseIndex()))
                            .sourceType(SourceType.OPEN_API)
                            .favorite(false)
                            .build()
                    )
                    .toList();

            // 중복이 제거된 가져온 새로운 지수 정보들 DB에 저장
            indexInfoRepository.saveAll(newIndexInfoList);

            pageNo++;
            //if(itemList.isEmpty())  break;
            if(pageNo == 5) break;
        }
    }


    @Override
    public MarketIndexApiResponse findAll() {
        MarketIndexApiResponse response = getFromOpenApiByPage(1, 100);
        return response;
    }

    public MarketIndexApiResponse getFromOpenApiByPage(int pageNo, int numOfRows) {
        return financeWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/getStockMarketIndex")
                        .queryParam("serviceKey", serviceKey)
                        .queryParam("resultType", "json")
                        .queryParam("pageNo", pageNo)
                        .queryParam("numOfRows", numOfRows)
                        .build())
                .accept(MediaType.ALL)
                .retrieve()
                .bodyToMono(MarketIndexApiResponse.class)
                .block();
    }

    public MarketIndexApiResponse getFromOpenApiByBaseDate(String beginDate, String endDate) {

        if(beginDate == null || beginDate.length() != 8) throw new IllegalArgumentException("잘못된 날짜 정보입니다.");
        if(endDate == null || endDate.length() != 8) throw new IllegalArgumentException("잘못된 날짜 정보입니다.");

        return financeWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/getStockMarketIndex")
                        .queryParam("serviceKey", serviceKey)
                        .queryParam("resultType", "json")
                        .queryParam("pageNo", 1)
                        .queryParam("numOfRows", 500)
                        .queryParam("beginBasDt", beginDate)
                        .queryParam("endBasDt", endDate)
                        .build())
                .accept(MediaType.ALL)
                .retrieve()
                .bodyToMono(MarketIndexApiResponse.class)
                .block();
    }

}
