import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom, Observable } from 'rxjs';
import { CartGetResponseView } from '../../../features/cart/models/cart-get-response-view.model';
import { API } from '../../constants/api-endpoints';
import { AddItemRequest } from '../../../features/cart/models/add-cart-item.model';
import { CartActionResp } from '../../../features/cart/models/cart-action-resp.model';
import { UpdateQuantityRequest } from '../../../features/cart/models/update-quantity-request.model';

@Injectable({
  providedIn: 'root'
})
export class CartService {

  private http = inject(HttpClient);

  getCart(): Observable<CartGetResponseView> {
    return (this.http.get<CartGetResponseView>(API.CART.ITEMS));
  }
  addItem(req: AddItemRequest): Observable<CartActionResp> {
    return this.http.post<CartActionResp>(API.CART.ITEMS, req);
  }
  updateQty(productId: number, req: UpdateQuantityRequest): Observable<CartActionResp> {
    return this.http.patch<CartActionResp>(`${API.CART.ITEMS}/${productId}`, req);
  }
  remove(productId: number): Observable<CartActionResp> {
    return this.http.delete<CartActionResp>(`${API.CART.ITEMS}/${productId}`);
  }
  clear(): Observable<CartActionResp> {
    return this.http.delete<CartActionResp>(API.CART.ROOT);
  }
}
