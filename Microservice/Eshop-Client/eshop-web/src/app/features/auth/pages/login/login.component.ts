import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../../../core/services/auth/auth.service';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { LoginRequest } from '../../models/login-request.model';
import { AuthStateService } from '../../../../core/services/auth/auth-state.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private router = inject(Router);
  private toastr = inject(ToastrService);
  private ar = inject(ActivatedRoute);
  private authState = inject(AuthStateService);

  loading = false;

  form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
    rememberMe: [true]
  });

  get f() { return this.form.controls; }

  // login.component.ts

onSubmit() {
    if (this.form.invalid) return;
    this.loading = true;

    const req: LoginRequest = {
      email: this.form.value.email!,
      password: this.form.value.password!,
      rememberMe: this.form.value.rememberMe!
    };

    const returnUrl = this.ar.snapshot.queryParamMap.get('returnUrl') || '/catalog';

    this.auth.login(req).subscribe({
      next: () => {
        this.authState.loginSuccess();

        this.toastr.success('Đăng nhập thành công');
        this.router.navigateByUrl(returnUrl);
      },
      error: () => this.loading = false,
      complete: () => this.loading = false
    });
}
}
