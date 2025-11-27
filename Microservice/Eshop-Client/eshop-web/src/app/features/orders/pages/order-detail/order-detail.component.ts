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
}
