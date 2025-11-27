import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { OrderSummary } from '../../../features/orders/models/order-summary.model';
import { OrderDetail } from '../../../features/orders/models/order-detail.model';
import { PageResponse } from '../../../features/catalog/models/page-response.model';
import { API } from '../../constants/api-endpoints';

@Injectable({
  providedIn: 'root'
})
export class OrdersService {
  private http = inject(HttpClient);
  private base = environment.baseGateway;

  list(page = 1): Observable<PageResponse<OrderSummary>> {
    return this.http.get<PageResponse<OrderSummary>>(
      this.base + API.ORDERS.MY_ORDERS
    );
  }

  get(orderNumber: string): Observable<OrderDetail> {
    return this.http.get<OrderDetail>(
      this.base + API.ORDERS.DETAIL(orderNumber)
    );
  }
}
