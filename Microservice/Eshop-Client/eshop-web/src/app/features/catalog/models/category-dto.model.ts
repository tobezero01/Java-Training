export interface CategoryDto {
  id: number;
  name: string;
  alias: string;
  imagePath?: string;
  hasChildren: boolean;
}
