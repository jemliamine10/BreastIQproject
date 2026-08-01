import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AllergyResponseDto } from '../../models/allergy-response.dto';
import { AllergySeverity } from '../../models/enums';

@Component({
  selector: 'app-allergies',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './allergies.component.html',
  styleUrl: './allergies.component.css'
})
export class AllergiesComponent implements OnInit {
  @Input() allergies: AllergyResponseDto[] = [];

  // Mock data — static, no service call
  mockAllergies: AllergyResponseDto[] = [
    {
      id: '1',
      patientProfileId: 'p1',
      substance: 'Pénicilline',
      reaction: 'Éruption cutanée, urticaire généralisée',
      severity: AllergySeverity.HIGH
    },
    {
      id: '2',
      patientProfileId: 'p1',
      substance: 'Arachides',
      reaction: "Gonflement des lèvres, difficulté à avaler",
      severity: AllergySeverity.HIGH
    },
    {
      id: '3',
      patientProfileId: 'p1',
      substance: 'Ibuprofène',
      reaction: 'Douleurs abdominales, nausées légères',
      severity: AllergySeverity.MEDIUM
    },
    {
      id: '4',
      patientProfileId: 'p1',
      substance: 'Latex',
      reaction: 'Rougeur légère au contact',
      severity: AllergySeverity.LOW
    },
    {
      id: '5',
      patientProfileId: 'p1',
      substance: 'Pollen de bouleau',
      reaction: 'Rhinite, yeux larmoyants',
      severity: AllergySeverity.LOW
    }
  ];

  displayedAllergies: AllergyResponseDto[] = [];

  ngOnInit(): void {
    this.displayedAllergies = this.allergies.length > 0 ? this.allergies : this.mockAllergies;
  }

  getSeverityLabel(severity: AllergySeverity): string {
    const labels: Record<AllergySeverity, string> = {
      [AllergySeverity.LOW]: 'Légère',
      [AllergySeverity.MEDIUM]: 'Modérée',
      [AllergySeverity.HIGH]: 'Sévère'
    };
    return labels[severity] ?? severity;
  }

  getSeverityClass(severity: AllergySeverity): string {
    const classes: Record<AllergySeverity, string> = {
      [AllergySeverity.LOW]: 'mild',
      [AllergySeverity.MEDIUM]: 'moderate',
      [AllergySeverity.HIGH]: 'severe'
    };
    return classes[severity] ?? '';
  }

  getSeverityIcon(severity: AllergySeverity): string {
    if (severity === AllergySeverity.HIGH) {
      return 'M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z|M12 9v4|M12 17h.01';
    }
    if (severity === AllergySeverity.MEDIUM) {
      return 'M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z';
    }
    return 'M22 11.08V12a10 10 0 11-5.93-9.14|M22 4L12 14.01l-3-3';
  }

  parsePaths(icon: string): string[] {
    return icon.split('|');
  }

  get severeCount(): number {
    return this.displayedAllergies.filter(a => a.severity === AllergySeverity.HIGH).length;
  }
  get moderateCount(): number {
    return this.displayedAllergies.filter(a => a.severity === AllergySeverity.MEDIUM).length;
  }
  get mildCount(): number {
    return this.displayedAllergies.filter(a => a.severity === AllergySeverity.LOW).length;
  }
}
