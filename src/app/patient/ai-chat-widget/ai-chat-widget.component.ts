import { CommonModule } from '@angular/common';
import { Component, ElementRef, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { AiDoctorSuggestion, AiChatAppointment, AiChatTreatment, AiChatAlert } from '../../models/ai-chat.models';
import { AiChatService } from '../../services/ai-chat.service';
import { AuthService } from '../../services/auth.service';

type ChatRole = 'user' | 'assistant';

interface ChatBubble {
  role: ChatRole;
  content: string;
  doctors?: AiDoctorSuggestion[];
  nextAppointment?: AiChatAppointment;
  activeTreatments?: AiChatTreatment[];
  recentAlerts?: AiChatAlert[];
  connectedDoctors?: AiDoctorSuggestion[];
}

@Component({
  selector: 'app-ai-chat-widget',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './ai-chat-widget.component.html',
  styleUrls: ['./ai-chat-widget.component.css']
})
export class AiChatWidgetComponent {
  @ViewChild('messagesViewport') private messagesViewport?: ElementRef<HTMLDivElement>;

  isOpen = false;
  isSending = false;
  draftMessage = '';
  messages: ChatBubble[] = [
    {
      role: 'assistant',
      content: 'Bonjour ! Je suis votre assistant santé spécialisé dans le cancer du sein. Posez-moi vos questions sur vos symptômes, traitements, rendez-vous ou médecins. Je suis là pour vous accompagner. 💗'
    }
  ];

  quickActions = [
    { label: '📅 Mon prochain RDV', text: 'Quel est mon prochain rendez-vous ?' },
    { label: '💊 Mes traitements', text: 'Quels sont mes traitements en cours ?' },
    { label: '🩺 Trouver un radiologue', text: 'Je veux un radiologue' },
    { label: '⚠️ Mes alertes', text: 'Est-ce que j\'ai des alertes ?' },
    { label: '👨‍⚕️ Mes médecins', text: 'Quels sont mes médecins connectés ?' }
  ];

  private readonly sessionId: string;

  constructor(
    private aiChatService: AiChatService,
    private authService: AuthService,
    private router: Router
  ) {
    this.sessionId = this.resolveSessionId();
  }

  openChat(): void {
    this.isOpen = true;
    this.scrollToBottomSoon();
  }

  closeChat(): void {
    this.isOpen = false;
  }

  handleEnter(event: Event): void {
    const keyboardEvent = event as KeyboardEvent;
    if (keyboardEvent.shiftKey) return;
    keyboardEvent.preventDefault();
    this.sendMessage();
  }

  sendQuickAction(text: string): void {
    this.draftMessage = text;
    this.sendMessage();
  }

  sendMessage(): void {
    const message = this.draftMessage.trim();
    if (!message || this.isSending) return;

    this.isOpen = true;
    this.messages.push({ role: 'user', content: message });
    this.draftMessage = '';
    this.isSending = true;
    this.scrollToBottomSoon();

    const patientUserId = this.authService.currentUser?.id;

    this.aiChatService.sendMessage({
      sessionId: this.sessionId,
      message,
      patientUserId: patientUserId || undefined
    }).pipe(
      finalize(() => {
        this.isSending = false;
        this.scrollToBottomSoon();
      })
    ).subscribe({
      next: (response) => {
        this.messages.push({
          role: 'assistant',
          content: response.reply || 'Je n\'ai pas pu générer une réponse. Veuillez réessayer.',
          doctors: response.doctors ?? [],
          nextAppointment: response.nextAppointment,
          activeTreatments: response.activeTreatments ?? [],
          recentAlerts: response.recentAlerts ?? [],
          connectedDoctors: response.connectedDoctors ?? []
        });
      },
      error: (error) => {
        this.messages.push({
          role: 'assistant',
          content: this.resolveErrorMessage(error)
        });
      }
    });
  }

  navigateToMyDoctors(): void {
    this.router.navigate(['/patient/my-doctors']);
  }

  navigateToAppointments(): void {
    this.router.navigate(['/patient/appointments']);
  }

  trackByIndex(index: number): number {
    return index;
  }

  trackByDoctorId(index: number, doctor: AiDoctorSuggestion): string {
    return doctor.doctorProfileId || String(index);
  }

  trackByTreatmentId(index: number, t: AiChatTreatment): string {
    return t.id || String(index);
  }

  trackByAlertId(index: number, a: AiChatAlert): string {
    return a.id || String(index);
  }

  initials(fullName: string): string {
    if (!fullName) return 'DR';
    const parts = fullName.split(' ').filter(Boolean);
    if (parts.length < 2) return parts[0].slice(0, 2);
    return (parts[0][0] + parts[1][0]).toUpperCase();
  }

  formatDate(isoDate: string): string {
    if (!isoDate) return '';
    try {
      const d = new Date(isoDate);
      return d.toLocaleDateString('fr-FR', { weekday: 'short', day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' });
    } catch { return isoDate; }
  }

  treatmentLabel(type: string): string {
    const map: Record<string, string> = {
      CHEMO: 'Chimiothérapie', RADIO: 'Radiothérapie', SURGERY: 'Chirurgie',
      HORMONAL: 'Hormonothérapie', IMMUNOTHERAPY: 'Immunothérapie'
    };
    return map[type] || type;
  }

  severityClass(severity: string): string {
    const map: Record<string, string> = {
      LOW: 'severity--low', MEDIUM: 'severity--medium',
      HIGH: 'severity--high', CRITICAL: 'severity--critical'
    };
    return map[severity] || '';
  }

  severityLabel(severity: string): string {
    const map: Record<string, string> = {
      LOW: 'Faible', MEDIUM: 'Moyen', HIGH: 'Élevé', CRITICAL: 'Critique'
    };
    return map[severity] || severity;
  }

  cyclePercent(current?: number, total?: number): number {
    if (!current || !total || total === 0) return 0;
    return Math.min(Math.round((current / total) * 100), 100);
  }

  modeLabel(mode?: string): string {
    const map: Record<string, string> = {
      IN_PERSON: 'En personne', VIDEO: 'Vidéo', HYBRID: 'Hybride'
    };
    return map[mode || ''] || mode || '';
  }

  private resolveSessionId(): string {
    const currentUserId = this.authService.currentUser?.id;
    if (currentUserId) return `patient-ai-${currentUserId}`;

    const storageKey = 'patient-ai-session-id';
    const existingSessionId = localStorage.getItem(storageKey);
    if (existingSessionId) return existingSessionId;

    const generatedSessionId = `patient-ai-guest-${crypto.randomUUID()}`;
    localStorage.setItem(storageKey, generatedSessionId);
    return generatedSessionId;
  }

  private resolveErrorMessage(error: unknown): string {
    const responseBody = (error as { error?: { error?: string } })?.error;
    if (typeof responseBody === 'object' && responseBody && typeof responseBody.error === 'string') {
      return responseBody.error;
    }
    const message = (error as { message?: string })?.message;
    return message || 'L\'assistant IA est indisponible pour le moment. Veuillez réessayer.';
  }

  private scrollToBottomSoon(): void {
    setTimeout(() => {
      const viewport = this.messagesViewport?.nativeElement;
      if (!viewport) return;
      viewport.scrollTop = viewport.scrollHeight;
    });
  }
}