import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { OrderDetail } from '../../models/order-detail.model';
import { OrdersService } from '../../../../core/services/order/orders.service';

@Component({
  selector: 'app-order-detail',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './order-detail.component.html',
  styleUrl: './order-detail.component.css'
})
export class OrderDetailComponent implements OnInit {

  private svc = inject(OrdersService);
  private route = inject(ActivatedRoute);

  loading = false;
  order?: OrderDetail;

  ngOnInit(): void {
    const orderNumber = this.route.snapshot.paramMap.get('orderNumber');
    if (orderNumber) {
      this.load(orderNumber);
    }
  }

  private load(orderNumber: string) {
    this.loading = true;
    this.svc.get(orderNumber).subscribe({
      next: o => this.order = o,
      error: () => this.loading = false,
      complete: () => this.loading = false
    });
  }

  // Trong class component
  getStatusClass(status: string): string {
    // Chuẩn hóa status về chữ thường để so sánh
    const s = status?.toLowerCase() || '';

    if (s.includes('new') || s.includes('pending')) return 'bg-info-subtle text-info border border-info-subtle';
    if (s.includes('ship')) return 'bg-warning-subtle text-warning border border-warning-subtle';
    if (s.includes('complete') || s.includes('delivered')) return 'bg-success-subtle text-success border border-success-subtle';
    if (s.includes('cancel')) return 'bg-danger-subtle text-danger border border-danger-subtle';

    return 'bg-secondary-subtle text-secondary'; // Mặc định
  }
}
