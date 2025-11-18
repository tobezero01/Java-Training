export interface ProductDto {
  id: number;
  name: string;
  alias: string;
  shortDescription?: string;
  fullDescription?: string;
  price: number;
  discountPrice?: number;
  mainImagePath?: string;
  averageRating?: number;
  reviewCount?: number;
  categoryId?: number;
  categoryName?: string;
  inStock?: boolean;
}
