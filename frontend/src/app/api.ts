import { DestroyRef, inject, WritableSignal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

/** Types mirror the backend JSON (docs/spec.md §13). */
export type Role = 'EMPLOYEE' | 'FRONT_DESK' | 'ADMIN';
export type VisitState =
  | 'EXPECTED'
  | 'AWAITING_HOST'
  | 'APPROVED'
  | 'DECLINED'
  | 'TIMED_OUT'
  | 'CHECKED_IN'
  | 'CHECKED_OUT';

export interface Me {
  id: number;
  username: string;
  fullName: string;
  role: Role;
}

export interface Employee {
  id: number;
  fullName: string;
}

export interface Visit {
  id: number;
  type: 'PRE_REGISTERED' | 'WALK_IN';
  state: VisitState;
  visitorName: string;
  visitorPhone: string | null;
  purpose: string;
  hostName: string;
  visitDate: string;
  arrivedAt: string | null;
  checkedInAt: string | null;
  checkedOutAt: string | null;
  passNumber: string | null;
  overstayed: boolean;
}

export const STATE_LABELS: Record<VisitState, string> = {
  EXPECTED: 'Expected',
  AWAITING_HOST: 'Waiting for host',
  APPROVED: 'Approved: issue a card',
  DECLINED: 'Declined: entry not granted',
  TIMED_OUT: 'No response: entry not granted',
  CHECKED_IN: 'On campus',
  CHECKED_OUT: 'Left',
};

/** Calls `load` now and every 5 seconds until the component is destroyed. */
export function poll(load: () => void): void {
  load();
  const timer = setInterval(load, 5000);
  inject(DestroyRef).onDestroy(() => clearInterval(timer));
}

/** Runs a request, shows the server's problem detail on failure, and calls `done` on success. */
export function act(request: Observable<unknown>, error: WritableSignal<string>, done: () => void): void {
  request.subscribe({
    next: () => {
      error.set('');
      done();
    },
    error: (e: HttpErrorResponse) => error.set(e.error?.detail ?? `Request failed (${e.status})`),
  });
}
