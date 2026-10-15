import { Component, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { DatePipe } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import { act, poll, STATE_LABELS, Visit } from './api';

interface Notification {
  id: number;
  message: string;
  createdAt: string;
}

/** Employee: pre-register visitors, answer walk-ins, read notifications. */
@Component({
  selector: 'app-host-page',
  imports: [FormsModule, DatePipe],
  template: `
    <section class="cards">
      <form class="card" #preRegistration="ngForm" (ngSubmit)="preRegister(preRegistration)">
        <h2>Pre-register a visitor</h2>
        <label>Visitor name <input name="visitorName" ngModel required maxlength="100" /></label>
        <label>Mobile (optional) <input name="visitorPhone" ngModel maxlength="30" /></label>
        <label>Purpose <input name="purpose" ngModel required maxlength="200" /></label>
        <label>Date <input name="visitDate" type="date" [ngModel]="today" [min]="today" required /></label>
        <button type="submit" [disabled]="preRegistration.invalid">Pre-register</button>
      </form>
      <div class="card">
        <h2>Notifications</h2>
        <ul>
          @for (notification of notifications(); track notification.id) {
            <li><small>{{ notification.createdAt | date: 'MMM d, h:mm a' }}</small> {{ notification.message }}</li>
          } @empty {
            <li>None yet.</li>
          }
        </ul>
      </div>
    </section>

    @if (error()) {
      <p class="error" role="alert">{{ error() }}</p>
    }

    <h2>My visitors · {{ waiting() }} waiting for your answer</h2>
    <table>
      <thead>
        <tr><th>Visitor</th><th>Date</th><th>Status</th><th>Card</th><th>Action</th></tr>
      </thead>
      <tbody>
        @for (visit of visits(); track visit.id) {
          <tr>
            <td>{{ visit.visitorName }}<br /><small>{{ visit.purpose }}</small></td>
            <td>{{ visit.visitDate | date: 'MMM d' }}</td>
            <td>{{ labels[visit.state] }}</td>
            <td>{{ visit.passNumber }}</td>
            <td>
              @if (visit.state === 'AWAITING_HOST') {
                <button (click)="respond(visit, 'confirm')">Confirm</button>
                <button class="secondary" (click)="respond(visit, 'decline')">Decline</button>
              }
            </td>
          </tr>
        } @empty {
          <tr><td colspan="5">No visitors yet.</td></tr>
        }
      </tbody>
    </table>
  `,
})
export class HostPage {
  private readonly http = inject(HttpClient);
  protected readonly labels = STATE_LABELS;
  protected readonly today = new Date().toLocaleDateString('en-CA'); // yyyy-MM-dd
  readonly visits = signal<Visit[]>([]);
  readonly notifications = signal<Notification[]>([]);
  readonly error = signal('');
  readonly waiting = computed(() => this.visits().filter((v) => v.state === 'AWAITING_HOST').length);

  constructor() {
    poll(() => this.load());
  }

  load() {
    this.http.get<Visit[]>('/api/visits/mine').subscribe((v) => this.visits.set(v));
    this.http.get<Notification[]>('/api/notifications').subscribe((n) => this.notifications.set(n));
  }

  preRegister(form: NgForm) {
    act(this.http.post('/api/visits/pre-registrations', form.value), this.error, () => {
      form.resetForm({ visitDate: this.today });
      this.load();
    });
  }

  respond(visit: Visit, answer: 'confirm' | 'decline') {
    act(this.http.post(`/api/visits/${visit.id}/${answer}`, null), this.error, () => this.load());
  }
}
