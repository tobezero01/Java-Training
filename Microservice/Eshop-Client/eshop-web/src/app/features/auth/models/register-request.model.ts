export interface RegisterRequest {
  firstName?: string;
  lastName?: string;
  email: string;
  password: string;
  countryCode?: string;
  addressLine1?: string;
  addressLine2?: string;
  city?: string;
  state?: string;
  postalCode?: string;
  phone?: string;
}
