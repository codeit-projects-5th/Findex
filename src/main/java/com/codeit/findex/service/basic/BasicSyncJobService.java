package com.codeit.findex.service.basic;

import com.codeit.findex.client.MarketIndexApiClient;
import com.codeit.findex.dto.data.CursorPageResponseSyncJobDto;
import com.codeit.findex.dto.data.SyncJobDto;
import com.codeit.findex.dto.request.IndexDataSyncRequest;
import com.codeit.findex.dto.request.SyncJobSearchRequest;
import com.codeit.findex.entity.*;
import com.codeit.findex.mapper.SyncJobMapper;
import com.codeit.findex.repository.IndexDataRepository;
import com.codeit.findex.repository.IndexInfoRepository;
import com.codeit.findex.repository.SyncJobRepository;
import com.codeit.findex.service.SyncJobService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class BasicSyncJobService implements SyncJobService {

    private final IndexDataRepository indexDataRepository;
    private final IndexInfoSyncService indexInfoSyncService;
    private final IndexDataSyncService indexDataSyncService;
    private final SyncJobRepository syncJobRepository;
    private final SyncJobMapper syncJobMapper;

    /**
     * 지수 정보
     * @param workerId 작성자의 IP 주소
     */
    @Transactional @Override
    public List<SyncJobDto> createIndexInfoSyncJob(String workerId) {
        // 1. 지수 정보 DB에 저장
        indexInfoSyncService.createIndexInfos(workerId);

//        List<SyncJob> syncJobList = indexInfoRepository.findAll().stream()
//                .map(indexInfo -> {
//                    return SyncJob.builder()
//                            .indexInfo(indexInfo)
//                            .jobType(JobType.INDEX_INFO)
//                            .jobTime(LocalDateTime.now()) // 작업 일시
//                            .targetDate(LocalDate.now()) // 연동한 날짜(대상 날짜)
//                            .worker(workerId)
//                            .result(ResultType.SUCCESS.toBoolean())
//                            .build();
//                }).toList();

//        List<SyncJob> created = syncJobRepository.saveAll(syncJobList);

        return null;
    }

    public List<SyncJobDto> createIndexDataSyncJob(String workerId, IndexDataSyncRequest request) {
        // 1. 지수 데이터 DB에 저장
        indexDataSyncService.createIndexData(request);

        // 2. 지수 정보(지수분류명)에 해당하는 지수 데이터 조회
        List<SyncJob> syncJobList = indexDataRepository.findByIndexInfoIds(request.indexInfoIds()).stream()
                .map(indexData -> {
                    return  SyncJob.builder()
                            .indexInfo(indexData.getIndexInfo())
                            .jobType(JobType.INDEX_DATA)
                            .jobTime(LocalDateTime.now())
                            .targetDate(LocalDate.now())
                            .worker(workerId)
                            .result(ResultType.SUCCESS.toBoolean())
                            .build();
                }).toList();

        return syncJobRepository.saveAll(syncJobList).stream()
                .map(syncJobMapper::toDto).toList();
    }

    @Override
    public CursorPageResponseSyncJobDto findAll(SyncJobSearchRequest param) {

        // 1. 쿼리로 데이터 조회
        List<SyncJob> syncJobList = syncJobRepository.search(param);
        long total = syncJobRepository.count(param);

        // 2. 다음 페이지 존재여부 확인
        boolean hasNext = syncJobList.size() > param.size();
        if (hasNext) syncJobList.remove(syncJobList.size() - 1);

        List<SyncJobDto> content = syncJobList.stream().map(syncJobMapper::toDto).toList();

        String nextCursor = null;
        Long nextIdAfter = null;

        if(hasNext) {
            SyncJob lastItem = syncJobList.get(syncJobList.size() - 1);
            String cursorJson = String.format("{\"id\":%d}", lastItem.getId());
            nextCursor = Base64.getEncoder().encodeToString(cursorJson.getBytes());
            nextIdAfter = lastItem.getId();
        }

        return CursorPageResponseSyncJobDto.builder()
                .content(content)
                .nextCursor(nextCursor)
                .nextIdAfter(nextIdAfter)
                .size(param.size())
                .totalElements(total)
                .hasNext(hasNext)
                .build();
    }
}