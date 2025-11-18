import { routes } from './../../../../app.routes';
import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CatalogService } from '../../../../core/services/catalog/catalog.service';
import { ProductDto } from '../../models/product-dto.model';
import { CategoryDto } from '../../models/category-dto.model';
import { ToastrService } from 'ngx-toastr';
import { CartService } from '../../../../core/services/cart/cart.service';

@Component({
  selector: 'app-product-detail',
  standalone : true,
  imports: [CommonModule, RouterLink],
  templateUrl: './product-detail.component.html',
  styleUrl: './product-detail.component.css'
})
export class ProductDetailComponent implements OnInit {
  private srv = inject(CatalogService);
  private ar = inject(ActivatedRoute);
  private toastr = inject(ToastrService);
  private router = inject(Router);
  private cart = inject(CartService);

  p = signal<ProductDto | null>(null);
  crumbs = signal<CategoryDto[]>([]);

  async ngOnInit() {
    const alias = this.ar.snapshot.paramMap.get('alias')!;
    const prod = await this.srv.getProductByAlias(alias);
    this.p.set(prod);

    if (prod.categoryId) {
      const parents = await this.srv.getCategoryParents(prod.categoryId);
      this.crumbs.set(parents);
    }
  }

  addToCart(prodId: number, qty = 1) {
    if (qty < 1) qty = 1;
    if (qty > 10) qty = 10; // clamp UI; server cũng clamp
    this.cart.addItem({ productId: prodId, quantity: qty }).subscribe({
    next: () => this.toastr.success('Đã thêm vào giỏ'),
    });
  }

  buyNow() {
    // TODO: checkout
    this.toastr.info('Chức năng mua ngay sẽ được nối với Checkout ở module tiếp theo.');
    // Ví dụ sau này: this.router.navigate(['/checkout'], { queryParams: { productId: this.p()?.id }});
  }
}
