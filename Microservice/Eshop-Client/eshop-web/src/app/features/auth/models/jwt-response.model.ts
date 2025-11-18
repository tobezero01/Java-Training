export interface JwtResponse {
  tokenType: string;          // "Bearer "
  accessToken: string;
  expiresInSeconds: number;
  fullName?: string;
}
