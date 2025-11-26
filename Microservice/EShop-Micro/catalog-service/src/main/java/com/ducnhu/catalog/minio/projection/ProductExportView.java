package com.ducnhu.catalog.minio.projection;

import java.util.Date;

public interface ProductExportView {
    Integer getId();
    String getName();
    String getAlias();
    Float getPrice();
    Float getDiscountPrice();
    Boolean getInStock();
    Float getAverageRating();
    Integer getReviewCount();
    Date getCreatedTime();
    String getCategoryName(); // lấy qua join
}
