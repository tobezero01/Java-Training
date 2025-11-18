import { AuthService } from './../../../../core/services/auth/auth.service';
import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Toast, ToastrService } from 'ngx-toastr';
import { markAllDirty } from '../../../../core/helpers/form.helpers';
import { ForgotPasswordRequest } from '../../models/forgot-password-request.model';

@Component({
  selector: 'app-forget-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './forget-password.component.html',
  styleUrl: './forget-password.component.css'
})
export class ForgetPasswordComponent {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private toastr = inject(ToastrService);
  loading = false;


  form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]]
  });

  submit() {
    if (this.form.invalid) {
      markAllDirty(this.form);
      return;
    }

    this.loading = true;
    const { email } = this.form.getRawValue();
        const payload: ForgotPasswordRequest = { email };
    this.auth.forgotPassword(payload).subscribe({
      next: (res) => this.toastr.success(res.message || 'Nếu email tồn tại, chúng tôi đã gửi hướng dẫn.'),
      error: () => this.loading = false,
      complete: () => this.loading = false
    });
  }
}
