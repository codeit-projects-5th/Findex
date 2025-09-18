package com.codeit.findex.service.basic;

import com.codeit.findex.dto.data.CursorPageResponseIndexDataDto;
import com.codeit.findex.dto.data.IndexDataDto;
import com.codeit.findex.dto.request.IndexDataCreateRequest;
import com.codeit.findex.dto.request.IndexDataSearchCondition;
import com.codeit.findex.dto.request.IndexDataUpdateRequest;
import com.codeit.findex.entity.IndexData;
import com.codeit.findex.entity.IndexInfo;
import com.codeit.findex.entity.SourceType;
import com.codeit.findex.mapper.IndexDataMapper;
import com.codeit.findex.repository.IndexDataRepository;
import com.codeit.findex.repository.IndexInfoRepository;
import com.codeit.findex.service.IndexDataService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BasicIndexDataService implements IndexDataService {

    private final IndexDataRepository indexDataRepository;
    private final IndexInfoRepository indexInfoRepository;
    private final IndexDataMapper indexDataMapper;

    @Override
    @Transactional
    public IndexDataDto createIndexData(IndexDataCreateRequest request) {
        if (indexDataRepository.existsByIndexInfoIdAndBaseDate(request.indexInfoId(), request.baseDate())) {
            throw new IllegalArgumentException("이미 해당 날짜에 등록된 지수 데이터가 존재합니다.");
        }

        IndexInfo indexInfo = indexInfoRepository.findById(request.indexInfoId())
                .orElseThrow(() -> new EntityNotFoundException("해당 ID의 지수 정보를 찾을 수 없습니다: " + request.indexInfoId()));

        IndexData indexData = indexDataMapper.toEntity(request, indexInfo, SourceType.USER);
        IndexData savedIndexData = indexDataRepository.save(indexData);

        return indexDataMapper.toDto(savedIndexData);
    }

    @Override
    @Transactional
    public IndexDataDto updateIndexData(Long id, IndexDataUpdateRequest request) {
        IndexData indexData = indexDataRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("해당 ID의 주가 데이터를 찾을 수 없습니다: " + id));

        if (!isUpdateNeeded(request, indexData)) {
            return indexDataMapper.toDto(indexData);
        }

        indexData.update(
                Objects.requireNonNullElse(request.marketPrice(), indexData.getMarketPrice()),
                Objects.requireNonNullElse(request.closingPrice(), indexData.getClosingPrice()),
                Objects.requireNonNullElse(request.highPrice(), indexData.getHighPrice()),
                Objects.requireNonNullElse(request.lowPrice(), indexData.getLowPrice()),
                Objects.requireNonNullElse(request.versus(), indexData.getVersus()),
                Objects.requireNonNullElse(request.fluctuationRate(), indexData.getFluctuationRate()),
                Objects.requireNonNullElse(request.tradingQuantity(), indexData.getTradingQuantity()),
                Objects.requireNonNullElse(request.tradingPrice(), indexData.getTradingPrice()),
                Objects.requireNonNullElse(request.marketTotalAmount(), indexData.getMarketTotalAmount())
        );

        return indexDataMapper.toDto(indexData);
    }

    @Override
    @Transactional
    public void deleteIndexData(Long id) {
        if (!indexDataRepository.existsById(id)) {
            throw new EntityNotFoundException("해당 ID의 주가 데이터를 찾을 수 없습니다: " + id);
        }
        indexDataRepository.deleteById(id);
    }

    /**
     * 🎯 완전히 새로운 커서 기반 페이지네이션 구현
     * QueryDSL Slice 패턴을 활용한 단순하고 안정적인 구현
     */
    @Override
    public CursorPageResponseIndexDataDto searchIndexData(IndexDataSearchCondition condition) {
        log.debug("Starting search with condition - cursor: {}, sortField: {}, sortDirection: {}, size: {}", 
                 condition.cursor(), condition.sortField(), condition.sortDirection(), condition.size());

        // 1. QueryDSL Slice 패턴으로 데이터 조회
        Slice<IndexData> slice = indexDataRepository.findSlice(condition);
        
        // 2. 엔티티 → DTO 변환
        List<IndexDataDto> content = slice.getContent().stream()
                .map(indexDataMapper::toDto)
                .toList();

        // 3. 다음 커서 생성 (간단한 "value_id" 형태)
        String nextCursor = null;
        Long nextIdAfter = null;
        
        if (slice.hasNext() && !content.isEmpty()) {
            IndexDataDto lastItem = content.get(content.size() - 1);
            Object sortValue = extractSortValue(lastItem, condition.sortField());
            
            // 단순한 "sortValue_id" 형태의 커서
            nextCursor = String.format("%s_%d", sortValue.toString(), lastItem.id());
            nextIdAfter = lastItem.id();
            
            log.debug("Generated next cursor: {}, nextIdAfter: {}", nextCursor, nextIdAfter);
        }

        // 4. 전체 개수 조회 (필요한 경우에만)
        long totalElements = indexDataRepository.count(condition);

        CursorPageResponseIndexDataDto response = new CursorPageResponseIndexDataDto(
                content,
                nextCursor,
                nextIdAfter,
                condition.size(),
                totalElements,
                slice.hasNext()
        );

        log.debug("Search completed - returned {} items, hasNext: {}", content.size(), slice.hasNext());
        return response;
    }

    /**
     * DTO에서 정렬 필드 값을 추출
     */
    private Object extractSortValue(IndexDataDto dto, String sortField) {
        return switch (sortField) {
            case "baseDate" -> dto.baseDate();
            case "marketPrice" -> dto.marketPrice();
            case "closingPrice" -> dto.closingPrice();
            case "highPrice" -> dto.highPrice();
            case "lowPrice" -> dto.lowPrice();
            case "versus" -> dto.versus();
            case "fluctuationRate" -> dto.fluctuationRate();
            case "tradingQuantity" -> dto.tradingQuantity();
            case "tradingPrice" -> dto.tradingPrice();
            case "marketTotalAmount" -> dto.marketTotalAmount();
            default -> dto.id(); // fallback
        };
    }

    /**
     * 업데이트가 필요한지 확인
     */
    private boolean isUpdateNeeded(IndexDataUpdateRequest request, IndexData indexData) {
        if (request.marketPrice() != null && indexData.getMarketPrice().compareTo(request.marketPrice()) != 0) return true;
        if (request.closingPrice() != null && indexData.getClosingPrice().compareTo(request.closingPrice()) != 0) return true;
        if (request.highPrice() != null && indexData.getHighPrice().compareTo(request.highPrice()) != 0) return true;
        if (request.lowPrice() != null && indexData.getLowPrice().compareTo(request.lowPrice()) != 0) return true;
        if (request.versus() != null && indexData.getVersus().compareTo(request.versus()) != 0) return true;
        if (request.fluctuationRate() != null && indexData.getFluctuationRate().compareTo(request.fluctuationRate()) != 0) return true;
        if (request.tradingQuantity() != null && !indexData.getTradingQuantity().equals(request.tradingQuantity())) return true;
        if (request.tradingPrice() != null && !indexData.getTradingPrice().equals(request.tradingPrice())) return true;
        return request.marketTotalAmount() != null && !indexData.getMarketTotalAmount().equals(request.marketTotalAmount());
    }

    @Override
    @Transactional(readOnly = true)
    public void exportIndexDataToCsv(Writer writer, IndexDataSearchCondition condition) {
        List<IndexData> indexDataList = indexDataRepository.findAllByCondition(condition);
        List<IndexDataDto> indexDataDtoList = indexDataMapper.toDtoList(indexDataList);

        String[] headers = {"baseDate", "marketPrice", "closingPrice", "highPrice", "lowPrice", 
                           "versus", "fluctuationRate", "tradingQuantity", "tradingPrice", "marketTotalAmount"};

        try (CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(headers))) {
            for (IndexDataDto dto : indexDataDtoList) {
                csvPrinter.printRecord(
                        dto.baseDate(),
                        dto.marketPrice(),
                        dto.closingPrice(),
                        dto.highPrice(),
                        dto.lowPrice(),
                        dto.versus(),
                        dto.fluctuationRate(),
                        dto.tradingQuantity(),
                        dto.tradingPrice(),
                        dto.marketTotalAmount()
                );
            }
        } catch (IOException e) {
            throw new RuntimeException("CSV export failed", e);
        }
    }
}