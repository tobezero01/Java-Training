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
        this.page = res.page || 1;
        this.totalPages = res.totalPages || 1;
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
}
