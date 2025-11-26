import { inject, Injectable } from '@angular/core';
import { ICheckoutService } from './checkout.service.interface.service';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CheckoutSummary } from '../../../features/checkout/models/checkout-summary.model';
import { API } from '../../constants/api-endpoints';
import { PlaceOrderResponse } from '../../../features/checkout/models/place-order-response.model';
import { PlaceOrderRequest } from '../../../features/checkout/models/place-order-request.model';

@Injectable({
  providedIn: 'root'
})
export class CheckoutService implements ICheckoutService {
  private http = inject(HttpClient);

  summary(addressId?: number): Observable<CheckoutSummary> {
    return this.http.get<CheckoutSummary>(API.CHECKOUT.SUMMARY(addressId));
  }

  placeOrderCod(req: PlaceOrderRequest): Observable<PlaceOrderResponse> {
    const body = { ...req, paymentMethod: 'COD' };
    return this.http.post<PlaceOrderResponse>(API.CHECKOUT.PLACE_ORDER, body);
  }

  cancelOrder(orderNumber: string, reason = 'User requested'): Observable<any> {
    return this.http.post(API.CHECKOUT.CANCEL_ORDER(orderNumber, reason), {});
  }
}
