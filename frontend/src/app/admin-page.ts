import { Component, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { DatePipe } from '@angular/common';
import { poll, Visit } from './api';

interface AuditEvent {
  id: number;
  occurredAt: string;
  actor: string;
  action: string;
  visitId: number | null;
  details: string | null;
}

/** Principal / admin office: read-only view of who is on campus and the audit trail. */
@Component({
  selector: 'app-admin-page',
  imports: [DatePipe],
  template: `
    <h2>On campus now: {{ onCampus().length }} · {{ overstayed() }} overstayed</h2>
    <table>
      <thead>
        <tr><th>Visitor</th><th>Host</th><th>Card</th><th>Checked in</th><th></th></tr>
      </thead>
      <tbody>
        @for (visit of onCampus(); track visit.id) {
          <tr [class.overstayed]="visit.overstayed">
            <td>{{ visit.visitorName }}<br /><small>{{ visit.purpose }}</small></td>
            <td>{{ visit.hostName }}</td>
            <td>{{ visit.passNumber }}</td>
            <td>{{ visit.checkedInAt | date: 'MMM d, h:mm a' }}</td>
            <td>@if (visit.overstayed) { <strong>Overstayed</strong> }</td>
          </tr>
        } @empty {
          <tr><td colspan="5">No visitors on campus.</td></tr>
        }
      </tbody>
    </table>

    <h2>Audit trail</h2>
    <table>
      <thead>
        <tr><th>Time</th><th>Actor</th><th>Action</th><th>Visit</th><th>Details</th></tr>
      </thead>
      <tbody>
        @for (event of audit(); track event.id) {
          <tr>
            <td>{{ event.occurredAt | date: 'MMM d, h:mm:ss a' }}</td>
            <td>{{ event.actor }}</td>
            <td>{{ event.action }}</td>
            <td>{{ event.visitId }}</td>
            <td>{{ event.details }}</td>
          </tr>
        }
      </tbody>
    </table>
  `,
})
export class AdminPage {
  private readonly http = inject(HttpClient);
  private readonly visits = signal<Visit[]>([]);
  readonly audit = signal<AuditEvent[]>([]);
  readonly onCampus = computed(() => this.visits().filter((v) => v.state === 'CHECKED_IN'));
  readonly overstayed = computed(() => this.onCampus().filter((v) => v.overstayed).length);

  constructor() {
    poll(() => {
      this.http.get<Visit[]>('/api/visits/today').subscribe((v) => this.visits.set(v));
      this.http.get<AuditEvent[]>('/api/audit').subscribe((a) => this.audit.set(a));
    });
  }
}
