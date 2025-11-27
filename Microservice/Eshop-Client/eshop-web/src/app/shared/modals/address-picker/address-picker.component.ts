import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { NgbActiveModal, NgbModalModule } from '@ng-bootstrap/ng-bootstrap';
import { Address } from '../../../features/address/models/address.model';
import { AddressService } from '../../../core/services/address/address.service';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-address-picker',
  standalone: true,
  imports: [CommonModule, NgbModalModule],
  templateUrl: './address-picker.component.html',
  styleUrl: './address-picker.component.css'
})
export class AddressPickerComponent {
  active = inject(NgbActiveModal);
  private svc = inject(AddressService);
  items: Address[] = [];
  loading = true;

  ngOnInit() {
    this.svc.list().subscribe({
      next: v => this.items = v || [],
      error: () => this.loading = false,
      complete: () => this.loading = false
    });
  }

  choose(a: Address) { this.active.close(a); }
}
