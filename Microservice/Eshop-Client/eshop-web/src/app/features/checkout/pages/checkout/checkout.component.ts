import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbModal, NgbModalModule } from '@ng-bootstrap/ng-bootstrap';
import { CheckoutService } from '../../../../core/services/checkout/checkout.service';
import { PaymentService } from '../../../../core/services/payment/payment.service';
import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { CheckoutSummary } from '../../models/checkout-summary.model';
import { AddressPickerComponent } from '../../../../shared/modals/address-picker/address-picker.component';

@Component({
  selector: 'app-checkout',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, NgbModalModule],
  templateUrl: './checkout.component.html',
  styleUrl: './checkout.component.css'
})
export class CheckoutComponent implements OnInit {
  private checkout = inject(CheckoutService);
  private payments = inject(PaymentService);
  private modal = inject(NgbModal);
  private router = inject(Router);
  private toast = inject(ToastrService);
  private fb = inject(FormBuilder);

  loading = false;
  summary = signal<CheckoutSummary | null>(null);

  // Chọn phương thức: COD | PAYPAL
  form = this.fb.nonNullable.group({
    addressId: [0, Validators.required],
    method: ['COD', Validators.required],
    note: ['']
  });

  async ngOnInit() {
    // Multi-promise: lấy summary theo default address (server tự hiểu) – Promise 'đa luồng'
    this.reloadSummary();
  }

  reloadSummary(addressId?: number) {
    this.loading = true;
    this.checkout.summary(addressId).subscribe({
      next: (s) => {
        this.summary.set(s);
        // set addressId form
        this.form.patchValue({ addressId: s.addressId || 0 });
        // Nếu shippingSupported=false => disable COD
        if (!s.shippingSupported && this.form.controls.method.value === 'COD') {
          this.form.controls.method.setValue('PAYPAL');
        }
      },
      error: () => this.loading = false,
      complete: () => this.loading = false
    });
  }

  openAddressPicker() {
    const ref = this.modal.open(AddressPickerComponent, { centered: true, size: 'lg' });
    ref.result.then((a) => {
      if (a?.id) {
        this.form.controls.addressId.setValue(a.id);
        this.reloadSummary(a.id);
      }
    }).catch(() => {});
  }

  placeOrder() {
    if (this.form.invalid || !this.summary()) return;
    const method = this.form.controls.method.value;
    const addrId = this.form.controls.addressId.value;
    const note = this.form.controls.note.value;

    if (method === 'COD') {
      if (!this.summary()?.shippingSupported) {
        this.toast.warning('Địa chỉ này không hỗ trợ COD.');
        return;
      }
      this.loading = true;
      this.checkout.placeOrderCod({ addressId: addrId, paymentMethod: 'COD', note }).subscribe({
        next: (res) => {
          this.toast.success(`Đặt hàng thành công. Mã đơn: ${res.orderNumber}. Vui lòng kiểm tra email.`);
          this.router.navigateByUrl('/orders');
        },
        error: () => this.loading = false,
        complete: () => this.loading = false
      });
    } else {
      // PAYPAL
      const origin = window.location.origin;
      const returnUrl = `${origin}/payment/paypal/return`;
      const cancelUrl = `${origin}/payment/paypal/cancel`;
      this.loading = true;
      this.payments.paypalCreate(addrId, returnUrl, cancelUrl).subscribe({
        next: (res) => {
          // Lưu orderNumber để dùng ở trang return
          sessionStorage.setItem('pp.orderNumber', res.orderNumber);
          // Redirect đến approvalUrl
          window.location.href = res.approvalUrl;
        },
        error: () => this.loading = false
      });
    }
  }
}
