package com.ducnhu.catalog.repository;

import com.ducnhu.catalog.elastic.ProductSearchDoc;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.Optional;

public interface ProductSearchRepository extends ElasticsearchRepository<ProductSearchDoc, Integer> {
    Optional<ProductSearchDoc> findByAlias(String alias);
}

