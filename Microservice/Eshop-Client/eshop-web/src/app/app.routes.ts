// src/app/app.routes.ts
import { Routes } from '@angular/router';

import { SimpleLayoutComponent } from './layouts/simple-layout/simple-layout.component';

// AUTH pages
import { LoginComponent } from './features/auth/pages/login/login.component';
import { ForgetPasswordComponent } from './features/auth/pages/forget-password/forget-password.component';
import { ResetPasswordComponent } from './features/auth/pages/reset-password/reset-password.component';

// FEATURE pages
import { ProductListComponent } from './features/catalog/pages/product-list/product-list.component';
import { CartComponent } from './features/cart/pages/cart/cart.component';
import { CheckoutComponent } from './features/checkout/pages/checkout/checkout.component';
import { OrdersComponent } from './features/orders/pages/orders/orders.component';
import { AccountComponent } from './features/account/pages/account/account.component';

// Errors
import { ServerErrorComponent } from './errors/server-error/server-error.component';

export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },

  {
    path: '',
    component: SimpleLayoutComponent,
    children: [
      // AUTH
      { path: 'login', component: LoginComponent },
      { path: 'forget-password', component: ForgetPasswordComponent },
      { path: 'reset-password', component: ResetPasswordComponent },

      // FEATURES (không lazy; trực tiếp path + component)
      { path: 'catalog', component: ProductListComponent },
      { path: 'cart', component: CartComponent },
      { path: 'checkout', component: CheckoutComponent },
      { path: 'orders', component: OrdersComponent },
      { path: 'account', component: AccountComponent },
    ]
  },

  // Lỗi 500 (tùy chọn)
  { path: '500', component: ServerErrorComponent },

  // Cuối cùng: wildcard -> /login
  { path: '**', redirectTo: 'login' }
];
