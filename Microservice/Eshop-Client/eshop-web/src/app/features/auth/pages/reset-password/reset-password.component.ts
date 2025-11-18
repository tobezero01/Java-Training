import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../../../core/services/auth/auth.service';
import { ToastrService } from 'ngx-toastr';
import { confirmPassword, passwordStrength } from '../../../../core/validation/password.validators';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './reset-password.component.html',
  styleUrl: './reset-password.component.css'
})
export class ResetPasswordComponent {
private fb = inject(FormBuilder);
  private route = inject(ActivatedRoute);
  private auth = inject(AuthService);
  private toast = inject(ToastrService);
  private router = inject(Router);

  loading = false;
  token = '';

  form = this.fb.group({
    newPassword: ['', [Validators.required, passwordStrength()]],
    confirm: ['', [Validators.required, confirmPassword('newPassword')]]
  });

  get f() { return this.form.controls; }

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParamMap.get('token') || '';
  }

  submit() {
    if (!this.token) { this.toast.error('Thiếu token'); return; }
    if (this.form.invalid) return;

    this.loading = true;
    this.auth.resetPassword({
      token: this.f.newPassword.root?.value
        ? this.token : this.token, newPassword: String(this.f.newPassword.value)
    })
      .subscribe({
        next: (res) => {
          this.toast.success(res.message || 'Đã đặt lại mật khẩu');
          this.router.navigateByUrl('/login');
        },
        error: () => this.loading = false,
        complete: () => this.loading = false
      });
  }
}
