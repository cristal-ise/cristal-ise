import { Injectable, inject } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { MessageService } from 'primeng/api';
import { TranslocoService } from '@jsverse/transloco';

@Injectable({
  providedIn: 'root'
})
export class ApiErrorService {
  private messageService = inject(MessageService);
  private translocoService = inject(TranslocoService);

  /**
   * Handles API errors by displaying a PrimeNG Toast message.
   * 
   * @param error The HttpErrorResponse from the service call
   */
  handleError(error: HttpErrorResponse): void {
    this.messageService.add({
      key: 'system',
      severity: 'error',
      summary: this.translocoService.translate(error.statusText || 'Error'),
      detail: this.translocoService.translate(error.error || 'Unknown error'),
      sticky: true,
      closable: true
    });
  }
}
