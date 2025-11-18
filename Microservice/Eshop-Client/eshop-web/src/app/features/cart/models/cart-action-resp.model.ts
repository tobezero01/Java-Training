export interface CartActionResp {
  productId: number | null;
  quantity: number;
  subtotal: number;
  message: string;
}
