import { Routes } from '@angular/router';

export const CHECKOUT_ROUTES: Routes = [
  { path: '', loadComponent: () => import('./pages/checkout/checkout.component').then(m => m.CheckoutComponent) }
];
