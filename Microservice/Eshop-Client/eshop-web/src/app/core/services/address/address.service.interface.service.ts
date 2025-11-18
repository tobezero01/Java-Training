import { Observable } from "rxjs";
import { Address } from "../../../features/address/models/address.model";
import { AddressCreateRequest } from "../../../features/address/models/address-create-request.model";
import { AddressUpdateRequest } from "../../../features/address/models/address-update-request.model";

export interface IAddressService {
  list(): Observable<Address[]>;
  getDefault(): Observable<Address>;
  create(req: AddressCreateRequest): Observable<Address>;
  update(id: number, req: AddressUpdateRequest): Observable<Address>;
  remove(id: number): Observable<void>;
  setDefault(id: number): Observable<void>;
}
