import { Component, inject, OnInit, signal } from '@angular/core';
import { ProductCardComponent } from "../../components/product-card/product-card.component";
import { CategoryTreeComponent } from "../../components/category-tree/category-tree.component";
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CatalogService } from '../../../../core/services/catalog/catalog.service';
import { clampPage } from '../../../../core/helpers/pagination.helpers';
import { CategoryDto } from '../../models/category-dto.model';
import { CategoryNodeDto } from '../../models/category-node-dto.model';

@Component({
  selector: 'app-product-list',
  standalone: true,
  imports: [ProductCardComponent, CategoryTreeComponent,CommonModule, RouterLink],
  templateUrl: './product-list.component.html',
  styleUrl: './product-list.component.css'
})
export class ProductListComponent implements OnInit{

  private catalogService = inject(CatalogService);
  private ar = inject(ActivatedRoute);
  private router = inject(Router);

  // state
  catTree = signal<any[]>([]);
  topCats = signal<CategoryDto[]>([]);
  selectedCat = signal<CategoryDto | null>(null);
  childCats = signal<CategoryDto[]>([]);

  items = signal<any[]>([]);
  page = signal(1);
  totalPages = signal(1);
  sort = signal('name|asc');
  keyword = signal('');

  async ngOnInit(): Promise<void> {

    const tree = await this.catalogService.getCategoryTree();
    this.catTree.set(tree);

    const top = await this.catalogService.getTopCategories();
    this.topCats.set(top);

    this.catalogService.getCategoryTree().then(tr => this.catTree.set(tr));

    this.ar.queryParamMap.subscribe(async query => {
      const catId = Number(query.get('catId') || 0) || undefined;
      const page = Number(query.get('page') || 1);
      const s = query.get('sort') || 'name';
      const d = (query.get('dir') as any) || 'asc';
      const kw = query.get('q') || '';

      this.sort.set(`${s}|${d}`);
      this.keyword.set(kw);
      this.page.set(Math.max(page, 1));

      if (kw) {
        this.selectedCat.set(null);
        this.childCats.set([]);
        const res = await this.catalogService.searchProducts(kw, page);
        this.items.set(res.items);
        this.totalPages.set(res.totalPages);
        this.page.set(clampPage(res.page, res.totalPages));
        return;
      }

      if (catId) {
        this.setSelectedAndChildren(catId);
        const res = await this.catalogService.listProductsByCategory(catId, page, s, d);
        this.items.set(res.items);
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

    });
  }
  goSearch(q: string) {
    this.router.navigate([], { queryParams: { q, page: 1 }, queryParamsHandling: 'merge' });
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

    const childs = (node.children || []).map< CategoryDto >(c => ({
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
