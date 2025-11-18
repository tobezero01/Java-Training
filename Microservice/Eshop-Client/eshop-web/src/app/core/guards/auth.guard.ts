import { CanActivateFn, Router } from '@angular/router';
import { TokenStorageService } from '../services/token-storage/token-storage.service';
import { inject } from '@angular/core';

export const authGuard: CanActivateFn = () => {
  const tokenStore = inject(TokenStorageService);
  const router = inject(Router);

  const token = tokenStore.getToken();
  if (!token) {
    router.navigateByUrl('/login');
    return false;
  }
  return true;
};
