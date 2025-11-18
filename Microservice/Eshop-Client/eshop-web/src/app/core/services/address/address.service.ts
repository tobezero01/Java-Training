import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Address } from '../../../features/address/models/address.model';
import { API } from '../../constants/api-endpoints';
import { AddressCreateRequest } from '../../../features/address/models/address-create-request.model';
import { AddressUpdateRequest } from '../../../features/address/models/address-update-request.model';

@Injectable({
  providedIn: 'root'
})
export class AddressService {
  private http = inject(HttpClient);

  list(): Observable<Address[]> {
    return this.http.get<Address[]>(API.ADDRESSES.ROOT);
  }

  getDefault(): Observable<Address> {
    return this.http.get<Address>(API.ADDRESSES.DEFAULT);
  }

  create(req: AddressCreateRequest): Observable<Address> {
    return this.http.post<Address>(API.ADDRESSES.ROOT, req);
  }

  update(id: number, req: AddressUpdateRequest): Observable<Address> {
    return this.http.put<Address>(`${API.ADDRESSES.ROOT}/${id}`, req);
  }

  remove(id: number): Observable<void> {
    return this.http.delete<void>(`${API.ADDRESSES.ROOT}/${id}`);
  }

  setDefault(id: number): Observable<void> {
    return this.http.put<void>(`${API.ADDRESSES.ROOT}/${id}/default`, {});
  }
}
