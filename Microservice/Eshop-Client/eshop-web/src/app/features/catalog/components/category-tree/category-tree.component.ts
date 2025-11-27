import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { CategoryNodeDto } from '../../models/category-node-dto.model';

@Component({
  selector: 'app-category-tree',
  standalone: true,
  imports: [CommonModule, RouterLink,RouterLinkActive],
  templateUrl: './category-tree.component.html',
  styleUrl: './category-tree.component.css'
})
export class CategoryTreeComponent {
  @Input() tree: CategoryNodeDto[] = [];

  expandedState: { [key: number]: boolean } = {};

  toggle(id: number, event: Event) {
    event.stopPropagation(); // Tránh kích hoạt router link khi bấm nút mở rộng
    event.preventDefault();
    this.expandedState[id] = !this.expandedState[id];
  }

  isExpanded(id: number): boolean {
    return !!this.expandedState[id];
  }
}
