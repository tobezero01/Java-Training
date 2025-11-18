import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CategoryNodeDto } from '../../models/category-node-dto.model';

@Component({
  selector: 'app-category-tree',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './category-tree.component.html',
  styleUrl: './category-tree.component.css'
})
export class CategoryTreeComponent {
  @Input() tree: CategoryNodeDto[] = [];
}
