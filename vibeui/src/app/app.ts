import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ThemeService } from './core/services/theme.service';
import { AuthService } from './core/services/auth.service';
import { inject, OnInit } from '@angular/core';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AppComponent implements OnInit {
  title = 'vibeui';
  private themeService = inject(ThemeService);
  private authService = inject(AuthService);

  ngOnInit() {
    // Check if the user is already authenticated (e.g. via session cookie)
    this.authService.checkSession().subscribe();
  }
}
