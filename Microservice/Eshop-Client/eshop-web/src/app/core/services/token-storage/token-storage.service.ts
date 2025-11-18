import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class TokenStorageService {

  private readonly ACCESS_TOKEN_KEY = 'eshop.accessToken';
  private readonly REMEMBER_KEY = 'eshop.remember';

  constructor() {

  }

  private get store() {
    const remember = localStorage.getItem(this.REMEMBER_KEY) === "1";
    return remember ? localStorage : sessionStorage;
  }

  setToken(token: string, remember: boolean) {
    localStorage.setItem(this.REMEMBER_KEY, remember ? '1' : '0'); // nho lua chon
    localStorage.removeItem(this.ACCESS_TOKEN_KEY);
    sessionStorage.removeItem(this.ACCESS_TOKEN_KEY);
    (remember ? localStorage : sessionStorage).setItem(this.ACCESS_TOKEN_KEY, token);
  }

  getToken(): string | any {
    const remember = localStorage.getItem(this.REMEMBER_KEY) === '1';
    return (remember ? localStorage : sessionStorage).getItem(this.ACCESS_TOKEN_KEY);
  }

  clear() {
    localStorage.removeItem(this.ACCESS_TOKEN_KEY);
    sessionStorage.removeItem(this.ACCESS_TOKEN_KEY);
    localStorage.removeItem(this.REMEMBER_KEY);
  }
}
