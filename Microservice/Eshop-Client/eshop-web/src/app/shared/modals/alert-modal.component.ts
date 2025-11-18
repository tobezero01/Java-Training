import { Component, Input } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

@Component({
  selector: 'app-alert-modal',
  imports: [],
  templateUrl: './alert-modal.component.html',
  styleUrl: './alert-modal.component.css'
})
export class AlertModalComponent {
  @Input() title?: string;
  @Input() message!: string;
  @Input() okText?: string;
  constructor(public activeModal: NgbActiveModal) {}
}
