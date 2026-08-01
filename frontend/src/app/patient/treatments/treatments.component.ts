import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TreatmentResponseDto } from '../../models/treatment-response.dto';

@Component({
  selector: 'app-treatments',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './treatments.component.html',
  styleUrl: './treatments.component.css'
})
export class TreatmentsComponent implements OnInit {
  @Input() treatments: TreatmentResponseDto[] = [];

  // Static mock data — no service calls
  mockTreatments: TreatmentResponseDto[] = [
    {
      id: '1',
      name: 'Tamoxifène 20mg',
      description: 'Hormonothérapie anti-œstrogénique de référence pour le cancer du sein HR+. Administration quotidienne par voie orale.',
      startDate: '2024-01-15',
      endDate: '2029-01-15'
    },
    {
      id: '2',
      name: 'Chimiothérapie AC-T',
      description: 'Protocole doxorubicine + cyclophosphamide suivi de paclitaxel. 8 cycles de 21 jours. Phase terminée avec succès.',
      startDate: '2023-06-01',
      endDate: '2023-12-20'
    },
    {
      id: '3',
      name: 'Radiothérapie adjuvante',
      description: 'Irradiation conformationnelle du sein droit, 25 séances, dose totale 50 Gy. Boost focalisé sur lit tumoral 10 Gy.',
      startDate: '2024-01-05',
      endDate: '2024-02-15'
    },
    {
      id: '4',
      name: 'Trastuzumab (Herceptin)',
      description: 'Anticorps monoclonal anti-HER2. Perfusions IV toutes les 3 semaines. Surveillance cardiaque mensuelle associée.',
      startDate: '2023-06-01',
      endDate: '2024-06-01'
    },
    {
      id: '5',
      name: 'Vitamine D3 + Calcium',
      description: 'Supplémentation préventive pour contrecarrer les effets osseux de l\'hormonothérapie. 1000 UI/jour.',
      startDate: '2024-01-15',
      endDate: undefined
    }
  ];

  displayedTreatments: TreatmentResponseDto[] = [];

  ngOnInit(): void {
    this.displayedTreatments = this.treatments.length > 0 ? this.treatments : this.mockTreatments;
  }

  getStatus(t: TreatmentResponseDto): 'active' | 'completed' | 'ongoing' {
    if (!t.endDate) return 'ongoing';
    const end = new Date(t.endDate);
    const now = new Date();
    return end >= now ? 'active' : 'completed';
  }

  getStatusLabel(t: TreatmentResponseDto): string {
    const s = this.getStatus(t);
    if (s === 'active') return 'En cours';
    if (s === 'ongoing') return 'Indéfini';
    return 'Terminé';
  }

  formatDate(date?: string): string {
    if (!date) return '—';
    return new Date(date).toLocaleDateString('fr-FR', { day: '2-digit', month: 'long', year: 'numeric' });
  }

  getDuration(t: TreatmentResponseDto): string {
    if (!t.startDate) return '—';
    const start = new Date(t.startDate);
    const end = t.endDate ? new Date(t.endDate) : new Date();
    const months = Math.round((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24 * 30));
    if (months < 1) return '< 1 mois';
    if (months === 1) return '1 mois';
    return `${months} mois`;
  }

  get activeCount(): number {
    return this.displayedTreatments.filter(t => this.getStatus(t) !== 'completed').length;
  }
  get completedCount(): number {
    return this.displayedTreatments.filter(t => this.getStatus(t) === 'completed').length;
  }
}
