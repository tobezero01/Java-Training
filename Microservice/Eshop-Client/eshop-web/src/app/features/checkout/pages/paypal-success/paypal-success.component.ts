import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

@Component({
  selector: 'app-paypal-success',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './paypal-success.component.html',
  styleUrl: './paypal-success.component.css'
})
export class PaypalSuccessComponent {
private route = inject(ActivatedRoute);
  orderNumber: string | null = null;

  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      this.orderNumber = params['orderNumber'];
    });
  }
}
