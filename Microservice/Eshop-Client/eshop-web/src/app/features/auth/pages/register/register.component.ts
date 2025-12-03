import { CommonModule } from "@angular/common";
import { Component, inject } from "@angular/core";
import { FormBuilder, ReactiveFormsModule, Validators } from "@angular/forms";
import { AuthService } from "../../../../core/services/auth/auth.service";
import { ToastrService } from "ngx-toastr";
import { Router } from "@angular/router";
import { confirmPassword, passwordStrength } from "../../../../core/validation/password.validators";
import { markAllDirty } from "../../../../core/helpers/form.helpers";
import { RegisterRequest } from "../../models/register-request.model";

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './register.component.html',
  styleUrl: './register.component.css'
})
export class RegisterComponent {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private toastr = inject(ToastrService);
  private router = inject(Router);

  loading = false;

  form = this.fb.group({
    firstName: ['', Validators.required],
    lastName: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, passwordStrength()]],
    confirmPassword: ['', [Validators.required, confirmPassword('password')]],

    phone: ['', Validators.required],
    countryCode: ['', Validators.required],
    addressLine1: ['', Validators.required],
    addressLine2: [''],
    city: ['', Validators.required],
    state: ['', Validators.required],
    postalCode: ['', Validators.required]
  });

  get f() { return this.form.controls; }

  submit() {
    if (this.form.invalid) {
      markAllDirty(this.form);
      return;
    }

    this.loading = true;

    const {
      firstName, lastName, email, password,
      phone, countryCode, addressLine1, addressLine2,
      city, state, postalCode
    } = this.form.getRawValue();

    const payload: RegisterRequest = {
      firstName: String(firstName),
      lastName: String(lastName),
      email: String(email),
      password: String(password),
      phone: String(phone),
      countryCode: String(countryCode),
      addressLine1: String(addressLine1),
      addressLine2: addressLine2 ?? '',
      city: String(city),
      state: String(state),
      postalCode: String(postalCode)
    };

    this.auth.register(payload).subscribe({
      next: (res) => {
        this.toastr.success(
          res.message || 'Đăng ký thành công. Vui lòng kiểm tra email để xác minh tài khoản.'
        );
        // backend trả message: "Please check your email to verify the account":contentReference[oaicite:1]{index=1}
        this.router.navigateByUrl('/login');
      },
      error: (err) => {
        this.loading = false;
        const msg = err?.error?.message || 'Đăng ký thất bại';
        this.toastr.error(msg);
      },
      complete: () => this.loading = false
    });
  }
}
