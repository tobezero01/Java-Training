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

  // Dùng signal là tốt, nhưng cần đảm bảo template bind vào summary()
  summary = signal<CheckoutSummary | null>(null);

  // Chọn phương thức: COD | PAYPAL
  form = this.fb.nonNullable.group({
    addressId: [0, Validators.required],
    method: ['COD', Validators.required],
    note: ['']
  });

  ngOnInit() {
    this.reloadSummary();
  }

  reloadSummary(addressId?: number) {
    this.loading = true;

    // Nếu addressId không truyền vào (lần đầu load), dùng null/undefined để server lấy default
    this.checkout.summary(addressId).subscribe({
      next: (s) => {
        console.log('New Summary Loaded:', s); // Debug xem dữ liệu mới về chưa

        // 1. Cập nhật Signal để template render lại
        this.summary.set(s);

        // 2. Đồng bộ Form Control
        this.form.patchValue({ addressId: s.addressId });

        // 3. Xử lý logic Payment Method (Quan trọng)
        const currentMethod = this.form.controls.method.value;

        if (!s.shippingSupported) {
          // Nếu địa chỉ mới KHÔNG hỗ trợ ship -> Disable COD, chuyển sang Paypal
          if (currentMethod === 'COD') {
            this.form.controls.method.setValue('PAYPAL');
            this.toast.info('Địa chỉ này chưa hỗ trợ COD, đã chuyển sang thanh toán PayPal.');
          }
          // Disable control COD trong form (nếu muốn chặt chẽ hơn về mặt logic form)
          // Tuy nhiên HTML đã handle [attr.disabled], nên ở đây setValue là đủ.
        } else {
          // Nếu địa chỉ mới CÓ hỗ trợ ship -> Đảm bảo người dùng có thể chọn lại COD
          // Không cần làm gì thêm vì HTML sẽ tự enable lại radio button
        }
      },
      error: (err) => {
        this.loading = false;
        this.toast.error('Không thể tải thông tin thanh toán');
        console.error(err);
      },
      complete: () => this.loading = false
    });
  }

  openAddressPicker() {
    const ref = this.modal.open(AddressPickerComponent, { centered: true, size: 'lg' });

    // Khi đóng modal picker
    ref.result.then((selectedAddress) => {
      if (selectedAddress?.id) {
        console.log('Selected Address ID:', selectedAddress.id);
        // Gọi reload ngay lập tức với ID mới
        this.reloadSummary(selectedAddress.id);
      }
    }).catch(() => {});
  }

  placeOrder() {
    // ... (Giữ nguyên code cũ)
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
          this.router.navigateByUrl('/orders'); // Hoặc trang success
        },
        error: () => this.loading = false,
        complete: () => this.loading = false
      });
    } else {
      // PAYPAL logic...
      const origin = window.location.origin;
      const returnUrl = `${origin}/payment/paypal/return`;
      const cancelUrl = `${origin}/payment/paypal/cancel`;
      this.loading = true;
      this.payments.paypalCreate(addrId, returnUrl, cancelUrl).subscribe({
        next: (res) => {
          sessionStorage.setItem('pp.orderNumber', res.orderNumber);
          window.location.href = res.approvalUrl;
        },
        error: () => this.loading = false
      });
    }
  }
}
