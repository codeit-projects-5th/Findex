package com.codeit.findex.repository.custom;

import com.codeit.findex.dto.request.SyncJobSearchRequest;
import com.codeit.findex.entity.SyncJob;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class SyncJobRepositoryImpl implements SyncJobRepositoryCustom {

    private final EntityManager em;

    @Override
    public List<SyncJob> search(SyncJobSearchRequest param) {

        StringBuilder jpqlBuilder = new StringBuilder("SELECT s FROM SyncJob s WHERE 1=1");

        if (param.jobType() != null) jpqlBuilder.append(" AND s.jobType = :jobType");
        if (param.indexInfoId() != null) jpqlBuilder.append(" AND s.indexInfo.id = :indexInfoId");
        if (param.baseDateFrom() != null) jpqlBuilder.append(" AND s.targetDate >= :baseDateFrom");
        if (param.baseDateTo() != null) jpqlBuilder.append(" AND s.targetDate <= :baseDateTo");
        if (param.worker() != null) jpqlBuilder.append(" AND s.worker = :worker");
        if (param.jobTimeFrom() != null) jpqlBuilder.append(" AND s.jobTime >= :jobTimeFrom");
        if (param.jobTimeTo() != null) jpqlBuilder.append(" AND s.jobTime <= :jobTimeTo");
        if (param.status() != null) jpqlBuilder.append(" AND s.result = :status");
        if (param.idAfter() != null) jpqlBuilder.append(" AND s.id > :idAfter");

        // 정렬
        String sortField = param.sortField();
        String sortDirection = param.sortDirection().equalsIgnoreCase("asc") ? "ASC" : "DESC";
        jpqlBuilder.append(" ORDER BY s.").append(sortField).append(" ").append(sortDirection);

        TypedQuery<SyncJob> query = em.createQuery(jpqlBuilder.toString(), SyncJob.class);

        if (param.jobType() != null) query.setParameter("jobType", param.jobType());
        if (param.indexInfoId() != null) query.setParameter("indexInfoId", param.indexInfoId());
        if (param.baseDateFrom() != null) query.setParameter("startDate", param.baseDateFrom());
        if (param.baseDateTo() != null) query.setParameter("endDate", param.baseDateTo());
        if (param.worker() != null) query.setParameter("worker", param.worker());
        if (param.jobTimeFrom() != null) query.setParameter("jobTimeFrom", param.jobTimeFrom());
        if (param.jobTimeTo() != null) query.setParameter("jobTimeTo", param.jobTimeTo());
        if (param.status() != null) query.setParameter("status", param.status());
        if (param.idAfter() != null) query.setParameter("idAfter", param.idAfter());

        query.setMaxResults(param.size() + 1); // 페이지네이션 (다음 페이지 여부 확인용)
        return query.getResultList();
    }

    @Override
    public long count(SyncJobSearchRequest param) {

        StringBuilder jpqlBuilder = new StringBuilder("SELECT count(d) FROM SyncJob d WHERE 1=1");

        if(param.indexInfoId() != null) jpqlBuilder.append(" AND s.indexInfo.id = :indexInfoId");
        if(param.baseDateFrom() != null) jpqlBuilder.append(" AND s.targetDate >= :baseDateFrom");
        if(param.baseDateTo() != null) jpqlBuilder.append(" AND s.targetDate <= :baseDateTo");

        // 최종 쿼리
        TypedQuery<Long> query = em.createQuery(jpqlBuilder.toString(), Long.class);

        if(param.indexInfoId() != null) query.setParameter("indexInfoId", param.indexInfoId());
        if(param.baseDateFrom() != null) query.setParameter("startDate", param.baseDateFrom());
        if(param.baseDateTo() != null) query.setParameter("endDate", param.baseDateTo());

        return query.getSingleResult();
    }
}
