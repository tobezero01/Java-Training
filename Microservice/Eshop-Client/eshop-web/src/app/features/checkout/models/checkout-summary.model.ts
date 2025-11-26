import { CheckoutItem } from "./checkout-item.model";

export interface CheckoutSummary {
  items: CheckoutItem[];
  productTotal: number;
  shippingCost: number;
  paymentTotal: number;
  shippingSupported: boolean;   // dùng để bật/tắt COD
  addressId?: number;
  addressLine?: string;
}
