package com.Nbbang.backend.domain.bookreport.config;

import com.Nbbang.backend.domain.product.entity.ProductCategory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DepartmentBookKeywordCatalogTest {

    @Test
    void loadsExactlyTheThirtySevenReportDepartments() {
        DepartmentBookKeywordCatalog catalog = new DepartmentBookKeywordCatalog();

        assertThat(catalog.categories()).hasSize(37)
                .doesNotContain(ProductCategory.FREE_MAJOR, ProductCategory.GENERAL);
        assertThat(catalog.categories())
                .allSatisfy(category -> assertThat(catalog.get(category).keywords()).hasSizeBetween(1, 3));
        assertThat(catalog.version()).isNotBlank();
    }
}
