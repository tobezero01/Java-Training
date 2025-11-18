import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { delay, finalize } from 'rxjs';
import { BusyLoadingService } from '../services/busy-loading.service';

export const loadingInterceptor: HttpInterceptorFn = (req, next) => {
  const busyService = inject(BusyLoadingService);
  busyService.busy();

  return next(req).pipe(
    delay(300),
    finalize(() => {
      busyService.idle()
    })
  )
};
