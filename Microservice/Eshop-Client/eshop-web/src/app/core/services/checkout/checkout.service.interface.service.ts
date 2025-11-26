import { Observable } from "rxjs";
import { CheckoutSummary } from "../../../features/checkout/models/checkout-summary.model";
import { PlaceOrderRequest } from "../../../features/checkout/models/place-order-request.model";
import { PlaceOrderResponse } from "../../../features/checkout/models/place-order-response.model";

export interface ICheckoutService {
  summary(addressId?: number): Observable<CheckoutSummary>;
  placeOrderCod(req: PlaceOrderRequest): Observable<PlaceOrderResponse>;
  cancelOrder(orderNumber: string, reason?: string): Observable<any>;
}
