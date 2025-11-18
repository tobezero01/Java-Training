import { CommonModule } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AddressService } from '../../../../core/services/address/address.service';
import { AddressUpdateRequest } from '../../models/address-update-request.model';
import { AddressCreateRequest } from '../../models/address-create-request.model';

@Component({
  selector: 'app-address-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './address-form.component.html',
  styleUrl: './address-form.component.css'
})
export class AddressFormComponent implements OnInit {
  private fb = inject(FormBuilder);
  private svc = inject(AddressService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  id?: number;
  loading = false;
  saving = false;

  // DÙNG nonNullable.group => type của controls là string/number/boolean, không null
  form = this.fb.nonNullable.group({
    firstName: ['', [Validators.required, Validators.maxLength(45)]],
    lastName: ['', [Validators.required, Validators.maxLength(45)]],
    phoneNumber: ['', [Validators.required, Validators.maxLength(15)]],
    addressLine1: ['', [Validators.required, Validators.maxLength(64)]],
    addressLine2: ['', [Validators.maxLength(64)]],
    city: ['', [Validators.required, Validators.maxLength(45)]],
    state: ['', [Validators.required, Validators.maxLength(45)]],
    postalCode: ['', [Validators.required, Validators.maxLength(10)]],
    countryId: 1, // mặc định 1, nonNullable nên không null
    defaultForShipping: false
  });

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.id = +idParam;
      this.loading = true;
      // Không có API get-by-id → lấy list rồi tìm
      this.svc.list().subscribe({
        next: (list) => {
          const found = list?.find(x => x.id === this.id);
          if (found) {
            this.form.patchValue({
              firstName: found.firstName ?? '',
              lastName: found.lastName ?? '',
              phoneNumber: found.phoneNumber ?? '',
              addressLine1: found.addressLine1 ?? '',
              addressLine2: found.addressLine2 ?? '',
              city: found.city ?? '',
              state: found.state ?? '',
              postalCode: found.postalCode ?? '',
              countryId: found.countryId ?? 1,
              defaultForShipping: !!found.defaultForShipping
            });
          }
        },
        error: () => this.loading = false,
        complete: () => this.loading = false
      });
    }
  }

  save() {
    if (this.form.invalid) return;
    this.saving = true;

    // LẤY RAW VALUE (KHÔNG null) + MAP SANG DTO
    const raw = this.form.getRawValue();
    const payload: AddressCreateRequest = {
      firstName: raw.firstName,
      lastName: raw.lastName,
      phoneNumber: raw.phoneNumber,
      addressLine1: raw.addressLine1,
      // Nếu rỗng thì để undefined cho đúng kiểu optional
      addressLine2: raw.addressLine2 || undefined,
      city: raw.city,
      state: raw.state,
      postalCode: raw.postalCode,
      countryId: raw.countryId,
      defaultForShipping: raw.defaultForShipping || undefined
    };

    const obs = this.id
      ? this.svc.update(this.id, payload as AddressUpdateRequest)
      : this.svc.create(payload);

    obs.subscribe({
      next: () => this.router.navigateByUrl('/account/addresses'),
      error: () => this.saving = false,
      complete: () => this.saving = false
    });
  }

  cancel() {
    this.router.navigateByUrl('/account/addresses');
  }
}
