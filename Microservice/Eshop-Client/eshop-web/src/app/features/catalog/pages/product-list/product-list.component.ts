import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { ProductCardComponent } from '../../components/product-card/product-card.component';
import { CategoryTreeComponent } from '../../components/category-tree/category-tree.component';
import { CatalogService } from '../../../../core/services/catalog/catalog.service';
import { clampPage } from '../../../../core/helpers/pagination.helpers';
import { CategoryDto } from '../../models/category-dto.model';
import { CategoryNodeDto } from '../../models/category-node-dto.model';
import { ToastrService } from 'ngx-toastr';

@Component({
  selector: 'app-product-list',
  standalone: true,
  imports: [ProductCardComponent, CategoryTreeComponent, CommonModule],
  templateUrl: './product-list.component.html',
  styleUrl: './product-list.component.css'
})
export class ProductListComponent implements OnInit {

  private catalogService = inject(CatalogService);
  private ar = inject(ActivatedRoute);
  private router = inject(Router);
  private toastr = inject(ToastrService);

  // state
  catTree = signal<CategoryNodeDto[]>([]);
  topCats = signal<CategoryDto[]>([]);
  selectedCat = signal<CategoryDto | null>(null);
  childCats = signal<CategoryDto[]>([]);


  items = signal<any[]>([]);
  page = signal(1);
  totalPages = signal(1);
  sort = signal('name|asc');
  keyword = signal('');
  size = signal(10);

  loadingTree = signal(false);
  loadingProducts = signal(false);

  featuredItems = signal<any[]>([]);
  featuredType = signal<'top-rated' | 'most-reviewed'>('top-rated'); // Tab đang chọn
  loadingFeatured = signal(false);
  featuredPage = signal(1);
  featuredTotalPages = signal(1);

  async ngOnInit(): Promise<void> {
    // load tree + topCats 1 lần, có catch lỗi rõ ràng
    this.loadingTree.set(true);
    try {
      const [tree, top] = await Promise.all([
        this.catalogService.getCategoryTree(),
        this.catalogService.getTopCategories()
      ]);
      this.catTree.set(tree);
      this.topCats.set(top);
    } catch (err) {
      console.error('Lỗi tải danh mục:', err);
      this.toastr.error('Không tải được danh mục. Vui lòng thử lại sau.');
    } finally {
      this.loadingTree.set(false);
    }

    // lắng nghe query params để load sản phẩm
    this.ar.queryParamMap.subscribe(async query => {
      const catId = Number(query.get('catId') || 0) || undefined;
      const page = Number(query.get('page') || 1);
      const s = query.get('sort') || 'name';
      const d = (query.get('dir') as any) || 'asc';
      const kw = query.get('q') || '';
      const sz = Number(query.get('size') || 10);

      this.sort.set(`${s}|${d}`);
      this.keyword.set(kw);
      this.page.set(Math.max(page, 1));
      this.size.set(sz);

      if (kw || catId) {
          this.featuredItems.set([]);
       }

       this.loadingProducts.set(true);

      try {
        if (kw) {
          this.selectedCat.set(null);
          this.childCats.set([]);
          const res = await this.catalogService.searchProducts(kw, page, sz);
          this.items.set(res.content);
          this.totalPages.set(res.totalPages);
          this.page.set(clampPage(res.page, res.totalPages));
          return;
        }

        if (catId) {
          this.setSelectedAndChildren(catId);
          const res = await this.catalogService.listProductsByCategory(catId, page, sz, s, d);
          this.items.set(res.content);
          this.totalPages.set(res.totalPages);
          this.page.set(clampPage(res.page, res.totalPages));
          return;
        }

        // nếu chưa chọn cat và không search → hiển thị top-level category gợi ý
        this.selectedCat.set(null);
        this.childCats.set([]);
        this.items.set([]);
        this.totalPages.set(1);
        this.page.set(1);
        this.loadFeaturedData();
      } catch (err) {
        console.error('Lỗi tải sản phẩm:', err);
        this.toastr.error('Không tải được sản phẩm. Vui lòng thử lại sau.');
      } finally {
        this.loadingProducts.set(false);
      }
    });
  }
  async loadFeaturedData(page: number = 1) {
    this.loadingFeatured.set(true);
    try {
      // Gọi API với page truyền vào
      const res = await this.catalogService.getFeaturedProducts(this.featuredType(), page, 10);

      this.featuredItems.set(res.content);

      // Cập nhật state phân trang
      this.featuredPage.set(res.page); // Hoặc dùng biến page
      this.featuredTotalPages.set(res.totalPages);

    } catch (e) {
      console.error('Lỗi load featured', e);
      this.toastr.error('Lỗi tải dữ liệu nổi bật');
    } finally {
      this.loadingFeatured.set(false);
    }
  }

  changeFeaturedTab(type: 'top-rated' | 'most-reviewed') {
    if (this.featuredType() === type) return;
    this.featuredType.set(type);

    // Khi đổi tab, reset về trang 1
    this.loadFeaturedData(1);
  }
  goFeaturedPage(p: number) {
    // Gọi hàm load lại dữ liệu ở trang mới
    this.loadFeaturedData(p);

    document.getElementById('featured-section')?.scrollIntoView({ behavior: 'smooth' });
  }

  goSearch(q: string) {
    this.router.navigate([], { queryParams: { q, page: 1, catId: null }, queryParamsHandling: 'merge' });
  }

  changeSort(val: string) {
    const [s, d] = val.split('|');
    this.router.navigate([], { queryParams: { sort: s, dir: d, page: 1 }, queryParamsHandling: 'merge' });
  }

  goPage(p: number) {
    this.router.navigate([], { queryParams: { page: p }, queryParamsHandling: 'merge' });
  }

  goCat(catId: number) {
    this.router.navigate([], { queryParams: { catId, page: 1, q: null }, queryParamsHandling: 'merge' });
  }

  private setSelectedAndChildren(catId: number) {
    const node = this.findNodeById(this.catTree(), catId);
    if (!node) {
      this.selectedCat.set(null);
      this.childCats.set([]);
      return;
    }
    this.selectedCat.set({
      id: node.id,
      name: node.name,
      alias: node.alias,
      imagePath: node.image,
      hasChildren: node.children?.length > 0
    });

    const childs = (node.children || []).map<CategoryDto>(c => ({
      id: c.id,
      name: c.name,
      alias: c.alias,
      imagePath: c.image,
      hasChildren: !!c.children?.length
    }));
    this.childCats.set(childs);
  }

  private findNodeById(tree: CategoryNodeDto[], id: number): CategoryNodeDto | null {
    for (const n of tree) {
      if (n.id === id) return n;
      if (n.children?.length) {
        const found = this.findNodeById(n.children, id);
        if (found) return found;
      }
    }
    return null;
  }
}
