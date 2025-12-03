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
import { AddItemRequest } from '../../../cart/models/add-cart-item.model';

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

  quantity = signal<number>(1);

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
  async addToCart() {
    if (!this.authState.isAuthenticated()) {
      this.toastr.info('Vui lòng đăng nhập để thêm vào giỏ hàng.');
      this.router.navigate(['/login'], { queryParams: { returnUrl: this.router.url } });
      return;
    }
    const alias = this.ar.snapshot.paramMap.get('alias')!;
    const prod = await this.srv.getProductByAlias(alias);
    if (!prod.inStock) {
      this.toastr.warning("Sản phẩm không còn trong kho");
      return;
    }
    this.p.set( prod);
    if (!this.p) {
      return;
    }

    let q = this.quantity();
    if (q < 1) q = 1;
    if (q > 10) q = 10;

    const req: AddItemRequest = {
    productId: prod.id,
    quantity: q
  };

    this.cartService.addItem(req).subscribe({
    next: () => {
      this.toastr.success('Đã thêm vào giỏ hàng.');
    },
    error: (err) => {
      console.error('addToCart error', err);
      this.toastr.error('Không thể thêm vào giỏ hàng.');
    }
  });
  }

  buyNow() {
    if (!this.authState.isAuthenticated()) {
      this.toastr.info('Vui lòng đăng nhập trước khi mua.');
      this.router.navigate(['/login'], { queryParams: { returnUrl: this.router.url } });
      return;
    }
    // TODO: điều hướng sang checkout khi có logic
    this.toastr.info('Chức năng này tạm chưa implement');
    //this.router.navigateByUrl('/checkout');
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
    // 1) Lấy URL ảnh chính hiện tại (đã set ở ngOnInit)
    const mainUrl = this.currentImage();       // vd: "assets/product-images/73/main.png"

    // 2) Khởi tạo mảng thumbnail
    const thumbs: string[] = [];

    // 3) Cho ảnh chính vào thumbnail đầu tiên
    //    (nếu không muốn hiển thị placeholder thì bỏ điều kiện `mainUrl !== 'assets/image.png'`)
    if (mainUrl && mainUrl !== 'assets/image.png') {
      thumbs.push(mainUrl);
    }

    // 4) Nếu backend trả danh sách ảnh phụ thì map sang URL thực tế
    if (prod.extraImagePaths && prod.extraImagePaths.length > 0) {
      const extraUrls = prod.extraImagePaths
        .map(p => {
          // Backend thường trả "/product-images/73/extras/xxx.png"
          if (p.startsWith('assets/')) {
            return p;                           // đã đầy đủ
          }
          return 'assets' + (p.startsWith('/')  // thêm prefix "assets"
            ? p
            : '/' + p);
        })
        .filter(u => u !== mainUrl);            // 5) tránh trùng với ảnh chính

      thumbs.push(...extraUrls);
    }

    this.extraImages.set(thumbs);
  }

  decQuantity() {
    const cur = this.quantity();
    if (cur > 1) {
      this.quantity.set(cur - 1);
    }
  }

  incQty() {
    // tăng 1 nhưng không vượt quá 10
    const cur = this.quantity();
    if (cur >= 10) {
      this.toastr.warning('Mỗi sản phẩm mua tối đa 10.');
      return;
    }
    this.quantity.set(cur + 1);
  }

  onManualQtyChange(val: string) {
    // parse từ input, clamp 1..10
    let n = Number(val || '1');
    if (Number.isNaN(n) || n < 1) n = 1;
    if (n > 10) {
      this.toastr.warning('Mỗi sản phẩm mua tối đa 10.');
      n = 10;
    }
    this.quantity.set(n);
  }

}
