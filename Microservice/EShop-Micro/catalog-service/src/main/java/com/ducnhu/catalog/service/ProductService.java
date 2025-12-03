package com.ducnhu.catalog.service;

import com.ducnhu.catalog.dto.ProductDTO;
import com.ducnhu.common.dto.PageResponse;
import com.ducnhu.common.exception.ProductNotFoundException;

import java.util.List;

public interface ProductService {
    //PageResponse<ProductDTO> listByCategory(Integer categoryId);
    List<ProductDTO> listByCategoryNoPaging(Integer categoryId);
    PageResponse<ProductDTO> listByCategory(Integer categoryId, int page, String sort, String dir);
    ProductDTO getProduct(String alias) throws ProductNotFoundException;
    ProductDTO getProduct(Integer id)    throws ProductNotFoundException;

    PageResponse<ProductDTO> search(String keyWord, int pageNum, int size);    PageResponse<ProductDTO> listByCategoryPaged(Integer categoryId, int page, int size, String sort, String dir);
    PageResponse<ProductDTO> listFeaturedProducts(String type, int page, int size);
    // Purchased products: cũng trả DTO để đồng nhất
    //PageResponse<ProductDTO> getPurchasedProducts(Integer customerId, int page, String sort, String dir);
}
