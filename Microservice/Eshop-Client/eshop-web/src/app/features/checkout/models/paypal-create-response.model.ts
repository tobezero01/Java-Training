export interface PaypalCreateResponse {
  paypalOrderId: string;
  approvalUrl: string;
  orderNumber: string;
  amount: number;
  currency: string;
}
