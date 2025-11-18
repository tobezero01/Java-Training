import { CartLineView } from "./cart-line-view.model";

export interface CartGetResponseView {
  items: CartLineView[];
  itemCount: number;
  totalQuantity: number;
  totalAmount: number;
}
