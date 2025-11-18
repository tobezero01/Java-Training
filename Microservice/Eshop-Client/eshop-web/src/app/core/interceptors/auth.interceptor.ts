import { HttpInterceptorFn } from '@angular/common/http';
import { environment } from '../../../environments/environment.development';
import { API } from '../constants/api-endpoints';
import { inject } from '@angular/core';
import { TokenStorageService } from '../services/token-storage/token-storage.service';

const skipList = new Set([
  `${environment.baseGateway}${API.AUTH.LOGIN}`,
  `${environment.baseGateway}${API.AUTH.REFRESH}`,
  `${environment.baseGateway}${API.AUTH.LOGOUT}`
]);

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const tokenStore = inject(TokenStorageService);
  const token = tokenStore.getToken();

  const isApi = req.url.startsWith(environment.baseGateway);
  const isSkip = skipList.has(req.url);

  const cloned = (!isApi || isSkip || !token) ? req : req.clone({
    setHeaders: { Authorization: `Bearer ${token}` }
  });

  return next(cloned);
};
