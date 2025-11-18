import { Injectable, signal } from '@angular/core';
import { MeResponse } from '../../../features/auth/models/me-response.model';

@Injectable({
  providedIn: 'root'
})
export class AuthStoreService {
  me = signal<MeResponse | null>(null);
  authenticated = signal<boolean>(false);

  setMe(m: MeResponse | null) {
    this.me.set(m);
    this.authenticated.set(!!m);
  }
}
