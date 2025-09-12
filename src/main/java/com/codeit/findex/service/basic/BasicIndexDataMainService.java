package com.codeit.findex.service.basic;

import com.codeit.findex.dto.data.*;
import com.codeit.findex.repository.IndexDataMainRepository;
import com.codeit.findex.service.IndexDataMainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BasicIndexDataMainService implements IndexDataMainService {

    private final IndexDataMainRepository indexDataMainRepository;

//    @Override
//    public List<IndexDataFavoriteDto> searchIndexDataFavorite(IndexDataFavoriteDto favoriteDto) {
//        List<IndexDataFavoriteDto> favoriteLIst = indexDataMainRepository.searchIndexDataFavorite(favoriteDto);
//
//        return favoriteLIst;
//    }

    @Override
    public List<IndexDataRankDto> searchIndexDataRank(IndexDataRankDto rankDto) {

        List<IndexDataPerformanceDto> performanceList = indexDataMainRepository.searchIndexDataRank(rankDto);
        List<IndexDataRankDto> result = new ArrayList<IndexDataRankDto>();

        int rank = 1;
        for (IndexDataPerformanceDto row : performanceList) {
            IndexDataPerformanceDto perf = new IndexDataPerformanceDto(
                    row.indexInfoId(),
                    row.indexClassification(),
                    row.indexName(),
                    row.versus(),
                    row.fluctuationRate(),
                    row.currentPrice(),
                    row.beforePrice()
            );
            result.add(new IndexDataRankDto(perf, rank++));
        }
        return result;
    }

    @Override
    public IndexDataChartDto searchIndexDataChart(IndexDataChartDto chartDto){
        List<IndexDataChartValueDto>     dataPoints     = indexDataMainRepository.searchIndexDataChart(chartDto);
        List<IndexDataChartValueMa5Dto>  dataPointsMa5  = indexDataMainRepository.searchIndexDataChartMa5(chartDto);
        List<IndexDataChartValueMa28Dto> dataPointsMa28 = indexDataMainRepository.searchIndexDataChartMa28(chartDto);

        Long indexInfoId            = 2L;
        String indexClassification  = "";
        String indexName            = "2";
        String periodType           = "MONTHLY";

        return new IndexDataChartDto(
                indexInfoId,
                indexClassification,
                indexName,
                periodType,
                dataPoints,
                dataPointsMa5,
                dataPointsMa28
        );
    }

}
