export interface OrderSummary {
  id?: number;              // nếu backend trả, dùng, còn không có cũng được
  orderNumber: string;
  orderTime: string;       // ISO string
  status: string;
  paymentMethod: string;
  totalItems: number;
  productTotal: number;
  shippingCost: number;
  paymentTotal: number;
}
