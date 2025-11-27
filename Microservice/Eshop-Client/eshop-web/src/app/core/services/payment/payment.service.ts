import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { IPaymentService } from './payment.service.interface.service';
import { PaypalCreateResponse } from '../../../features/checkout/models/paypal-create-response.model';
import { API } from '../../constants/api-endpoints';
import { Observable } from 'rxjs';
import { PaypalCaptureResponse } from '../../../features/checkout/models/paypal-capture-response.model';
import { environment } from '../../../../environments/environment.development';

@Injectable({ providedIn: 'root' })
export class PaymentService implements IPaymentService {
  private http = inject(HttpClient);
  private base = environment.baseGateway;   // ví dụ: http://localhost:8080

  paypalCreate(addressId: number, returnUrl: string, cancelUrl: string): Observable<PaypalCreateResponse> {
    const body = new HttpParams()
      .set('addressId', String(addressId))
      .set('returnUrl', returnUrl)
      .set('cancelUrl', cancelUrl);

    const headers = new HttpHeaders({ 'Content-Type': 'application/x-www-form-urlencoded' });

    // POST http://localhost:8080/api/payments/paypal/create
    return this.http.post<PaypalCreateResponse>(
      `${this.base}${API.PAYMENTS.PAYPAL.CREATE}`,
      body.toString(),
      { headers }
    );
  }

  paypalCapture(paypalOrderId: string, orderNumber: string): Observable<PaypalCaptureResponse> {
    const body = new HttpParams()
      .set('paypalOrderId', paypalOrderId)
      .set('orderNumber', orderNumber);

    const headers = new HttpHeaders({ 'Content-Type': 'application/x-www-form-urlencoded' });

    // POST http://localhost:8080/api/payments/paypal/capture
    return this.http.post<PaypalCaptureResponse>(
      `${this.base}${API.PAYMENTS.PAYPAL.CAPTURE}`,
      body.toString(),
      { headers }
    );
  }

  paypalCancel(
    orderNumber: string,
    paypalOrderId?: string,
    reason = 'Buyer cancelled at PayPal'
  ): Observable<any> {
    const body = new HttpParams()
      .set('orderNumber', orderNumber)
      .set('reason', reason)
      .set('paypalOrderId', paypalOrderId || '');

    const headers = new HttpHeaders({ 'Content-Type': 'application/x-www-form-urlencoded' });

    // POST http://localhost:8080/api/payments/paypal/cancel
    return this.http.post<any>(
      `${this.base}${API.PAYMENTS.PAYPAL.CANCEL}`,
      body.toString(),
      { headers }
    );
  }
}
