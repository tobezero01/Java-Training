package com.ducnhu.catalog.repository;

import com.ducnhu.catalog.elastic.ProductSearchDoc;
import com.ducnhu.catalog.entity.product.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;
import java.util.Optional;

public interface ProductSearchRepository extends ElasticsearchRepository<ProductSearchDoc, Integer> {
    Optional<ProductSearchDoc> findByAlias(String alias);

    @Query("""
                 {
                   "multi_match": {
                     "query": "?0",
                     "fields": ["name^3", "alias^2", "aliasNgram", "categoryName"],\s
                     "fuzziness": "AUTO"
                   }
                 }
            \s""")
    Page<ProductSearchDoc> searchByKeyword(String query, Pageable pageable);

}

