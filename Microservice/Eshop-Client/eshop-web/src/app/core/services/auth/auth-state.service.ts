import { computed, inject, Injectable, signal } from "@angular/core";
import { AuthService } from "./auth.service";
import { ProfileResponse } from "../../../features/auth/models/profile-response.model";
import { catchError, of, tap } from "rxjs";

@Injectable({
  providedIn: 'root'
})
export class AuthStateService {
  private auth = inject(AuthService);

  // State quản lý User
  readonly user = signal<ProfileResponse | null>(null);

  // Computed signal: Tự động true nếu user != null
  readonly isAuthenticated = computed(() => !!this.user());

  // Biến check xem đã load xong thông tin user chưa (dùng cho APP_INITIALIZER)
  readonly isInitialized = signal(false);

  constructor() {
    // Khi App khởi động (F5), thử gọi API lấy profile xem Cookie còn sống không
    this.tryAutoLogin();
  }

  // Gọi khi F5 trang
  tryAutoLogin() {
    this.auth.getProfile().pipe(
      tap((profile) => {
        this.user.set(profile);
        this.isInitialized.set(true);
      }),
      catchError(() => {
        // Nếu lỗi (401) -> Cookie chết hoặc không có -> Coi như chưa login
        this.user.set(null);
        this.isInitialized.set(true);
        return of(null);
      })
    ).subscribe();
  }

  // Gọi khi Login thành công
  loginSuccess() {
    // Không cần truyền token vào đây nữa
    this.loadProfile();
  }

  logout() {
    this.user.set(null);
    // Backend tự xóa cookie, Frontend chỉ cần clear state
  }

  private loadProfile() {
    this.auth.getProfile().subscribe({
      next: profile => this.user.set(profile),
      error: () => this.user.set(null)
    });
  }
}
