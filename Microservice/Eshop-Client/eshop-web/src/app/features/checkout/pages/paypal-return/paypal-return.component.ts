import { CommonModule } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { PaymentService } from '../../../../core/services/payment/payment.service';
import { ToastrService } from 'ngx-toastr';
import { HttpErrorResponse } from '@angular/common/http';

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
  private router = inject(Router);

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
          this.toast.error('Thanh toán không hoàn tất. Vui lòng thử lại.');
        }
      },
      error: (err: HttpErrorResponse) => {
        this.ok = false;
        if (err.status === 400 && err.error && err.error.error === 'PAYMENT_INTENT_EXPIRED') {
          this.toast.warning(err.error.message || 'Phiên thanh toán đã hết hạn. Vui lòng thanh toán lại.');
          sessionStorage.removeItem('pp.orderNumber');
          this.router.navigateByUrl('/checkout');
        } else {
          this.toast.error('Thanh toán thất bại. Vui lòng thử lại sau.');
        }
      },
      complete: () => this.loading = false
    });
  }
}
