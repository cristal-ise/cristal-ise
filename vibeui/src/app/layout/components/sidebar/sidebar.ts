import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { ButtonModule } from 'primeng/button';

@Component({
  selector: 'app-sidebar',
  imports: [RouterLink, RouterLinkActive, ButtonModule, TranslocoPipe],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Sidebar {
  menuItems = [
    { label: 'layout.dashboard_label', icon: 'pi pi-home', routerLink: '/dashboard' },
    { label: 'layout.users_label', icon: 'pi pi-users', routerLink: '/dashboard/users' },
    { label: 'layout.settings_label', icon: 'pi pi-cog', routerLink: '/dashboard/settings' }
  ];
}
