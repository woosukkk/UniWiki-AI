package com.uniwiki.repository;

import com.uniwiki.entity.OfficialSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OfficialSourceRepository extends JpaRepository<OfficialSource, Long> {
    boolean existsByName(String name);
    List<OfficialSource> findAllByOrderByNameAsc();
    List<OfficialSource> findByActiveTrueOrderByIdAsc();
}
