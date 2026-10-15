import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Auth, HOME } from './auth';

@Component({
  selector: 'app-login-page',
  imports: [FormsModule],
  template: `
    <form class="card narrow" #f="ngForm" (ngSubmit)="signIn(f.value)">
      <h2>Sign in</h2>
      <label>Username <input name="username" ngModel required autocomplete="username" /></label>
      <label>Password <input name="password" type="password" ngModel required autocomplete="current-password" /></label>
      <button type="submit" [disabled]="f.invalid">Sign in</button>
      @if (error()) {
        <p class="error" role="alert">{{ error() }}</p>
      }
    </form>
  `,
})
export class LoginPage {
  private readonly auth = inject(Auth);
  private readonly router = inject(Router);
  readonly error = signal('');

  constructor() {
    const user = this.auth.user();
    if (user) {
      this.router.navigateByUrl(HOME[user.role]);
    }
  }

  signIn({ username, password }: { username: string; password: string }) {
    this.auth.login(username, password).subscribe({
      next: (me) => this.router.navigateByUrl(HOME[me.role]),
      error: () => this.error.set('Wrong username or password'),
    });
  }
}
