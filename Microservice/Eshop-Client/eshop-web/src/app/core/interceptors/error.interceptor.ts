

import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router, NavigationExtras } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { BehaviorSubject, catchError, filter, switchMap, take, throwError } from 'rxjs';
import { AuthService } from '../services/auth/auth.service';

// Biến toàn cục (ngoài hàm) để quản lý trạng thái Refresh
let isRefreshing = false;
const refreshTokenSubject = new BehaviorSubject<boolean>(false); // Đổi thành boolean, không cần chứa token string
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const toast = inject(ToastrService);
  const auth = inject(AuthService);
  const router = inject(Router);

  return next(req).pipe(
    catchError((err: HttpErrorResponse) => {
      // 1. Xử lý 401 (Cookie hết hạn / Access Token hết hạn)
      if (err.status === 401 && !req.url.includes('/auth/login')) {

        // Case A: Mình là người đầu tiên refresh
        if (!isRefreshing) {
          isRefreshing = true;
          refreshTokenSubject.next(false); // Block người sau

          return auth.refresh().pipe(
            switchMap(() => {
              isRefreshing = false;
              refreshTokenSubject.next(true); // Mở cửa cho người sau

              // QUAN TRỌNG: Không cần gắn header mới.
              // Trình duyệt tự động gửi Cookie mới vừa được Backend set.
              return next(req);
            }),
            catchError((refreshErr) => {
              isRefreshing = false;
              // Refresh thất bại (Refresh token cookie cũng hết hạn)
              if (!router.url.includes('/login')) {
                 // toast.error('Phiên đăng nhập hết hạn.'); // Tùy chọn
                 router.navigate(['/login']);
              }
              return throwError(() => refreshErr);
            })
          );
        }

        // Case B: Đang có người refresh rồi -> Đợi
        else {
          return refreshTokenSubject.pipe(
            filter(status => status === true), // Đợi đến khi xong (true)
            take(1),
            switchMap(() => {
              // Retry request (Browser tự gửi cookie)
              return next(req);
            })
          );
        }
      }

      // 2. Xử lý các lỗi khác (403, 404, 500...)
      // Không refresh được hoặc lỗi khác thì xử lý tập trung ở đây
      const error = err.error?.message || err.statusText;

      switch (err.status) {
        case 400:
             // Bad Request (Validate lỗi)
             toast.error(error || 'Dữ liệu không hợp lệ');
             break;
        case 403:
             // Forbidden (Không có quyền)
             toast.error('Bạn không có quyền thực hiện thao tác này');
             router.navigate(['/access-denied']);
             break;
        case 404:
             // Not found
             router.navigate(['/not-found']);
             toast.error('Không tìm thấy dữ liệu yêu cầu');
             break;
        case 500:
             // Server Error
             const navigationExtras: NavigationExtras = { state: { error: err.error } };
             router.navigateByUrl('/server-error', navigationExtras);
             toast.error('Lỗi hệ thống. Vui lòng thử lại sau.');
             break;
        default:
             if (err.status !== 401) {
                 toast.error(`Lỗi: ${error}`);
             }
      }

      return throwError(() => err);
    })
  );
};

// Helper function để gắn token vào header
