export interface PaypalCaptureResponse {
  status: string;     // COMPLETED
  captureId: string;
  validation?: any;
}
