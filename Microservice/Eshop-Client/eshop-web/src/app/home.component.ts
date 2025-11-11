import { Component } from '@angular/core';

@Component({
  standalone: true,
  selector: 'app-home',
  template: `
    <div class="p-3 rounded-2 border mb-3">
      <div class="p-4 rounded-3 shadow-sm bg-white">
        <span class="badge bg-secondary me-2">Tailwind ON</span>
        <span class="badge bg-success">Bootstrap ON</span>
      </div>
    </div>
  `
})
export class HomeComponent {}
