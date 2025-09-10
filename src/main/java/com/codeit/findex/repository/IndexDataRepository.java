package com.codeit.findex.repository;

import com.codeit.findex.entity.IndexData;
import com.codeit.findex.repository.custom.IndexDataRepositoryCustom;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IndexDataRepository extends JpaRepository<IndexData, Long>, IndexDataRepositoryCustom {

    boolean existsByIndexInfoIdAndBaseDate(Long indexInfoId, LocalDate baseDate);
}