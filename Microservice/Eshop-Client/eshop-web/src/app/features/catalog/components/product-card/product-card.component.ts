import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ProductDto } from '../../models/product-dto.model';

@Component({
  selector: 'app-product-card',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './product-card.component.html',
  styleUrl: './product-card.component.css'
})
export class ProductCardComponent {
  @Input() p!: ProductDto;

  onImgError(event: Event) {
    const img = event.target as HTMLImageElement;
    img.onerror = null;                    // tránh loop nếu image.png cũng lỗi
    img.src = 'assets/image.png';
  }
}
