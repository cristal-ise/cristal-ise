import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { AvatarModule } from 'primeng/avatar';
import { BadgeModule } from 'primeng/badge';
import { MenuModule } from 'primeng/menu';
import { MenuItem } from 'primeng/api';
import { AuthService } from '../../../core/services/auth.service';
import { DefaultService } from '../../../api';
import { finalize } from 'rxjs';

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
    MenuModule
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
  private defaultService = inject(DefaultService);

  userMenuItems: MenuItem[] = [
    {
      label: 'Profile',
      icon: 'pi pi-user',
      routerLink: ['/dashboard/settings']
    },
    {
      label: 'Settings',
      icon: 'pi pi-cog',
      routerLink: ['/dashboard/settings']
    },
    {
      separator: true
    },
    {
      label: 'Logout',
      icon: 'pi pi-sign-out',
      command: () => this.logout()
    }
  ];

  logout() {
    this.authService.logout().subscribe({
        next: () => console.log('Successfully logged out'),
        error: (err) => console.error('Logout error:', err)
    });
  }
}
