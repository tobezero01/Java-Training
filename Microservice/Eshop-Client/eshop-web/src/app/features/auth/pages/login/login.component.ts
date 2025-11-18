import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../../../core/services/auth/auth.service';
import { TokenStorageService } from '../../../../core/services/token-storage/token-storage.service';
import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { LoginRequest } from '../../models/login-request.model';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private tokenStore = inject(TokenStorageService);
  private router = inject(Router);
  private toastr = inject(ToastrService);

  loading = false;

  form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
    rememberMe: [true]
  });

  get f() { return this.form.controls; }

  onSubmit() {
    if (this.form.invalid) return;
    this.loading = true;
    const { email, password, rememberMe } = this.form.getRawValue();
    const payload: LoginRequest = { email, password, rememberMe };

    this.auth.login(payload).subscribe({
      next: (res) => {
        this.tokenStore.setToken(res.accessToken, !!this.form.value.rememberMe);
        this.toastr.success('Đăng nhập thành công');
        this.router.navigateByUrl('/catalog');
      },
      error: () => this.loading = false,
      complete: () => this.loading = false
    })
  }
}
