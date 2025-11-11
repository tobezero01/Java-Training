import { Routes } from '@angular/router';

export const ACCOUNT_ROUTES: Routes = [
  { path: '', loadComponent: () => import('./pages/account/account.component').then(m => m.AccountComponent) }
];
