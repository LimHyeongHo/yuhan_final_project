package com.Nbbang.backend.domain.bookreport.config;

import com.Nbbang.backend.domain.product.entity.ProductCategory;
import org.springframework.beans.factory.config.YamlMapFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class DepartmentBookKeywordCatalog {

    private final String version;
    private final Map<ProductCategory, DepartmentDefinition> definitions;

    public DepartmentBookKeywordCatalog() {
        YamlMapFactoryBean factory = new YamlMapFactoryBean();
        factory.setResources(new ClassPathResource("department-book-keywords.yml"));
        Map<String, Object> root = factory.getObject();
        if (root == null) {
            throw new IllegalStateException("Department book keyword configuration is missing");
        }
        this.version = requireText(root.get("version"), "version");
        this.definitions = loadDefinitions(root.get("departments"));
        validateCoverage();
    }

    public String version() {
        return version;
    }

    public Set<ProductCategory> categories() {
        return Set.copyOf(definitions.keySet());
    }

    public DepartmentDefinition get(ProductCategory category) {
        DepartmentDefinition definition = definitions.get(category);
        if (definition == null) {
            throw new IllegalArgumentException("Unsupported department category: " + category);
        }
        return definition;
    }

    @SuppressWarnings("unchecked")
    private Map<ProductCategory, DepartmentDefinition> loadDefinitions(Object rawDepartments) {
        if (!(rawDepartments instanceof Map<?, ?> departments)) {
            throw new IllegalStateException("departments must be configured");
        }
        Map<ProductCategory, DepartmentDefinition> loaded = new EnumMap<>(ProductCategory.class);
        for (Map.Entry<?, ?> entry : departments.entrySet()) {
            ProductCategory category = ProductCategory.valueOf(String.valueOf(entry.getKey()));
            if (!(entry.getValue() instanceof Map<?, ?> definition)) {
                throw new IllegalStateException("Invalid department configuration: " + category);
            }
            String name = requireText(definition.get("name"), category + ".name");
            Object rawKeywords = definition.get("keywords");
            if (!(rawKeywords instanceof List<?> keywordList)) {
                throw new IllegalStateException("Keywords must be a list: " + category);
            }
            List<String> keywords = keywordList.stream()
                    .map(String::valueOf)
                    .map(String::trim)
                    .filter(keyword -> !keyword.isBlank())
                    .toList();
            if (keywords.size() < 1 || keywords.size() > 3
                    || new LinkedHashSet<>(keywords).size() != keywords.size()) {
                throw new IllegalStateException("Department must have 1-3 unique keywords: " + category);
            }
            loaded.put(category, new DepartmentDefinition(name, keywords));
        }
        return Map.copyOf(loaded);
    }

    private void validateCoverage() {
        Set<ProductCategory> expected = EnumSet.allOf(ProductCategory.class);
        expected.remove(ProductCategory.FREE_MAJOR);
        expected.remove(ProductCategory.GENERAL);
        if (!definitions.keySet().equals(expected)) {
            throw new IllegalStateException("Department keyword coverage must match 37 product categories");
        }
    }

    private String requireText(Object value, String field) {
        String text = value == null ? "" : String.valueOf(value).trim();
        if (text.isBlank()) {
            throw new IllegalStateException("Missing department book keyword field: " + field);
        }
        return text;
    }

    public record DepartmentDefinition(String name, List<String> keywords) {
        public DepartmentDefinition {
            keywords = List.copyOf(keywords);
        }
    }
}
