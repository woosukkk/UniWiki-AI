package com.uniwiki.repository;

import com.uniwiki.entity.OfficialSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OfficialSourceRepository extends JpaRepository<OfficialSource, Long> {
    boolean existsByName(String name);
    Optional<OfficialSource> findByName(String name);
    List<OfficialSource> findAllByOrderByNameAsc();
    List<OfficialSource> findByActiveTrueOrderByIdAsc();
}
