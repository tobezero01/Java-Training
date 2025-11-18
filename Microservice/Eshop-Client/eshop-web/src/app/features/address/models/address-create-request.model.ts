export interface AddressCreateRequest {
  firstName: string;
  lastName: string;
  phoneNumber: string;
  addressLine1: string;
  addressLine2?: string;
  city: string;
  state: string;
  postalCode: string;
  countryId: number;
  defaultForShipping?: boolean;
}
