import { Directive, ElementRef, inject, input, effect } from '@angular/core';
import { Router } from '@angular/router';
import { TranslocoService } from '@jsverse/transloco';
import { DomainService } from '../services/domain.service';

@Directive({
  selector: '[itemData]',
  standalone: true
})
export class ItemDataDirective {
  itemData = input<string | undefined>();

  private domainService = inject(DomainService);
  private translocoService = inject(TranslocoService);
  private router = inject(Router);
  private el = inject(ElementRef);

  private uuidRegex = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/;

  constructor() {
    effect(() => {
      const data = this.itemData();
      if (!data) {
        this.el.nativeElement.textContent = '';
        return;
      }

      if (this.uuidRegex.test(data)) {
        this.handleUuid(data);
      } else {
        this.handleText(data);
      }
    });
  }

  private handleUuid(uuid: string): void {
    console.log('Handling UUID:', uuid);
    this.domainService.resolveUuid(uuid).subscribe(name => {
      this.el.nativeElement.textContent = this.translocoService.translate(name);
      this.el.nativeElement.style.cursor = 'pointer';
      this.el.nativeElement.style.textDecoration = 'underline';

      this.el.nativeElement.onclick = () => {
        this.router.navigate(['/admin/items', uuid]);
      };
    });
  }

  private handleText(text: string): void {
    console.log('Handling text:', text);
    this.el.nativeElement.textContent = this.translocoService.translate(text);
    this.el.nativeElement.style.cursor = 'default';
    this.el.nativeElement.style.textDecoration = 'none';
    this.el.nativeElement.onclick = null;
  }
}
