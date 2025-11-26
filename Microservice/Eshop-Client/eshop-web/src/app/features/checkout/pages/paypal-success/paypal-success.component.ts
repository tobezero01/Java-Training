import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-paypal-success',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './paypal-success.component.html',
  styleUrl: './paypal-success.component.css'
})
export class PaypalSuccessComponent {

}
