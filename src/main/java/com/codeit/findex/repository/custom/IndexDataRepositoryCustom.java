package com.codeit.findex.repository.custom;

import com.codeit.findex.dto.request.IndexDataSearchCondition;
import com.codeit.findex.entity.IndexData;
import org.springframework.data.domain.Slice;

import java.util.List;

public interface IndexDataRepositoryCustom {

    /**
     * 새로운 QueryDSL 기반 Slice 페이지네이션
     */
    Slice<IndexData> findSlice(IndexDataSearchCondition condition);

    /**
     * 기존 메서드들 (호환성 유지)
     */
    List<IndexData> search(IndexDataSearchCondition condition);

    long count(IndexDataSearchCondition condition);

    List<IndexData> findAllByCondition(IndexDataSearchCondition condition);

    void saveAllInBatch(List<IndexData> indexDataList, Long indexInfoId);
}
