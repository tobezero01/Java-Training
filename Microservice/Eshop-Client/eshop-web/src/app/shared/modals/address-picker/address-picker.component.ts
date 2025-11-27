import { CommonModule } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { NgbActiveModal, NgbModal, NgbModalModule } from '@ng-bootstrap/ng-bootstrap';
import { Address } from '../../../features/address/models/address.model';
import { AddressService } from '../../../core/services/address/address.service';
import { RouterLink, RouterModule } from '@angular/router';
import { AddressFormComponent } from '../../../features/address/pages/address-form/address-form.component';

@Component({
  selector: 'app-address-picker',
  standalone: true,
  imports: [CommonModule, NgbModalModule, RouterModule],
  templateUrl: './address-picker.component.html',
  styleUrl: './address-picker.component.css'
})
export class AddressPickerComponent implements OnInit {
  active = inject(NgbActiveModal);       // Để đóng chính nó
  private modalService = inject(NgbModal); // Để mở modal form con
  private svc = inject(AddressService);

  items: Address[] = [];
  loading = true;

  ngOnInit() {
    this.loadData();
  }

  loadData() {
    this.loading = true;
    this.svc.list().subscribe({
      next: v => this.items = v || [],
      error: () => this.loading = false,
      complete: () => this.loading = false
    });
  }

  choose(a: Address) {
    this.active.close(a);
  }

  // Hàm mở form thêm mới hoặc sửa
  openForm(addressId?: number) {
    // Mở AddressFormComponent đè lên
    const modalRef = this.modalService.open(AddressFormComponent, { size: 'lg', backdrop: 'static' });

    // Truyền input cho form
    modalRef.componentInstance.isModal = true;
    if (addressId) {
      modalRef.componentInstance.addressId = addressId;
    }

    // Lắng nghe kết quả khi đóng form
    modalRef.result.then((result) => {
      if (result === true) {
        // Nếu lưu thành công -> Load lại danh sách địa chỉ ở Picker
        this.loadData();
      }
    }, () => {
      // Dismiss (Hủy) thì không làm gì
    });
  }
}
