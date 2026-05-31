import { CommonModule, NgOptimizedImage } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal, input } from '@angular/core';
import { SelectModule } from 'primeng/select';
import { FormsModule } from '@angular/forms';
import { TranslocoService } from '@jsverse/transloco';

@Component({
  selector: 'app-language-selector',
  imports: [CommonModule, SelectModule, FormsModule, NgOptimizedImage],
  template: `
    <p-select [options]="languages" [ngModel]="currentLang()" (ngModelChange)="setLanguage($event)"
      optionLabel="label" optionValue="code" [styleClass]="styleClass()" [panelStyle]="{'min-width': '160px'}">
      <ng-template #selectedItem let-selectedOption>
          <div class="flex items-center gap-3 px-2">
              <img [ngSrc]="selectedOption.icon" width="20" height="15" alt="flag" class="rounded-sm shadow-sm" />
              <span [class]="labelClass()">{{ selectedOption.label }}</span>
          </div>
      </ng-template>
      <ng-template #item let-option>
          <div class="flex items-center gap-3 py-1">
              <img [ngSrc]="option.icon" width="20" height="15" alt="flag" class="rounded-sm shadow-sm" />
              <span class="text-sm font-bold">{{ option.label }}</span>
          </div>
      </ng-template>
    </p-select>
  `,
  styles: [`
    :host {
      display: block;
    }
    :host ::ng-deep .p-select {
        transition: all 0.2s ease;
    }
    :host ::ng-deep .p-select:hover {
        opacity: 0.8;
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class LanguageSelector {
  private translocoService = inject(TranslocoService);

  styleClass = input<string>('!bg-transparent !border-none !shadow-none !p-0 !min-w-[140px]');
  labelClass = input<string>('text-xs font-bold text-[var(--vibe-text)]');

  protected readonly currentLang = signal(this.translocoService.getActiveLang());

  protected readonly languages = [
    { label: 'English', code: 'en', icon: 'https://flagcdn.com/w20/gb.png' },
    { label: 'Français', code: 'fr', icon: 'https://flagcdn.com/w20/fr.png' },
    { label: 'Deutsch', code: 'de', icon: 'https://flagcdn.com/w20/de.png' },
    { label: 'Magyar', code: 'hu', icon: 'https://flagcdn.com/w20/hu.png' }
  ];

  setLanguage(lang: string) {
    this.translocoService.setActiveLang(lang);
    this.currentLang.set(lang);
  }
}
