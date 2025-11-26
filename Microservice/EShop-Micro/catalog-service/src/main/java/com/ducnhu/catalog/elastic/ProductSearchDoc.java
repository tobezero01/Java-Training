package com.ducnhu.catalog.elastic;

import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.elasticsearch.annotations.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "products")
@Setting(settingPath = "/es/settings-products.json")   // khai báo analyzer/ngram (file resources)
@Mapping(mappingPath = "/es/mapping-products.json")
public class ProductSearchDoc {
    @Id
    private Integer id;

    @Field(type = FieldType.Keyword)
    private String alias;       // tra cứu chính xác (term)

    @Field(type = FieldType.Text, analyzer = "alias_ngram", searchAnalyzer = "standard")
    private String aliasNgram;  // phục vụ prefix/contains nếu muốn mở rộng

    @Field(type = FieldType.Text, analyzer = "standard")
    private String name;

    @Field(type = FieldType.Keyword)
    private String categoryName;
}
