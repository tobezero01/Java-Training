import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { ToastrService } from 'ngx-toastr';
import { AuthService } from '../services/auth/auth.service';
import { TokenStorageService } from '../services/token-storage/token-storage.service';
import { catchError, switchMap, throwError } from 'rxjs';
import { NavigationExtras, Router } from '@angular/router';

let refreshing = false;

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const toast = inject(ToastrService);
  const auth = inject(AuthService);
  const tokenStore = inject(TokenStorageService);
  const router = inject(Router);

  return next(req).pipe(
    catchError( (err: HttpErrorResponse) => {
      if (err.status === 401 && !refreshing) {
        refreshing = true;
        return auth.refresh().pipe(
          switchMap((jwt) => {
            refreshing = false;
            tokenStore.setToken(jwt.accessToken, true); // sau refresh, coi như "remember"
            const retried = req.clone({ setHeaders: { Authorization: `Bearer ${jwt.accessToken}` } });
            return next(retried);
          }),
          catchError((e) => {
            refreshing = false;
            tokenStore.clear();
            toast.error('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.');
            return throwError(() => err);
          })
        );
      }
      if (err.status === 401 && refreshing) {
        return throwError(() => err);
      }
      // show lỗi chung
      const msg = (err.error && (err.error.message || err.error.error)) || err.message || 'Đã có lỗi xảy ra';
      toast.error(String(msg));
      return throwError(() => err);
    }),
    catchError((error: HttpErrorResponse) => {
      switch (error.status) {
        case 401:
          router.navigate(['/access-denied']);
          break;
        case 404:
          router.navigate(['/not-found']);
          break;
        case 500: {
          const navigationExtras: NavigationExtras = {state: {error: error.error}};
          router.navigateByUrl('/server-error', navigationExtras);
          break;
        }
        default: {
            const errorMessage = error.error?.message ?? 'An unexpected error occurred.';
            toast.error(errorMessage, `Error ${error.status}`);
            break;
        }
      }
      return throwError(() => error);
    })
  )
};
