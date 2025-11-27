import { Component, Input, OnInit, Optional, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap'; // Import cái này
import { AddressService } from '../../../../core/services/address/address.service';
// ... import AddressService, DTOs ...

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

  // Inject ActiveModal để đóng modal (dùng @Optional vì nếu chạy dạng page sẽ ko có cái này)
  private activeModal = inject(NgbActiveModal, { optional: true });

  // --- CẤU HÌNH CHO MODAL MODE ---
  @Input() isModal = false;      // Biến cờ để biết đang chạy mode nào
  @Input() addressId?: number;   // Nhận ID trực tiếp từ cha
  // -------------------------------
provinces = [
    "An Giang", "Ba Ria Vung Tau", "Bac Giang", "Bac Kan", "Bac Lieu", "Bac Ninh",
    "Ben Tre", "Binh Dinh", "Binh Duong", "Binh Phuoc", "Binh Thuan", "Ca Mau",
    "Can Tho", "Cao Bang", "Da Nang", "Dak Lak", "Dak Nong", "Dien Bien",
    "Dong Nai", "Dong Thap", "Gia Lai", "Ha Giang", "Ha Nam", "Ha Noi",
    "Ha Tinh", "Hai Duong", "Hai Phong", "Hau Giang", "Ho Chi Minh", "Hoa Binh",
    "Hung Yen", "Khanh Hoa", "Kien Giang", "Kon Tum", "Lai Chau", "Lam Dong",
    "Lang Son", "Lao Cai", "Long An", "Nam Dinh", "Nghe An", "Ninh Binh",
    "Ninh Thuan", "Phu Tho", "Phu Yen", "Quang Binh", "Quang Nam", "Quang Ngai",
    "Quang Ninh", "Quang Tri", "Soc Trang", "Son La", "Tay Ninh", "Thai Binh",
    "Thai Nguyen", "Thanh Hoa", "Thua Thien Hue", "Tien Giang", "Tra Vinh",
    "Tuyen Quang", "Vinh Long", "Vinh Phuc", "Yen Bai"
  ];
  id?: number;
  loading = false;
  saving = false;

  form = this.fb.nonNullable.group({
    firstName: ['', [Validators.required, Validators.maxLength(45)]],
    lastName: ['', [Validators.required, Validators.maxLength(45)]],
    phoneNumber: ['', [Validators.required, Validators.maxLength(15)]],
    addressLine1: ['', [Validators.required, Validators.maxLength(64)]],
    addressLine2: ['', [Validators.maxLength(64)]],
    city: ['', [Validators.required, Validators.maxLength(45)]],
    state: ['', [Validators.required, Validators.maxLength(45)]],
    postalCode: ['', [Validators.required, Validators.maxLength(10)]],
    countryId: 1,
    defaultForShipping: false
  });

  ngOnInit(): void {
    // Ưu tiên lấy ID từ Input (Modal), nếu ko có thì lấy từ URL (Page)
    if (this.addressId) {
      this.id = this.addressId;
    } else {
      const idParam = this.route.snapshot.paramMap.get('id');
      if (idParam) this.id = +idParam;
    }

    if (this.id) {
      this.loadAddressData(this.id);
    }
  }

  // Tách hàm load riêng để gọn
  loadAddressData(id: number) {
    this.loading = true;
    this.svc.list().subscribe({
      next: (list) => {
        const found = list?.find(x => x.id === id);
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
            countryId: found.countryId ?? 242,
            defaultForShipping: !!found.defaultForShipping
          });
        }
      },
      error: () => this.loading = false,
      complete: () => this.loading = false
    });
  }

  save() {
    if (this.form.invalid) return;
    this.saving = true;

    const raw = this.form.getRawValue();
    // Map DTO... (Code cũ của bạn giữ nguyên)
    const payload: any = { ...raw, addressLine2: raw.addressLine2 || undefined, defaultForShipping: raw.defaultForShipping || undefined };

    const obs = this.id
      ? this.svc.update(this.id, payload)
      : this.svc.create(payload);

    obs.subscribe({
      next: () => {
        if (this.isModal && this.activeModal) {
          // Nếu là Modal: Đóng modal và trả về true để báo thành công
          this.activeModal.close(true);
        } else {
          // Nếu là Page: Navigate
          this.router.navigateByUrl('/account/addresses');
        }
      },
      error: () => this.saving = false,
      complete: () => this.saving = false
    });
  }

  cancel() {
    if (this.isModal && this.activeModal) {
      this.activeModal.dismiss(); // Đóng modal
    } else {
      this.router.navigateByUrl('/account/addresses');
    }
  }
}
