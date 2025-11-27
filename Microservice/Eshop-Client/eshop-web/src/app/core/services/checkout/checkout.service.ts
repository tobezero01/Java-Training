import { inject, Injectable } from '@angular/core';
import { ICheckoutService } from './checkout.service.interface.service';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CheckoutSummary } from '../../../features/checkout/models/checkout-summary.model';
import { API } from '../../constants/api-endpoints';
import { PlaceOrderResponse } from '../../../features/checkout/models/place-order-response.model';
import { PlaceOrderRequest } from '../../../features/checkout/models/place-order-request.model';
import { environment } from '../../../../environments/environment.development';

@Injectable({
  providedIn: 'root'
})
export class CheckoutService implements ICheckoutService {
  private http = inject(HttpClient);
  private base = environment.baseGateway;

  summary(addressId?: number): Observable<CheckoutSummary> {
    const path = API.CHECKOUT.SUMMARY(addressId);
    return this.http.get<CheckoutSummary>(`${this.base}${path}`);
  }

  placeOrderCod(req: PlaceOrderRequest): Observable<PlaceOrderResponse> {
    const body = { ...req, paymentMethod: 'COD' };
    return this.http.post<PlaceOrderResponse>(
      `${this.base}${API.CHECKOUT.PLACE_ORDER}`,
      body
    );
  }

  cancelOrder(orderNumber: string, reason = 'User requested'): Observable<void> {
    const path = API.CHECKOUT.CANCEL_ORDER(orderNumber, reason);
    return this.http.post<void>(`${this.base}${path}`, {});
  }
}
