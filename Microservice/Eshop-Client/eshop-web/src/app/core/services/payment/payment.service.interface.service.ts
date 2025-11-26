import { Observable } from "rxjs";
import { PaypalCaptureResponse } from "../../../features/checkout/models/paypal-capture-response.model";
import { PaypalCreateResponse } from "../../../features/checkout/models/paypal-create-response.model";

export interface IPaymentService {
  paypalCreate(addressId: number, returnUrl: string, cancelUrl: string): Observable<PaypalCreateResponse>;
  paypalCapture(paypalOrderId: string, orderNumber: string): Observable<PaypalCaptureResponse>;
  paypalCancel(orderNumber: string, paypalOrderId?: string, reason?: string): Observable<any>;
}
