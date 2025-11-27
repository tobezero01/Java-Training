import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Address } from '../../../features/address/models/address.model';
import { API } from '../../constants/api-endpoints';
import { AddressCreateRequest } from '../../../features/address/models/address-create-request.model';
import { AddressUpdateRequest } from '../../../features/address/models/address-update-request.model';
import { environment } from '../../../../environments/environment.development';

@Injectable({
  providedIn: 'root'
})
export class AddressService {
  private http = inject(HttpClient);
  private base = environment.baseGateway;   // http://localhost:8080

  list(): Observable<Address[]> {
    // GET /api/addresses
    return this.http.get<Address[]>(`${this.base}${API.ADDRESS.ROOT}`);
  }

  getDefault(): Observable<Address> {
    // GET /api/addresses/default
    return this.http.get<Address>(`${this.base}${API.ADDRESS.ROOT}/default`);
  }

  create(req: AddressCreateRequest): Observable<Address> {
    // POST /api/addresses
    return this.http.post<Address>(`${this.base}${API.ADDRESS.ROOT}`, req);
  }

  update(id: number, req: AddressUpdateRequest): Observable<Address> {
    // PUT /api/addresses/{id}
    return this.http.put<Address>(`${this.base}${API.ADDRESS.BY_ID(id)}`, req);
  }

  remove(id: number): Observable<void> {
    // DELETE /api/addresses/{id}
    return this.http.delete<void>(`${this.base}${API.ADDRESS.BY_ID(id)}`);
  }

  setDefault(id: number): Observable<void> {
    // PUT /api/addresses/{id}/default
    return this.http.put<void>(`${this.base}${API.ADDRESS.BY_ID(id)}/default`, {});
  }
}
