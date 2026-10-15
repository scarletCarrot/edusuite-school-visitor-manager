import { Component, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { DatePipe } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import { act, Employee, poll, STATE_LABELS, Visit } from './api';

/** Front desk: register walk-ins, issue lanyard cards, check visitors out, see who is on campus. */
@Component({
  selector: 'app-desk-page',
  imports: [FormsModule, DatePipe],
  template: `
    <section class="cards">
      <form class="card" #walkIn="ngForm" (ngSubmit)="registerWalkIn(walkIn)">
        <h2>Register walk-in</h2>
        <label>Visitor name <input name="visitorName" ngModel required maxlength="100" /></label>
        <label>Mobile (optional) <input name="visitorPhone" ngModel maxlength="30" /></label>
        <label>Purpose <input name="purpose" ngModel required maxlength="200" /></label>
        <label
          >Here to see
          <select name="hostId" ngModel required>
            @for (employee of employees(); track employee.id) {
              <option [ngValue]="employee.id">{{ employee.fullName }}</option>
            }
          </select>
        </label>
        <button type="submit" [disabled]="walkIn.invalid">Register and notify host</button>
      </form>
      <form class="card" #returned="ngForm" (ngSubmit)="checkOut(returned.value.passNumber, returned)">
        <h2>Check out</h2>
        <label>Returned card number <input name="passNumber" ngModel required maxlength="20" /></label>
        <button type="submit" [disabled]="returned.invalid">Check out</button>
      </form>
    </section>

    @if (error()) {
      <p class="error" role="alert">{{ error() }}</p>
    }

    <h2>Today · {{ onCampus() }} on campus · {{ overstayed() }} overstayed</h2>
    <table>
      <thead>
        <tr><th>Visitor</th><th>Host</th><th>Status</th><th>Card</th><th>Arrived</th><th>Action</th></tr>
      </thead>
      <tbody>
        @for (visit of visits(); track visit.id) {
          <tr [class.overstayed]="visit.overstayed">
            <td>{{ visit.visitorName }}<br /><small>{{ visit.purpose }}</small></td>
            <td>{{ visit.hostName }}</td>
            <td>{{ labels[visit.state] }}@if (visit.overstayed) { · <strong>Overstayed</strong> }</td>
            <td>{{ visit.passNumber }}</td>
            <td>{{ visit.arrivedAt | date: 'MMM d, h:mm a' }}</td>
            <td>
              @if (visit.state === 'EXPECTED' || visit.state === 'APPROVED') {
                <input #card size="6" placeholder="Card no." [attr.aria-label]="'Card for ' + visit.visitorName" />
                <button (click)="checkIn(visit, card.value)">Check in</button>
              } @else if (visit.state === 'CHECKED_IN') {
                <button (click)="checkOut(visit.passNumber!)">Check out</button>
              }
            </td>
          </tr>
        } @empty {
          <tr><td colspan="6">No visits today yet.</td></tr>
        }
      </tbody>
    </table>
  `,
})
export class DeskPage {
  private readonly http = inject(HttpClient);
  protected readonly labels = STATE_LABELS;
  readonly visits = signal<Visit[]>([]);
  readonly employees = signal<Employee[]>([]);
  readonly error = signal('');
  readonly onCampus = computed(() => this.visits().filter((v) => v.state === 'CHECKED_IN').length);
  readonly overstayed = computed(() => this.visits().filter((v) => v.overstayed).length);

  constructor() {
    this.http.get<Employee[]>('/api/employees').subscribe((e) => this.employees.set(e));
    poll(() => this.load());
  }

  load() {
    this.http.get<Visit[]>('/api/visits/today').subscribe((v) => this.visits.set(v));
  }

  registerWalkIn(form: NgForm) {
    act(this.http.post('/api/visits/walk-ins', form.value), this.error, () => {
      form.resetForm();
      this.load();
    });
  }

  checkIn(visit: Visit, passNumber: string) {
    if (!passNumber.trim()) {
      return this.error.set(`Enter the lanyard card number for ${visit.visitorName}`);
    }
    act(this.http.post(`/api/visits/${visit.id}/check-in`, { passNumber }), this.error, () => this.load());
  }

  checkOut(passNumber: string, form?: NgForm) {
    act(this.http.post('/api/visits/check-out', { passNumber }), this.error, () => {
      form?.resetForm();
      this.load();
    });
  }
}
