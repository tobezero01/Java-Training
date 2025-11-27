import { OrderItem } from './order-item.model';

export interface OrderDetail {
  orderNumber: string;
  orderTime: string;
  status: string;
  paymentMethod: string;
  productTotal: number;
  shippingCost: number;
  paymentTotal: number;
  shippingAddress: string;
  items: OrderItem[];
}
