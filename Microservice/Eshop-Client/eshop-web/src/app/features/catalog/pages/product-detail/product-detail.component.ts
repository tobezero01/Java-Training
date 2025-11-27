import { routes } from './../../../../app.routes';
import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CatalogService } from '../../../../core/services/catalog/catalog.service';
import { ProductDto } from '../../models/product-dto.model';
import { CategoryDto } from '../../models/category-dto.model';
import { ToastrService } from 'ngx-toastr';
import { CartService } from '../../../../core/services/cart/cart.service';
import { AuthStateService } from '../../../../core/services/auth/auth-state.service';

@Component({
  standalone: true,
  selector: 'app-product-detail',
  imports: [CommonModule, RouterLink],
  templateUrl: './product-detail.component.html',
  styleUrl: './product-detail.component.css'
})
export class ProductDetailComponent implements OnInit {
  private srv = inject(CatalogService);
  private ar = inject(ActivatedRoute);
  private toastr = inject(ToastrService);
  private router = inject(Router);
  private authState = inject(AuthStateService);
  private cartService = inject(CartService);

  p = signal<ProductDto | null>(null);
  crumbs = signal<CategoryDto[]>([]);
  currentImage = signal<string>('assets/image.png');
  extraImages = signal<string[]>([]);

  async ngOnInit() {
    const alias = this.ar.snapshot.paramMap.get('alias')!;
    const prod = await this.srv.getProductByAlias(alias);
    this.p.set(prod);

    const main = prod.mainImagePath ? 'assets' + prod.mainImagePath : 'assets/image.png';
    this.currentImage.set(main);
    this.buildExtraImages(prod);

    if (prod.categoryId) {
      const parents = await this.srv.getCategoryParents(prod.categoryId);
      this.crumbs.set(parents);
    }
  }

  // Task 2: addToCart check login
  addToCart() {
    if (!this.authState.isAuthenticated()) {
      this.toastr.info('Vui lòng đăng nhập để thêm vào giỏ hàng.');
      this.router.navigate(['/login'], { queryParams: { returnUrl: this.router.url } });
      return;
    }

    // TODO: gọi CartService.addItem khi module giỏ hàng đã hoàn thiện
    this.toastr.success('Đã thêm vào giỏ (demo).');
  }

  buyNow() {
    if (!this.authState.isAuthenticated()) {
      this.toastr.info('Vui lòng đăng nhập trước khi mua.');
      this.router.navigate(['/login'], { queryParams: { returnUrl: this.router.url } });
      return;
    }
    // TODO: điều hướng sang checkout khi có logic
    this.router.navigateByUrl('/checkout');
  }

  // Task 3 – phần gallery, description (sẽ dùng trong HTML dưới)
  changeMainImage(url: string) {
    this.currentImage.set(url);
  }

  onMainImgError(event: Event) {
    (event.target as HTMLImageElement).src = 'assets/image.png';
  }

  hideThumbIfError(event: Event) {
    (event.target as HTMLImageElement).style.display = 'none';
  }

  private buildExtraImages(prod: ProductDto) {
    const baseDir = `assets/product-images/${prod.id}/extras`;
    const urls: string[] = [];
    for (let i = 1; i <= 4; i++) {
      urls.push(`${baseDir}/${i}.jpg`);
    }
    this.extraImages.set(urls);
  }
}
