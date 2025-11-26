import { CommonModule } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { PaymentService } from '../../../../core/services/payment/payment.service';
import { ToastrService } from 'ngx-toastr';

@Component({
  selector: 'app-paypal-return',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './paypal-return.component.html',
  styleUrl: './paypal-return.component.css'
})
export class PaypalReturnComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private payments = inject(PaymentService);
  private toast = inject(ToastrService);

  loading = true;
  ok = false;

  ngOnInit(): void {
    // PayPal trả về ?token=<paypalOrderId>&PayerID=... (tuỳ config)
    const paypalOrderId = this.route.snapshot.queryParamMap.get('token') || '';
    const orderNumber = sessionStorage.getItem('pp.orderNumber') || '';

    if (!paypalOrderId || !orderNumber) {
      this.loading = false;
      this.ok = false;
      return;
    }

    this.payments.paypalCapture(paypalOrderId, orderNumber).subscribe({
      next: (res) => {
        if (res.status === 'COMPLETED') {
          this.ok = true;
          this.toast.success('Thanh toán thành công. Email xác nhận đã được gửi.');
          // dọn session key
          sessionStorage.removeItem('pp.orderNumber');
        } else {
          this.ok = false;
        }
      },
      error: () => this.ok = false,
      complete: () => this.loading = false
    });
  }
}
