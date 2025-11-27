import { computed, inject, Injectable, signal } from "@angular/core";
import { AuthService } from "./auth.service";
import { TokenStorageService } from "../token-storage/token-storage.service";
import { ProfileResponse } from "../../../features/auth/models/profile-response.model";

@Injectable({
  providedIn: 'root'
})
export class AuthStateService {
  private auth = inject(AuthService);
  private tokenStore = inject(TokenStorageService);

  readonly user = signal<ProfileResponse | null>(null);
  readonly loaded = signal(false);
  readonly isAuthenticated = computed(() => !!this.user());

  constructor() {
    const token = this.tokenStore.getToken();
    if (token) {
      this.loadProfile();
    } else {
      this.loaded.set(true);
    }
  }

  loginSuccess(accessToken: string, remember: boolean) {
    this.tokenStore.setToken(accessToken, remember);
    this.loadProfile();
  }

  logout() {
    this.tokenStore.clear();
    this.user.set(null);
    this.loaded.set(true);
  }

  loadProfile() {
    this.loaded.set(false);
    this.auth.getProfile().subscribe({
      next: profile => {
        this.user.set(profile);
        this.loaded.set(true);
      },
      error: () => {
        this.tokenStore.clear();
        this.user.set(null);
        this.loaded.set(true);
      }
    });
  }

}
