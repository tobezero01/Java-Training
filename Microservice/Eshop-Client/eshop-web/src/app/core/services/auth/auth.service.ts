import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
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

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private readonly base = environment.baseGateway;

  constructor(private readonly http: HttpClient) { }

  login(request: LoginRequest) {
    return this.http.post<JwtResponse>(`${this.base}${API.AUTH.LOGIN}`,
      request, { withCredentials: true });
  }

  refresh() {
    return this.http.post<JwtResponse>(`${this.base}${API.AUTH.REFRESH}`, {}, { withCredentials: true });
  }

  logout() {
    return this.http.post<void>(`${this.base}${API.AUTH.LOGOUT}`, {}, { withCredentials: true });
  }

  me() {
    return this.http.get<MeResponse>(`${this.base}${API.AUTH.ME}`);
  }

  getProfile() {
    // nếu dùng /me2 trả ProfileResponse, còn /me trả MeResponse – trong code này dùng PATCH /me cho update
    return this.http.get<ProfileResponse>(`${this.base}${API.AUTH.ME_PROFILE}`);
  }

  updateProfile(req: ProfileUpdateRequest) {
    return this.http.patch<ProfileResponse>(`${this.base}${API.AUTH.ME_PROFILE}`, req);
  }

  forgotPassword(req: ForgotPasswordRequest) {
    return this.http.post<SimpleMessage>(`${this.base}${API.AUTH.FORGOT}`, req);
  }

  resetPassword(req: ResetPasswordRequest) {
    return this.http.post<SimpleMessage>(`${this.base}${API.AUTH.RESET}`, req);
  }
}
