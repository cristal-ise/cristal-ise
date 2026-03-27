import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { SelectButtonModule } from 'primeng/selectbutton';
import { FormsModule } from '@angular/forms';
import { ThemeService } from '../../core/services/theme.service';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CardModule, ButtonModule, SelectButtonModule, FormsModule],
  templateUrl: './settings.html',
  styleUrl: './settings.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Settings {
  themeService = inject(ThemeService);

  themeOptions = [
    { label: 'Light', value: false, icon: 'pi pi-sun' },
    { label: 'Dark', value: true, icon: 'pi pi-moon' }
  ];
}
