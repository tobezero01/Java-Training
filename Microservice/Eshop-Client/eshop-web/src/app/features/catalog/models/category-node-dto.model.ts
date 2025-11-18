export interface CategoryNodeDto {
  id: number;
  name: string;
  alias: string;
  image?: string;
  children: CategoryNodeDto[];
}
