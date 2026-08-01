import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { SidebarDoctorComponent } from '../sidebar-doctor/sidebar-doctor.component';

@Component({
  selector: 'app-doctor-layout',
  standalone: true,
  imports: [RouterOutlet, SidebarDoctorComponent],
  template: `
    <app-sidebar-doctor></app-sidebar-doctor>
    <main class="doctor-content">
      <router-outlet></router-outlet>
    </main>
  `,
  styles: [`
    :host {
      display: flex;
      min-height: 100vh;
      background: #f6eef3;
    }
    .doctor-content {
      margin-left: 270px;
      flex: 1;
      padding: 2rem 2.5rem;
      min-height: 100vh;
      position: relative; /* Ensure absolute children are confined */
      transition: margin-left .35s cubic-bezier(.22, 1, .36, 1), padding .35s cubic-bezier(.22, 1, .36, 1);
    }
    /* Messages special: take all surface */
    :host:has(app-doctor-messages) .doctor-content {
      padding: 0;
    }
    /* Compact mode selector using :has() */
    :host:has(app-sidebar-doctor .compact) .doctor-content {
      margin-left: 72px;
      padding: 2rem 1.5rem;
    }
    :host:has(app-sidebar-doctor .compact):has(app-doctor-messages) .doctor-content {
      padding: 0;
    }

    @media (max-width: 768px) {
      .doctor-content {
        margin-left: 0;
        padding: 5rem 1rem 2rem;
      }
      :host:has(app-doctor-messages) .doctor-content {
        padding: 0;
      }
      /* On mobile, sidebar overlays content, no shift needed */
      :host:has(app-sidebar-doctor .compact) .doctor-content {
        margin-left: 0;
      }
    }
  `]
})
export class DoctorLayoutComponent {}
