import { Component, inject, OnInit } from '@angular/core';
import { CartService } from '../../../../core/services/cart/cart.service';
import { Router } from '@angular/router';
import { NgbModal, NgbModalModule } from '@ng-bootstrap/ng-bootstrap';
import { CommonModule } from '@angular/common';
import { CartGetResponseView } from '../../models/cart-get-response-view.model';
import { CartLineView } from '../../models/cart-line-view.model';
import { AlertModalComponent } from '../../../../shared/modals/alert-modal.component';

@Component({
  selector: 'app-cart',
  standalone: true,
  imports: [CommonModule, NgbModalModule],
  templateUrl: './cart.component.html',
  styleUrl: './cart.component.css'
})
export class CartComponent implements OnInit{

  private cart = inject(CartService);
  private router = inject(Router);
  private modal = inject(NgbModal);

  view?: CartGetResponseView;
  loading = false;

  ngOnInit(): void {
    this.reload();
  }

  reload() {
    this.loading = true;
    this.cart.getCart().subscribe({
      next: (v) => this.view = v,
      error: () => this.loading = false,
      complete: () => this.loading = false
    });
  }

  subtotalOf(cartLine: CartLineView) {
    return (cartLine?.subtotal ?? 0).toFixed(2);
  }

  unitPrice(cartLine: CartLineView) {
    const u = (cartLine.discountPrice && cartLine.discountPrice > 0)
      ? cartLine.discountPrice : (cartLine.price ?? 0);
    return u.toFixed(2);
  }

  dec(cartLine: CartLineView) {
    const next = Math.max(1, (cartLine.quantity ?? 1) - 1);
    if (next !== cartLine.quantity) this.changeQuantity(cartLine, next);
  }

  inc(it: CartLineView) {
    const next = Math.min(10, (it.quantity ?? 1) + 1);  // FE clamp ≤ 10 (server cũng clamp)【7:file_000000008a687207a04c498d734d48f0】
    if (next !== it.quantity) this.changeQuantity(it, next);
    else if (next === 10) this.openWarn('Giới hạn', 'Mỗi sản phẩm mua tối đa 10.');
  }

  onManualChange(it: CartLineView, val: string) {
    const n = Number(val || '1');
    if (Number.isNaN(n) || n < 1) return this.changeQuantity(it, 1);
    if (n > 10) { this.openWarn('Giới hạn', 'Mỗi sản phẩm mua tối đa 10.'); return this.changeQuantity(it, 10); }
    this.changeQuantity(it, n);
  }

  private changeQuantity(it: CartLineView, q: number) {
    this.cart.updateQty(it.productId, { quantity: q }).subscribe({
      next: () => this.reload(),
      error: () => {}
    });
  }

  remove(it: CartLineView) {
    this.cart.remove(it.productId).subscribe({ next: () => this.reload() });
  }

  clear() {
    this.cart.clear().subscribe({ next: () => this.reload() });
  }

  checkout() {
    this.router.navigateByUrl('/checkout');
  }

  private openWarn(title: string, message: string) {
    const ref = this.modal.open(AlertModalComponent, { centered: true });
    ref.componentInstance.title = title;
    ref.componentInstance.message = message;
  }

}
