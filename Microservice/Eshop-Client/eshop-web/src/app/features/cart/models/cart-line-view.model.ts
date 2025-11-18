export interface CartLineView {
  productId: number;
  name: string;
  alias: string;
  image: string;
  quantity: number;
  price?: number;
  discountPrice?: number;
  subtotal: number;
}
