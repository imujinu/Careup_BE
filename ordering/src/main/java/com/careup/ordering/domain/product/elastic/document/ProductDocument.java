package com.careup.ordering.domain.product.elastic.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Mapping;
import org.springframework.data.elasticsearch.annotations.Setting;

@Document(indexName = "products", createIndex = true)
@Setting(settingPath = "elastic/product-index-settings.json")
@Mapping(mappingPath = "elastic/product-index-mappings.json")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDocument {

    // Elasticsearch _id 로 사용 */
    @Id
    private String id;

    // 비즈니스 PK(상품 PK)는 별도 필드로 저장 */
    @Field(type = FieldType.Long)
    private Long productId;

    @Field(type = FieldType.Text, analyzer = "name_analyzer")
    private String name;

    @Field(type = FieldType.Text, analyzer = "korean_analyzer")
    private String description;

    @Field(type = FieldType.Text, analyzer = "korean_analyzer")
    private String categoryName;

    @Field(type = FieldType.Long)
    private Long categoryId;

    @Field(type = FieldType.Long)
    private Long price;

    @Field(type = FieldType.Keyword)
    private String imageUrl;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Keyword)
    private String visibility;
}