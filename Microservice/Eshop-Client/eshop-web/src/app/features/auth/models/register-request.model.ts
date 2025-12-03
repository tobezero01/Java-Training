export interface RegisterRequest {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  countryCode: string;
  addressLine1: string;
  addressLine2?: string | null;
  city: string;
  state: string;
  postalCode: string;
  phone: string;
}
