import { CommonModule } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { Router, RouterLink, RouterOutlet } from '@angular/router';
import { AuthStateService } from '../../core/services/auth/auth-state.service';

@Component({
  selector: 'app-simple-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink],
  templateUrl: './simple-layout.component.html',
  styleUrl: './simple-layout.component.css'
})
export class SimpleLayoutComponent {
  private authState = inject(AuthStateService);
  private router = inject(Router);

  user = this.authState.user;
  isAuthenticated = this.authState.isAuthenticated;

  userDisplayName = computed(() => {
    const u = this.user();
    if (!u) {
      return '';
    }
    if (u.fullName && u.fullName.trim().length > 0) {
      return u.fullName.trim();
    }
    const fullName = `${u.firstName ?? ''} ${u.lastName ?? ''}`.trim();
    return fullName || u.email || '';
  })
  logout() {
    this.authState.logout();
    this.router.navigateByUrl('/catalog');
  }
}
