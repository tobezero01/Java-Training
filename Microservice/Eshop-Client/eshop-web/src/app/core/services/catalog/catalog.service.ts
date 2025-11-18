import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { environment } from '../../../../environments/environment.development';
import { firstValueFrom } from 'rxjs';
import { CategoryNodeDto } from '../../../features/catalog/models/category-node-dto.model';
import { API } from '../../constants/api-endpoints';
import { CategoryDto } from '../../../features/catalog/models/category-dto.model';
import { ProductDto } from '../../../features/catalog/models/product-dto.model';
import { PageResponse } from '../../../features/catalog/models/page-response.model';

@Injectable({
  providedIn: 'root'
})
export class CatalogService {
  private http = inject(HttpClient);
  private base = environment.baseGateway;

  // CATEGORIES
  getCategoryTree(): Promise<CategoryNodeDto[]> {
    return firstValueFrom(this.http.get<CategoryNodeDto[]>(`${this.base}${API.CATEGORIES.TREE}`));
  }
  getTopCategories(): Promise<CategoryDto[]> {
    return firstValueFrom(this.http.get<CategoryDto[]>(`${this.base}${API.CATEGORIES.TOP}`));
  }
  getLeafCategories(): Promise<CategoryDto[]> {
    return firstValueFrom(this.http.get<CategoryDto[]>(`${this.base}${API.CATEGORIES.LEAF}`));
  }
  getCategoryByAlias(alias: string): Promise<CategoryDto> {
    return firstValueFrom(this.http.get<CategoryDto>(`${this.base}${API.CATEGORIES.BY_ALIAS(alias)}`));
  }
  getCategoryParents(id: number): Promise<CategoryDto[]> {
    return firstValueFrom(this.http.get<CategoryDto[]>(`${this.base}${API.CATEGORIES.PARENTS(id)}`));
  }

  // PRODUCTS
  getProductById(id: number): Promise<ProductDto> {
    return firstValueFrom(this.http.get<ProductDto>(`${this.base}${API.PRODUCTS.BY_ID(id)}`));
  }
  getProductByAlias(alias: string): Promise<ProductDto> {
    return firstValueFrom(this.http.get<ProductDto>(`${this.base}${API.PRODUCTS.BY_ALIAS(alias)}`));
  }
  listProductsByCategory(catId: number, page = 1, sort = 'name', dir: 'asc'|'desc' = 'asc'): Promise<PageResponse<ProductDto>> {
    return firstValueFrom(this.http.get<PageResponse<ProductDto>>(`${this.base}${API.PRODUCTS.BY_CAT(catId, page, sort, dir)}`));
  }
  searchProducts(keyword: string, page = 1): Promise<PageResponse<ProductDto>> {
    return firstValueFrom(this.http.get<PageResponse<ProductDto>>(`${this.base}${API.PRODUCTS.SEARCH(keyword, page)}`));
  }
}
