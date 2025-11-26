export interface PlaceOrderRequest {
  addressId: number;
  paymentMethod: string;  // "COD" (server sẽ ép COD)
  note?: string;
}
