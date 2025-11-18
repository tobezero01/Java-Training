import { AbstractControl, ValidationErrors, ValidatorFn } from "@angular/forms";

export const passwordStrength = (): ValidatorFn => {
  const regex = /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d!@#$%^&*()_+\-=\[\]{};':",.<>/?]{6,}$/;
  return (c: AbstractControl): ValidationErrors | null => {
    const v = String(c.value ?? '');
    return v && !regex.test(v) ? { weakPassword: true } : null;
  };
}

export const confirmPassword = (matchTo: string): ValidatorFn => {
  return (c: AbstractControl): ValidationErrors | null => {
    const parent = c.parent as any;
    if (!parent) return null;
    return c.value !== parent.controls?.[matchTo]?.value ? { passwordMismatch: true } : null;
  };
};
