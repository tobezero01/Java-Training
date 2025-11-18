export function mormalizeKeyword(q: string): string {
  return (q || '').trim().replace(/\s+/g, ' ');
}
