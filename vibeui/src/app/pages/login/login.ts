import { ChangeDetectionStrategy, Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { CardModule } from 'primeng/card';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';
import { DefaultService } from '../../api';
import { finalize } from 'rxjs';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule,
    CardModule,
    InputTextModule,
    PasswordModule,
    ButtonModule,
    CheckboxModule,
    ReactiveFormsModule,
    ToastModule
  ],
  templateUrl: './login.html',
  styleUrl: './login.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Login implements OnInit {
  private fb = inject(FormBuilder);
  private router = inject(Router);
  private authService = inject(AuthService);
  private messageService = inject(MessageService);

  loading = signal(false);

  loginForm = this.fb.group({
    username: ['', [Validators.required]],
    password: ['', [Validators.required]],
    remember: [false]
  });

  ngOnInit() {
    if (history.state?.loggedOutByTimeout) {
      // Clear the state to prevent the message from showing again on page reload
      history.replaceState({ ...history.state, loggedOutByTimeout: false }, '');

      setTimeout(() => {
        this.messageService.add({
          key: 'system',
          severity: 'info',
          summary: 'Session Expired',
          detail: 'You were automatically logged out due to inactivity.',
          sticky: true,
          closable: true
        });
      });
    }
  }

  onSubmit() {
    if (this.loginForm.invalid) {
      return;
    }

    this.messageService.clear();

    const { username, password } = this.loginForm.value;
    if (!username || !password) return;

    this.loading.set(true);

    // Base64 encode as per OpenAPI spec
    // Note: btoa is safe for Latin-1 characters
    const b64User = btoa(username);
    const b64Pass = btoa(password);

    this.authService.login({
      username: b64User,
      password: b64Pass
    }).pipe(
      finalize(() => this.loading.set(false))
    ).subscribe({
      next: (result) => {
        console.log('Login successful:', result);
        const returnUrl = this.authService.redirectUrl || '/dashboard';
        this.authService.redirectUrl = null; // Clear to prevent stale redirects
        this.router.navigateByUrl(returnUrl);
      },
      error: (err) => {
        console.error('Login failed:', err);
        let errorMsg = 'Invalid credentials';

        // Handle JAX-RS JSON error format if provided
        if (err.error && typeof err.error === 'object') {
          errorMsg = err.error.message || err.error.errorMessage || err.error.error || errorMsg;
        } else if (err.status === 401) {
          errorMsg = 'Incorrect username or password';
        }

        this.messageService.add({
          severity: 'error',
          summary: 'Login Failed',
          detail: errorMsg,
          life: 3000
        });
      }
    });
  }
}
