import { HttpInterceptorFn } from '@angular/common/http';
import { environment } from '../../../environments/environment.development';
import { API } from '../constants/api-endpoints';

const skipList = new Set([
  `${environment.baseGateway}${API.AUTH.LOGIN}`,
  `${environment.baseGateway}${API.AUTH.REFRESH}`,
  `${environment.baseGateway}${API.AUTH.LOGOUT}`
]);

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const isApi = req.url.startsWith(environment.baseGateway);

  // Nếu gọi API -> Luôn bật withCredentials để trình duyệt gửi Cookie đi kèm
  if (isApi) {
    req = req.clone({
      withCredentials: true
    });
  }

  return next(req);
};
