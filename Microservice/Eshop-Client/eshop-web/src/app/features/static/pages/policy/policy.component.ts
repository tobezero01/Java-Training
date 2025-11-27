import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

interface RegionData {
  region: string;
  provinces: {
    name: string;
    rate: number;
    days: number;
    cod: boolean;
  }[];
}

@Component({
  selector: 'app-policy',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './policy.component.html',
  styleUrl: './policy.component.css'
})
export class PolicyComponent {

  // Dữ liệu static lấy từ logic SQL shipping_rates
  shippingTable: RegionData[] = [
    {
      region: 'Miền Bắc',
      provinces: [
        { name: 'Hà Nội', rate: 3.50, days: 2, cod: true },
        { name: 'Hải Phòng', rate: 3.80, days: 2, cod: true },
        { name: 'Bắc Ninh', rate: 3.50, days: 2, cod: true },
        { name: 'Hà Nam', rate: 3.50, days: 2, cod: true },
        { name: 'Hải Dương', rate: 3.50, days: 2, cod: true },
        { name: 'Hưng Yên', rate: 3.50, days: 2, cod: true },
        { name: 'Nam Định', rate: 3.80, days: 2, cod: true },
        { name: 'Ninh Bình', rate: 3.80, days: 2, cod: true },
        { name: 'Thái Bình', rate: 3.80, days: 2, cod: true },
        { name: 'Vĩnh Phúc', rate: 3.50, days: 2, cod: true },
        { name: 'Quảng Ninh', rate: 4.20, days: 3, cod: true },
        { name: 'Bắc Giang', rate: 4.00, days: 3, cod: true },
        { name: 'Phú Thọ', rate: 4.00, days: 3, cod: true },
        { name: 'Thái Nguyên', rate: 4.00, days: 3, cod: true },
        { name: 'Tuyên Quang', rate: 4.00, days: 3, cod: true },
        { name: 'Lạng Sơn', rate: 4.00, days: 3, cod: true },
        { name: 'Bắc Kạn', rate: 4.00, days: 3, cod: true },
        { name: 'Cao Bằng', rate: 4.00, days: 3, cod: true },
        { name: 'Hà Giang', rate: 4.00, days: 3, cod: true },
        { name: 'Lào Cai', rate: 5.00, days: 4, cod: true },
        { name: 'Yên Bái', rate: 5.00, days: 4, cod: true },
        { name: 'Hòa Bình', rate: 5.00, days: 4, cod: true },
        { name: 'Sơn La', rate: 5.50, days: 4, cod: true },
        { name: 'Điện Biên', rate: 6.00, days: 5, cod: false }, // Vùng sâu
        { name: 'Lai Châu', rate: 6.50, days: 5, cod: false },  // Vùng sâu
      ]
    },
    {
      region: 'Miền Trung & Tây Nguyên',
      provinces: [
        { name: 'Thanh Hóa', rate: 4.50, days: 3, cod: true },
        { name: 'Nghệ An', rate: 4.50, days: 3, cod: true },
        { name: 'Hà Tĩnh', rate: 4.50, days: 3, cod: true },
        { name: 'Quảng Bình', rate: 4.50, days: 3, cod: true },
        { name: 'Quảng Trị', rate: 4.50, days: 3, cod: true },
        { name: 'Thừa Thiên Huế', rate: 4.50, days: 3, cod: true },
        { name: 'Đà Nẵng', rate: 4.00, days: 3, cod: true },
        { name: 'Quảng Nam', rate: 4.50, days: 3, cod: true },
        { name: 'Quảng Ngãi', rate: 4.50, days: 3, cod: true },
        { name: 'Bình Định', rate: 4.50, days: 3, cod: true },
        { name: 'Phú Yên', rate: 4.50, days: 3, cod: true },
        { name: 'Khánh Hòa', rate: 4.20, days: 3, cod: true },
        { name: 'Ninh Thuận', rate: 4.50, days: 3, cod: true },
        { name: 'Bình Thuận', rate: 4.50, days: 3, cod: true },
        // Tây Nguyên
        { name: 'Kon Tum', rate: 5.50, days: 4, cod: true },
        { name: 'Gia Lai', rate: 5.50, days: 4, cod: true },
        { name: 'Đắk Lắk', rate: 5.00, days: 4, cod: true },
        { name: 'Đắk Nông', rate: 5.50, days: 4, cod: true },
        { name: 'Lâm Đồng', rate: 5.00, days: 4, cod: true },
      ]
    },
    {
      region: 'Miền Nam',
      provinces: [
        { name: 'Hồ Chí Minh', rate: 3.50, days: 2, cod: true },
        { name: 'Bà Rịa - Vũng Tàu', rate: 3.80, days: 2, cod: true },
        { name: 'Bình Dương', rate: 3.50, days: 2, cod: true },
        { name: 'Bình Phước', rate: 3.80, days: 2, cod: true },
        { name: 'Đồng Nai', rate: 3.50, days: 2, cod: true },
        { name: 'Tây Ninh', rate: 3.80, days: 2, cod: true },
        { name: 'Cần Thơ', rate: 4.00, days: 3, cod: true },
        { name: 'Long An', rate: 4.20, days: 3, cod: true },
        { name: 'Tiền Giang', rate: 4.20, days: 3, cod: true },
        { name: 'Bến Tre', rate: 4.20, days: 3, cod: true },
        { name: 'Trà Vinh', rate: 4.20, days: 3, cod: true },
        { name: 'Vĩnh Long', rate: 4.20, days: 3, cod: true },
        { name: 'Đồng Tháp', rate: 4.20, days: 3, cod: true },
        { name: 'An Giang', rate: 4.20, days: 3, cod: true },
        { name: 'Kiên Giang', rate: 4.20, days: 3, cod: true },
        { name: 'Hậu Giang', rate: 4.20, days: 3, cod: true },
        { name: 'Sóc Trăng', rate: 4.20, days: 3, cod: true },
        { name: 'Bạc Liêu', rate: 4.20, days: 3, cod: true },
        { name: 'Cà Mau', rate: 4.50, days: 4, cod: true },
      ]
    }
  ];
}
