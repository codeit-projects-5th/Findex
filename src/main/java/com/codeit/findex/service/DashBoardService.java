package com.codeit.findex.service;

import com.codeit.findex.dto.response.IndexDataRank;
import com.codeit.findex.dto.response.MajorIndexDataResponse;

import java.util.List;

public interface DashBoardService {
    List<MajorIndexDataResponse> getMajorIndex(String periodType);
    List<IndexDataRank> getIndexPerformance(String periodType, int limit);
}
