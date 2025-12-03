import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthStateService } from '../services/auth/auth-state.service';
import { toObservable } from '@angular/core/rxjs-interop';
import { filter, map, take } from 'rxjs';

export const authGuard: CanActivateFn = () => {
  const authState = inject(AuthStateService);
  const router = inject(Router);

  // Chuyển signal thành observable để đợi isInitialized = true
  return toObservable(authState.isInitialized).pipe(
    filter(isInit => isInit), // Chỉ chạy tiếp khi đã init xong
    take(1),
    map(() => {
      // Check trạng thái login
      if (!authState.isAuthenticated()) {
        router.navigate(['/login']);
        return false;
      }
      return true;
    })
  );
};
