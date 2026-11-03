package com.Nbbang.backend.domain.bookreport.repository;

import com.Nbbang.backend.domain.bookreport.entity.DepartmentBookAnalysis;
import com.Nbbang.backend.domain.product.entity.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DepartmentBookAnalysisRepository extends JpaRepository<DepartmentBookAnalysis, Long> {
    Optional<DepartmentBookAnalysis> findByCandidateIdAndCategoryAndSourceHashAndPromptVersionAndKeywordConfigVersion(
            Long candidateId, ProductCategory category, String sourceHash, String promptVersion,
            String keywordConfigVersion);

    List<DepartmentBookAnalysis> findAllByIdIn(List<Long> ids);
}
