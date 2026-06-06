import { Injectable, signal, effect } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class ThemeService {
  private readonly THEME_KEY = 'vibe-theme';
  isDarkMode = signal<boolean>(this.getInitialTheme());

  constructor() {
    // Synchronize HTML class with signal state
    effect(() => {
      const dark = this.isDarkMode();
      if (dark) {
        document.documentElement.classList.add('my-app-dark');
      } else {
        document.documentElement.classList.remove('my-app-dark');
      }
      localStorage.setItem(this.THEME_KEY, dark ? 'dark' : 'light');
    });
  }

  toggleTheme() {
    this.isDarkMode.update(v => !v);
  }

  setTheme(dark: boolean) {
    this.isDarkMode.set(dark);
  }

  private getInitialTheme(): boolean {
    const saved = localStorage.getItem(this.THEME_KEY);
    if (saved) {
      return saved === 'dark';
    }
    // Default to dark as requested in previous conversations
    return true;
  }
}
