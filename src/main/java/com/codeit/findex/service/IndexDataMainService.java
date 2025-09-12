package com.codeit.findex.service;

import com.codeit.findex.dto.data.*;
import com.codeit.findex.dto.request.IndexDataCreateRequest;
import com.codeit.findex.dto.request.IndexDataSearchCondition;
import com.codeit.findex.dto.request.IndexDataUpdateRequest;

import java.io.Writer;
import java.util.List;

public interface IndexDataMainService {
//    CursorPageResponseIndexDataDto searchIndexData(IndexDataSearchCondition condition);
    IndexDataChartDto searchIndexDataChart(IndexDataChartDto chartDto);

    List<IndexDataRankDto> searchIndexDataRank(IndexDataRankDto rankDto);

//    List<IndexDataFavoriteDto> searchIndexDataFavorite(IndexDataFavoriteDto favoriteDto);
}