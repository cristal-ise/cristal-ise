import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { AvatarModule } from 'primeng/avatar';
import { BadgeModule } from 'primeng/badge';
import { MenuModule } from 'primeng/menu';
import { SelectModule } from 'primeng/select';
import { FormsModule } from '@angular/forms';
import { MenuItem } from 'primeng/api';
import { TranslocoPipe } from '@jsverse/transloco';
import { TranslocoService } from '@jsverse/transloco';
import { AuthService } from '../../../core/services/auth.service';
import { DefaultService } from '../../../api';
import { finalize } from 'rxjs';

import { LanguageSelector } from '../language-selector/language-selector';

@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink, 
    ButtonModule, 
    InputTextModule, 
    AvatarModule, 
    BadgeModule,
    MenuModule,
    SelectModule,
    FormsModule,
    TranslocoPipe,
    LanguageSelector
  ],
  templateUrl: './topbar.html',
  styleUrl: './topbar.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    'class': 'block'
  }
})
export class Topbar {
  private router = inject(Router);
  private authService = inject(AuthService);
  private translocoService = inject(TranslocoService);
  
  protected readonly userMenuItems = computed<MenuItem[]>(() => {
    return [
      {
        label: this.translocoService.translate('layout.profile'),
        icon: 'pi pi-user',
        routerLink: ['/dashboard/settings']
      },
      {
        label: this.translocoService.translate('layout.settings'),
        icon: 'pi pi-cog',
        routerLink: ['/dashboard/settings']
      },
      {
        separator: true
      },
      {
        label: this.translocoService.translate('layout.logout'),
        icon: 'pi pi-sign-out',
        command: () => this.logout()
      }
    ];
  });

  logout() {
    this.authService.logout().subscribe({
        next: () => console.log('Successfully logged out'),
        error: (err) => console.error('Logout error:', err)
    });
  }
}
