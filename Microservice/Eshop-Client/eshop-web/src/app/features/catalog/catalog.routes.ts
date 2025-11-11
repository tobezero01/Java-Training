import { Routes } from '@angular/router';

export const CATALOG_ROUTES: Routes = [
  { path: '', loadComponent: () => import('./pages/product-list/product-list.component').then(m => m.ProductListComponent) }
];
