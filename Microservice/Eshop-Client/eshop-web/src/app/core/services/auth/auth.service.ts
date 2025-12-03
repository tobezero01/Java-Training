import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { environment } from '../../../../environments/environment.development';
import { LoginRequest } from '../../../features/auth/models/login-request.model';
import { JwtResponse } from '../../../features/auth/models/jwt-response.model';
import { API } from '../../constants/api-endpoints';
import { MeResponse } from '../../../features/auth/models/me-response.model';
import { ProfileResponse } from '../../../features/auth/models/profile-response.model';
import { ProfileUpdateRequest } from '../../../features/auth/models/profile-update-request.model';
import { ForgotPasswordRequest } from '../../../features/auth/models/forgot-password-request.model';
import { ResetPasswordRequest } from '../../../features/auth/models/reset-password-request.model';
import { SimpleMessage } from '../../../features/auth/models/simple-message.model';
import { RegisterRequest } from '../../../features/auth/models/register-request.model';
import { AuthStoreService } from '../auth-store/auth-store.service';
import { tap } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private readonly base = environment.baseGateway;

  private authStore = inject(AuthStoreService);

  constructor(private readonly http: HttpClient) { }

  login(request: LoginRequest) {
    // Backend sẽ set Cookie, frontend không cần nhận token trả về để lưu nữa
    return this.http.post<JwtResponse>(`${this.base}${API.AUTH.LOGIN}`, request)
      .pipe(
        tap((res) => {
          // Login thành công -> Set trạng thái đã đăng nhập
          this.authStore.authenticated.set(true);

        })
      );
  }

  refresh() {
    return this.http.post<JwtResponse>(`${this.base}${API.AUTH.REFRESH}`, {}, { withCredentials: true });
  }

  register(req: RegisterRequest) {
    return this.http.post<SimpleMessage>(`${this.base}${API.AUTH.REGISTER}`, req);
  }

  logout() {
    return this.http.post<void>(`${this.base}${API.AUTH.LOGOUT}`, {})
      .pipe(
        tap(() => {
          // Logout thành công -> Xóa state, trình duyệt tự xóa cookie
          this.authStore.authenticated.set(false);
          this.authStore.me.set(null);
        })
      );
  }

  me() {
    return this.http.get<MeResponse>(`${this.base}${API.AUTH.ME}`);
  }

  getProfile() {
    // nếu dùng /me2 trả ProfileResponse, còn /me trả MeResponse – trong code này dùng PATCH /me cho update
    return this.http.get<ProfileResponse>(`${this.base}${API.AUTH.ME_PROFILE}`);
  }

  updateProfile(req: ProfileUpdateRequest) {
    return this.http.patch<ProfileResponse>(`${this.base}${API.AUTH.ME}`, req);
  }

  forgotPassword(req: ForgotPasswordRequest) {
    return this.http.post<SimpleMessage>(`${this.base}${API.AUTH.FORGOT}`, req);
  }

  resetPassword(req: ResetPasswordRequest) {
    return this.http.post<SimpleMessage>(`${this.base}${API.AUTH.RESET}`, req);
  }
}
