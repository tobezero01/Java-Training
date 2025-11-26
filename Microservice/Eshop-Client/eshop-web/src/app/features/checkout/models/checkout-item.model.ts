export interface CheckoutItem {
  productId: number;
  name: string;
  alias: string;
  image: string;
  unitPrice: number;   // !
  quantity: number;    // !
  subtotal: number;    // !
}
