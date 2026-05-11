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
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { SearchTextService } from '../../../core/services/search-text.service';

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
  private searchService = inject(SearchTextService);
  private searchSubject = new Subject<string>();

  protected searchText = this.searchService.searchText;

  constructor() {
    this.searchSubject.pipe(
      debounceTime(200),
      distinctUntilChanged(),
      takeUntilDestroyed()
    ).subscribe(text => {
      this.searchService.setSearchText(text);
    });
  }

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

  onSearch(event: Event) {
    const text = (event.target as HTMLInputElement).value;
    this.searchSubject.next(text);
  }

  logout() {
    this.authService.logout().subscribe({
        next: () => console.log('Successfully logged out'),
        error: (err) => console.error('Logout error:', err)
    });
  }
}
