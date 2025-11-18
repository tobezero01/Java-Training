import { Component, inject, OnInit, signal } from '@angular/core';
import { ProductCardComponent } from "../../components/product-card/product-card.component";
import { CatalogService } from '../../../../core/services/catalog/catalog.service';
import { ActivatedRoute, Router } from '@angular/router';
import { clampPage } from '../../../../core/helpers/pagination.helpers';

@Component({
  selector: 'app-search',
  standalone: true,
  imports: [ProductCardComponent],
  templateUrl: './search.component.html',
  styleUrl: './search.component.css'
})
export class SearchComponent implements OnInit{

  private srv = inject(CatalogService);
  private ar = inject(ActivatedRoute);
  private router = inject(Router);

  keyword = signal('');
  items = signal<any[]>([]);
  page = signal(1);
  totalPages = signal(1);

  ngOnInit(): void {
    this.ar.queryParamMap.subscribe(async qp => {
      const q = qp.get('q') || '';
      const page = Number(qp.get('page') || 1);
      this.keyword.set(q);
      const res = await this.srv.searchProducts(q, page);
      this.items.set(res.items);
      this.totalPages.set(res.totalPages);
      this.page.set(clampPage(res.page, res.totalPages));
    });
  }

  goSearch(q: string) {
    this.router.navigate([], { queryParams: { q, page: 1 }, queryParamsHandling: 'merge' });
  }
  goPage(p: number) {
    this.router.navigate([], { queryParams: { page: p }, queryParamsHandling: 'merge' });
  }


}
