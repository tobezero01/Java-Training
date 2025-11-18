export class ApiError extends Error {
  constructor(
    public status: number,
    public override message: string,
    public details?: any
  ) {
    super(message);
  }
}
