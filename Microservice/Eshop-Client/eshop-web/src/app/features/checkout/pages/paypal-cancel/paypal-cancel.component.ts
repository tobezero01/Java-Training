import { CommonModule } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { PaymentService } from '../../../../core/services/payment/payment.service';

@Component({
  selector: 'app-paypal-cancel',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './paypal-cancel.component.html',
  styleUrl: './paypal-cancel.component.css'
})
export class PaypalCancelComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private payments = inject(PaymentService);
  orderNumber = '';

  ngOnInit(): void {
    const paypalOrderId = this.route.snapshot.queryParamMap.get('token') || '';
    this.orderNumber = sessionStorage.getItem('pp.orderNumber') || '';
    if (this.orderNumber) {
      this.payments.paypalCancel(this.orderNumber, paypalOrderId).subscribe({
        complete: () => sessionStorage.removeItem('pp.orderNumber')
      });
    }
  }
}
