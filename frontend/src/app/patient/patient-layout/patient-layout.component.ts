import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { SidebarPatientComponent } from '../sidebar-patient/sidebar-patient.component';
import { AiChatWidgetComponent } from '../ai-chat-widget/ai-chat-widget.component';

@Component({
  selector: 'app-patient-layout',
  standalone: true,
  imports: [RouterOutlet, SidebarPatientComponent, AiChatWidgetComponent],
  template: `
    <app-sidebar-patient />
    <main class="patient-content">
      <router-outlet />
    </main>
    <app-ai-chat-widget />
  `,
  styles: [`
    :host {
      display: flex;
      min-height: 100vh;
      background: linear-gradient(165deg, #f8f1f5 0%, #f5ecf1 35%, #f2e9ee 65%, #f6eef3 100%);
    }
    .patient-content {
      margin-left: 270px;
      flex: 1;
      padding: 2rem 2.5rem;
      min-height: 100vh;
      position: relative; /* Ensure absolute children are confined */
      transition: margin-left .35s cubic-bezier(.22, 1, .36, 1);
      animation: contentFadeIn 0.5s cubic-bezier(.22, 1, .36, 1) both;
    }
    /* Messages special: take all surface */
    :host:has(app-messages) .patient-content {
      padding: 0;
    }
    @keyframes contentFadeIn {
      from { opacity: 0; transform: translateY(8px); }
      to   { opacity: 1; transform: translateY(0); }
    }
    /* Compact sidebar support — listens to sidebar CSS var */
    :host-context(.compact) .patient-content,
    :host:has(app-sidebar-patient .compact) .patient-content {
      margin-left: 72px;
    }
    :host:has(app-sidebar-patient .compact):has(app-messages) .patient-content {
      padding: 0;
    }

    @media (max-width: 768px) {
      .patient-content {
        margin-left: 0;
        padding: 4.5rem 1rem 1.5rem;
      }
      :host:has(app-messages) .patient-content {
        padding: 0;
      }
    }
  `]
})
export class PatientLayoutComponent {}
