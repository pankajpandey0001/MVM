package com.pankaj.mvm.repository;

import com.pankaj.mvm.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    boolean existsByCode(String code);

    List<Category> findByParentCategoryIsNullAndIsBannedFalse();

    List<Category> findByParentCategoryIdAndIsBannedFalse(Long parentId);

    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.parentCategory WHERE c.isBanned = false ORDER BY c.name ASC")
    List<Category> findAllWithParent();
}