package com.example.blog.popular.repository;

import com.example.blog.popular.entity.PopularPostDaily;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface PopularPostDailyRepository extends JpaRepository<PopularPostDaily, Long> {

    boolean existsByTargetDate(LocalDate targetDate);

    void deleteByTargetDate(LocalDate targetDate);

    List<PopularPostDaily> findByTargetDateOrderByRankNoAsc(LocalDate targetDate);
}