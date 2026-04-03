import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { ButtonModule } from 'primeng/button';
import { SelectButtonModule } from 'primeng/selectbutton';
import { FormsModule } from '@angular/forms';
import { ThemeService } from '../../core/services/theme.service';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule, ButtonModule, SelectButtonModule, FormsModule, TranslocoPipe],
  templateUrl: './settings.html',
  styleUrl: './settings.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Settings {
  protected readonly themeService = inject(ThemeService);
  private translocoService = inject(TranslocoService);

  themeOptions = [
    { label: 'settings.light', value: false, icon: 'pi pi-sun' },
    { label: 'settings.dark', value: true, icon: 'pi pi-moon' }
  ];
}
