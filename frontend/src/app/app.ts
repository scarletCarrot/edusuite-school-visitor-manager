import { Component, inject } from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';
import { Auth } from './auth';

@Component({
  imports: [RouterOutlet],
  selector: 'app-root',
  template: `
    <header>
      <h1>EduSuite Visitors</h1>
      @if (auth.user(); as user) {
        <span>{{ user.fullName }} · {{ user.role }}</span>
        <button class="secondary" (click)="signOut()">Sign out</button>
      }
    </header>
    <main><router-outlet /></main>
  `,
})
export class App {
  protected readonly auth = inject(Auth);
  private readonly router = inject(Router);

  signOut() {
    this.auth.logout();
    this.router.navigateByUrl('/');
  }
}
