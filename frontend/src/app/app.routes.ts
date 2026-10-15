import { Routes } from '@angular/router';
import { requireRole } from './auth';
import { LoginPage } from './login-page';
import { DeskPage } from './desk-page';
import { HostPage } from './host-page';
import { AdminPage } from './admin-page';

export const routes: Routes = [
  { path: '', component: LoginPage },
  { path: 'desk', component: DeskPage, canActivate: [requireRole('FRONT_DESK')] },
  { path: 'host', component: HostPage, canActivate: [requireRole('EMPLOYEE')] },
  { path: 'admin', component: AdminPage, canActivate: [requireRole('ADMIN')] },
  { path: '**', redirectTo: '' },
];
