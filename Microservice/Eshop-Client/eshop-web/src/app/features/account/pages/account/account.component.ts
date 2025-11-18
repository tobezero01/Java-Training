import { CommonModule } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../../../core/services/auth/auth.service';
import { ToastrService } from 'ngx-toastr';
import { ProfileResponse } from '../../../auth/models/profile-response.model';
import { ProfileUpdateRequest } from '../../../auth/models/profile-update-request.model';

@Component({
  selector: 'app-account',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './account.component.html',
  styleUrl: './account.component.css'
})
export class AccountComponent implements OnInit {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private toastr = inject(ToastrService);

  loading = false;

  form = this.fb.nonNullable.group({
    firstName: ['',[Validators.maxLength(45)]],
    lastName: ['',[Validators.maxLength(45)]],
    phone: ['',[Validators.maxLength(15)]],
    addressLine1: ['',[Validators.maxLength(64)]],
    addressLine2: ['',[Validators.maxLength(64)]],
    city: ['',[Validators.maxLength(45)]],
    state: ['',[Validators.maxLength(45)]],
    postalCode: ['',[Validators.maxLength(10)]],
    countryCode: [''],
    countryId: [null]
  })

  ngOnInit(): void {
      this.reload();
  }

  private setForm(p: ProfileResponse) {
    this.form.patchValue({
      firstName: p.firstName ?? '',
      lastName: p.lastName ?? '',
      phone: p.phone ?? '',
      addressLine1: p.addressLine1 ?? '',
      addressLine2: p.addressLine2 ?? '',
      city: p.city ?? '',
      state: p.state ?? '',
      postalCode: p.postalCode ?? '',
      countryCode: p.countryCode ?? '',
      countryId: p.countryCode ? null : null // có thể map thêm khi cần
    });
  }

  reload() {
    this.loading = true;
    this.auth.getProfile().subscribe({
      next: (p) => this.setForm(p),
      error: () => this.loading = false,
      complete: () => this.loading = false
    });
  }

save() {
  if (this.form.invalid) return;

  const raw = this.form.getRawValue();
  const req: ProfileUpdateRequest = {
    firstName: raw.firstName || undefined,
    lastName: raw.lastName || undefined,
    phone: raw.phone || undefined,
    addressLine1: raw.addressLine1 || undefined,
    addressLine2: raw.addressLine2 || undefined,
    city: raw.city || undefined,
    state: raw.state || undefined,
    postalCode: raw.postalCode || undefined,
    countryCode: raw.countryCode || undefined,
    countryId: raw.countryId ?? undefined
  };

  this.loading = true;
  this.auth.updateProfile(req).subscribe({
    next: (p) => {
      this.toastr.success('Đã lưu hồ sơ');
      this.setForm(p);
    },
    error: () => this.loading = false,
    complete: () => this.loading = false
  });
}


}
