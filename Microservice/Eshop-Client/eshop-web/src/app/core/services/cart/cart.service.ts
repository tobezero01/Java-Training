import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom, Observable } from 'rxjs';
import { CartGetResponseView } from '../../../features/cart/models/cart-get-response-view.model';
import { API } from '../../constants/api-endpoints';
import { AddItemRequest } from '../../../features/cart/models/add-cart-item.model';
import { CartActionResp } from '../../../features/cart/models/cart-action-resp.model';
import { UpdateQuantityRequest } from '../../../features/cart/models/update-quantity-request.model';
import { environment } from '../../../../environments/environment.development';

@Injectable({
  providedIn: 'root'
})
export class CartService {

   private http = inject(HttpClient);
  private base = environment.baseGateway;   // http://localhost:8080

  // GET /api/cart/items — lấy giỏ hiện tại
  getCart(): Observable<CartGetResponseView> {
    return this.http.get<CartGetResponseView>(
      `${this.base}${API.CART.VIEW}`
    );
  }

  // POST /api/cart/items — thêm sản phẩm
  addItem(req: AddItemRequest): Observable<CartActionResp> {
    return this.http.post<CartActionResp>(
      `${this.base}${API.CART.VIEW}`,
      req
    );
  }

  // PATCH /api/cart/items/{productId} — cập nhật số lượng
  updateQty(productId: number, req: UpdateQuantityRequest): Observable<CartActionResp> {
    return this.http.patch<CartActionResp>(
      `${this.base}${API.CART.UPDATE_QTY(productId)}`,
      req
    );
  }

  // DELETE /api/cart/items/{productId} — xóa 1 dòng
  remove(productId: number): Observable<CartActionResp> {
    return this.http.delete<CartActionResp>(
      `${this.base}${API.CART.REMOVE_ITEM(productId)}`
    );
  }

  // DELETE /api/cart — xóa toàn bộ giỏ
  clear(): Observable<CartActionResp> {
    return this.http.delete<CartActionResp>(
      `${this.base}${API.CART.CLEAR}`
    );
  }
}
