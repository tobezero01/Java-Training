import { CategoryDto } from "../../../features/catalog/models/category-dto.model";
import { CategoryNodeDto } from "../../../features/catalog/models/category-node-dto.model";
import { PageResponse } from "../../../features/catalog/models/page-response.model";
import { ProductDto } from '../../../features/catalog/models/product-dto.model';

export interface ICatalogService {
  getCategoryTree(): Promise<CategoryNodeDto[]>;
  getTopCategories(): Promise<CategoryDto[]>;
  getLeafCategories(): Promise<CategoryDto[]>;
  getCategoryByAlias(alias: string): Promise<CategoryDto>;
  getCategoryParents(id: number): Promise<CategoryDto[]>;

  // Products
  getProductById(id: number): Promise<ProductDto>;
  getProductByAlias(alias: string): Promise<ProductDto>;
  listProductsByCategory(catId: number, page?: number, sort?: string, dir?: 'asc'|'desc'): Promise<PageResponse<ProductDto>>;
  searchProducts(keyword: string, page?: number): Promise<PageResponse<ProductDto>>;

}
