package com.codeit.findex.controller;

import com.codeit.findex.dto.data.*;
import com.codeit.findex.dto.request.IndexDataCreateRequest;
import com.codeit.findex.dto.request.IndexDataSearchCondition;
import com.codeit.findex.dto.request.IndexDataUpdateRequest;
import com.codeit.findex.service.IndexDataMainService;
import com.codeit.findex.service.IndexDataService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")

public class IndexDataMainController {

    private final IndexDataMainService indexDataMainService;

    @GetMapping("/index-data/{id}/chart")
    public ResponseEntity<IndexDataChartDto> searchIndexDataChart(
            @ModelAttribute IndexDataChartDto chartDto) {
        IndexDataChartDto response = indexDataMainService.searchIndexDataChart(chartDto);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/index-data/performance/rank")
    public ResponseEntity<List<IndexDataRankDto>> searchIndexDataRank(
            @ModelAttribute IndexDataRankDto rankDto) {
        return ResponseEntity.ok(indexDataMainService.searchIndexDataRank(rankDto));
    }

//    @GetMapping("/index-data/performance/favorite")
//    public ResponseEntity<IndexDataFavoriteDto> searchIndexDataFavorite(
//            @ModelAttribute IndexDataFavoriteDto favoriteDto) {
//        List<IndexDataFavoriteDto> response = indexDataMainService.searchIndexDataFavorite(favoriteDto);
//        return ResponseEntity.ok(response);
//    }

}
