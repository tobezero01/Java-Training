import { CommonModule } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { AddressService } from '../../../../core/services/address/address.service';
import { Router, RouterLink, RouterModule } from '@angular/router';
import { Address } from '../../models/address.model';

@Component({
  selector: 'app-address-list',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './address-list.component.html',
  styleUrl: './address-list.component.css'
})
export class AddressListComponent implements OnInit{
  private addressService = inject(AddressService);
  private router = inject(Router);

  items: Address[] = [];
  loading = true;

  ngOnInit(): void { this.reload(); }

  reload() {
    this.loading = true;
    this.addressService.list().subscribe({
      next: (v) => this.items = v || [],
      error: () => this.loading = false,
      complete: () => this.loading = false
    });
  }

  setDefault(a: Address) {
    this.addressService.setDefault(a.id).subscribe({ next: () => this.reload() });
  }

  edit(a: Address) {
    this.router.navigate(['/account/addresses', a.id, 'edit']);
  }

  remove(a: Address) {
    if (!confirm('Xóa địa chỉ này?')) return;
    this.addressService.remove(a.id).subscribe({ next: () => this.reload() });
  }

  create() {
    this.router.navigateByUrl('/account/addresses/new');
  }
}
