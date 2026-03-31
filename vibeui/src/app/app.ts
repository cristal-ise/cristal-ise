import { ChangeDetectionStrategy, Component, HostListener } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ThemeService } from './core/services/theme.service';
import { AuthService } from './core/services/auth.service';
import { SessionTimeoutService } from './core/services/session-timeout.service';
import { inject, OnInit } from '@angular/core';
import { ToastModule } from 'primeng/toast';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, ToastModule],
  templateUrl: './app.html',
  styleUrl: './app.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AppComponent implements OnInit {
  title = 'vibeui';
  private themeService = inject(ThemeService);
  private authService = inject(AuthService);
  private sessionTimeoutService = inject(SessionTimeoutService);

  ngOnInit() {
    // Check if the user is already authenticated (e.g. via session cookie)
    this.authService.checkSession().subscribe();
  }

  @HostListener('window:beforeunload')
  onWindowClose() {
    if (this.authService.isAuthenticated()) {
      this.authService.logout('windowClose').subscribe();
    }
  }
}
