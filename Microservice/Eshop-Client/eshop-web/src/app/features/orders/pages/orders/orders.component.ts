import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { OrderSummary } from '../../models/order-summary.model';
import { OrdersService } from '../../../../core/services/order/orders.service';

@Component({
  selector: 'app-orders',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './orders.component.html',
  styleUrl: './orders.component.css'
})
export class OrdersComponent implements OnInit {

  private svc = inject(OrdersService);
  private router = inject(Router);

  loading = false;
  page = 1;
  totalPages = 1;
  items: OrderSummary[] = [];

  ngOnInit(): void {
    this.load(1);
  }

  load(p: number) {
    this.loading = true;
    this.svc.list(p).subscribe({
      next: res => {
        this.items = res.content || [];
        this.page = res.page ;
        this.totalPages = res.totalPages ;
      },
      error: () => {
        this.loading = false;
      },
      complete: () => {
        this.loading = false;
      }
    });
  }

  goDetail(o: OrderSummary) {
    this.router.navigate(['/orders', o.orderNumber]);
  }

  // Thêm hàm này vào class component
  getStatusClass(status: string): string {
    const s = status?.toLowerCase() || '';
    if (s.includes('new') || s.includes('pending')) return 'bg-info-subtle';
    if (s.includes('ship')) return 'bg-warning-subtle';
    if (s.includes('complete') || s.includes('delivered')) return 'bg-success-subtle';
    if (s.includes('cancel')) return 'bg-danger-subtle';
    return 'bg-secondary-subtle';
  }
}
