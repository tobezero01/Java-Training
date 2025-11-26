export interface PlaceOrderResponse {
  success: boolean;
  orderNumber: string;
  productTotal: number;
  shippingCost: number;
  paymentTotal: number;
}
