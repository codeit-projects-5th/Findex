package com.codeit.findex.repository;

import com.codeit.findex.entity.SyncJob;
import com.codeit.findex.repository.custom.SyncJobRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SyncJobRepository extends JpaRepository<SyncJob, Long>, SyncJobRepositoryCustom {
    Optional<SyncJob> findTopByOrderByJobTimeDesc();
}
