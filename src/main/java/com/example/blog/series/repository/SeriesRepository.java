package com.example.blog.series.repository;

import com.example.blog.series.entity.Series;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SeriesRepository extends JpaRepository<Series, Long> {
    Optional<Series> findByIdAndOwner_UserId(Long id, Long userId);

    List<Series> findAllByOwner_UserIdOrderByIdDesc(Long userId);

}

