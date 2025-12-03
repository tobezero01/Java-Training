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
import { authGuard } from './core/guards/auth.guard';
import { AccessDeniedComponent } from './errors/access-denied/access-denied.component';
import { NotFoundComponent } from './errors/not-found/not-found.component';
import { SearchComponent } from './features/catalog/pages/search/search.component';
import { ProductDetailComponent } from './features/catalog/pages/product-detail/product-detail.component';
import { AddressListComponent } from './features/address/pages/address-list/address-list.component';
import { AddressFormComponent } from './features/address/pages/address-form/address-form.component';
import { PaypalReturnComponent } from './features/checkout/pages/paypal-return/paypal-return.component';
import { PaypalCancelComponent } from './features/checkout/pages/paypal-cancel/paypal-cancel.component';
import { PaypalSuccessComponent } from './features/checkout/pages/paypal-success/paypal-success.component';
import { ContactComponent } from './features/static/pages/contact/contact.component';
import { PolicyComponent } from './features/static/pages/policy/policy.component';
import { OrderDetailComponent } from './features/orders/pages/order-detail/order-detail.component';
import { RegisterComponent } from './features/auth/pages/register/register.component';

export const routes: Routes = [
  { path: '', redirectTo: 'catalog', pathMatch: 'full' },

  {
    path: '',
    component: SimpleLayoutComponent,
    children: [
      // AUTH
      { path: 'login', component: LoginComponent },
      { path: 'forget-password', component: ForgetPasswordComponent },
      { path: 'reset-password', component: ResetPasswordComponent },
      { path: 'contact', component: ContactComponent },
      { path: 'policy', component: PolicyComponent },
      { path: 'register', component: RegisterComponent},

      // FEATURES (không lazy; trực tiếp path + component)
      { path: 'catalog', component: ProductListComponent },
      { path: 'cart', component: CartComponent },
      { path: 'checkout', component: CheckoutComponent , canActivate: [authGuard]},
      { path: 'orders', component: OrdersComponent , canActivate: [authGuard]},
      { path: 'account', component: AccountComponent, canActivate: [authGuard] },

      // CATALOG
      { path: 'catalog', component: ProductListComponent },
      { path: 'catalog/search', component: SearchComponent },
      { path: 'catalog/p/:alias', component: ProductDetailComponent },

      // Address book (Customer)
      { path: 'account/addresses', component: AddressListComponent, canActivate: [authGuard] },
      { path: 'account/addresses/new', component: AddressFormComponent, canActivate: [authGuard] },
      { path: 'account/addresses/:id/edit', component: AddressFormComponent, canActivate: [authGuard] },

      { path: 'checkout', component: CheckoutComponent, canActivate: [authGuard] },
      { path: 'payment/paypal/return', component: PaypalReturnComponent, canActivate: [authGuard] },
      { path: 'payment/paypal/cancel', component: PaypalCancelComponent, canActivate: [authGuard] },
      { path: 'payment/paypal/success', component: PaypalSuccessComponent, canActivate: [authGuard] },

      { path: 'orders', component: OrdersComponent, canActivate: [authGuard] },
      { path: 'orders/:orderNumber', component: OrderDetailComponent, canActivate: [authGuard] },
    ]
  },

  { path: 'not-found', component: NotFoundComponent },
  { path: 'access-denied', component: AccessDeniedComponent },
  { path: 'server-error', component: ServerErrorComponent },
  { path: '**', redirectTo: 'not-found' }

];
