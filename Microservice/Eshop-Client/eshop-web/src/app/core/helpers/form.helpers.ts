import { AbstractControl } from '@angular/forms'

export function markAllDirty(control: AbstractControl): void {
  control.markAsDirty({ onlySelf: true})
  control.markAsTouched({ onlySelf: true })
  if ('controls' in control) {
    const group = control as any;
    Object.keys(group.controls).forEach(k => markAllDirty(group.controls[k]));
  }
}
