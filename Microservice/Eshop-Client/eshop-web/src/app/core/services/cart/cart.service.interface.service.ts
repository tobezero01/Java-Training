import { Observable } from "rxjs";
import { CartGetResponseView } from "../../../features/cart/models/cart-get-response-view.model";
import { AddItemRequest } from "../../../features/cart/models/add-cart-item.model";
import { CartActionResp } from "../../../features/cart/models/cart-action-resp.model";
import { UpdateQuantityRequest } from "../../../features/cart/models/update-quantity-request.model";

export interface ICartService {
  getCart(): Observable<CartGetResponseView>;
  addItem(req: AddItemRequest): Observable<CartActionResp>;
  updateQty(productId: number, req: UpdateQuantityRequest): Observable<CartActionResp>;
  remove(productId: number): Observable<CartActionResp>;
  clear(): Observable<CartActionResp>;
}
