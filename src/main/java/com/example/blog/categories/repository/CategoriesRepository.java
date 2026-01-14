package com.example.blog.categories.repository;

import com.example.blog.categories.entity.Categories;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoriesRepository extends JpaRepository<Categories, Long> {
    Optional<Categories> findByCategoriesName(String categoriesName);
}

