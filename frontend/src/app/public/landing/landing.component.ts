import { Component, HostListener, OnInit, OnDestroy, AfterViewInit, ElementRef, QueryList, ViewChildren } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { animate, style, transition, trigger, state, keyframes } from '@angular/animations';

interface FaqItem {
  question: string;
  answer: string;
  isOpen: boolean;
}

interface Testimonial {
  name: string;
  role: string;
  content: string;
  rating: number;
}

interface Stat {
  value: string;
  label: string;
  suffix?: string;
}

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './landing.component.html',
  styleUrl: './landing.component.css',
  animations: [
    trigger('fadeInUp', [
      transition(':enter', [
        style({ opacity: 0, transform: 'translateY(40px)' }),
        animate('0.8s cubic-bezier(0.22, 1, 0.36, 1)', style({ opacity: 1, transform: 'translateY(0)' }))
      ])
    ]),
    trigger('fadeInLeft', [
      transition(':enter', [
        style({ opacity: 0, transform: 'translateX(-40px)' }),
        animate('0.8s cubic-bezier(0.22, 1, 0.36, 1)', style({ opacity: 1, transform: 'translateX(0)' }))
      ])
    ]),
    trigger('fadeInRight', [
      transition(':enter', [
        style({ opacity: 0, transform: 'translateX(40px)' }),
        animate('0.8s cubic-bezier(0.22, 1, 0.36, 1)', style({ opacity: 1, transform: 'translateX(0)' }))
      ])
    ]),
    trigger('fade', [
      transition(':enter', [
        style({ opacity: 0, transform: 'scale(0.98)' }),
        animate('0.5s cubic-bezier(0.22, 1, 0.36, 1)', style({ opacity: 1, transform: 'scale(1)' }))
      ])
    ]),
    trigger('scaleIn', [
      transition(':enter', [
        style({ opacity: 0, transform: 'scale(0.9)' }),
        animate('0.6s 0.1s cubic-bezier(0.22, 1, 0.36, 1)', style({ opacity: 1, transform: 'scale(1)' }))
      ])
    ]),
    trigger('staggerIn', [
      transition(':enter', [
        style({ opacity: 0, transform: 'translateY(30px)' }),
        animate('0.6s cubic-bezier(0.22, 1, 0.36, 1)', style({ opacity: 1, transform: 'translateY(0)' }))
      ])
    ])
  ]
})
export class LandingComponent implements OnInit, OnDestroy, AfterViewInit {

  @ViewChildren('revealEl') revealElements!: QueryList<ElementRef>;

  activeFeatureTab: 'doctor' | 'patient' = 'patient';
  isScrolled = false;
  mouseX = 0;
  mouseY = 0;
  currentYear = new Date().getFullYear();
  mobileMenuOpen = false;

  // Animated counters
  stats: Stat[] = [
    { value: '15K', label: 'Patients(es) suivis(es)', suffix: '+' },
    { value: '98', label: 'Précision IA', suffix: '%' },
    { value: '500', label: 'Médecins partenaires', suffix: '+' },
    { value: '24/7', label: 'Support dédié' }
  ];

  // Intersection observer
  private observer!: IntersectionObserver;

  @HostListener('window:scroll', [])
  onWindowScroll() {
    this.isScrolled = window.scrollY > 50;
  }

  @HostListener('window:mousemove', ['$event'])
  onMouseMove(event: MouseEvent) {
    this.mouseX = (event.clientX / window.innerWidth - 0.5) * 20;
    this.mouseY = (event.clientY / window.innerHeight - 0.5) * 20;
  }

  ngOnInit() {}

  ngAfterViewInit() {
    this.initScrollReveal();
  }

  ngOnDestroy() {
    if (this.observer) {
      this.observer.disconnect();
    }
  }

  private initScrollReveal() {
    this.observer = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          entry.target.classList.add('revealed');
          this.observer.unobserve(entry.target);
        }
      });
    }, { threshold: 0.1, rootMargin: '0px 0px -50px 0px' });

    // Observe all elements with reveal class
    setTimeout(() => {
      document.querySelectorAll('.reveal').forEach(el => {
        this.observer.observe(el);
      });
    }, 100);
  }

  faqItems: FaqItem[] = [
    {
      question: "L'IA remplace-t-elle le médecin ?",
      answer: "Non. Notre plateforme est un outil d'aide à la décision. Le médecin reste le seul responsable du diagnostic final et de la validation des résultats de l'IA.",
      isOpen: false
    },
    {
      question: "Quels examens sont supportés ?",
      answer: "Nous supportons actuellement les mammographies numérisées standards et travaillons sur l'intégration des IRM et échographies mammaires pour les prochaines versions.",
      isOpen: false
    },
    {
      question: "Mes données sont-elles en sécurité ?",
      answer: "Absolument. Nous utilisons un chiffrement de bout en bout AES-256, hébergement certifié HDS (Données de Santé) et conformité RGPD stricte.",
      isOpen: false
    },
    {
      question: "Comment relier patient et médecin ?",
      answer: "Le/la patient(e) peut générer un code de liaison unique ou accepter une invitation envoyée directement par son médecin via la plateforme.",
      isOpen: false
    },
    {
      question: "Est-ce adapté aux cliniques ?",
      answer: "Oui, nous proposons des offres 'Clinic' et 'Enterprise' permettant la gestion de multiples praticiens et le partage sécurisé des dossiers.",
      isOpen: false
    },
    {
      question: "Comment commencer gratuitement ?",
      answer: "Créez simplement un compte gratuitement. Si vous êtes médecin, une vérification professionnelle sera effectuée avant l'accès complet.",
      isOpen: false
    }
  ];

  testimonials: Testimonial[] = [
    {
      name: "Dr. Sophie Martin",
      role: "Radiologue — CHU Paris",
      content: "L'IA de BreastIQ me permet de prioriser les cas urgents et de sécuriser mes lectures avec un second avis instantané. Un game-changer.",
      rating: 5
    },
    {
      name: "Claire Lefèvre",
      role: "Patient(e)",
      content: "Avoir accès à mon suivi complet et pouvoir partager mes résultats facilement avec mon oncologue me rassure énormément au quotidien.",
      rating: 5
    },
    {
      name: "Dr. Marc Dupont",
      role: "Oncologue — Institut Curie",
      content: "La timeline centralisée des traitements et allergies facilite grandement la prise de décision lors des consultations multidisciplinaires.",
      rating: 5
    }
  ];

  toggleFeatureTab(tab: 'doctor' | 'patient') {
    this.activeFeatureTab = tab;
  }

  toggleFaq(index: number) {
    this.faqItems[index].isOpen = !this.faqItems[index].isOpen;
  }

  toggleMobileMenu() {
    this.mobileMenuOpen = !this.mobileMenuOpen;
  }

  scrollToSection(sectionId: string) {
    this.mobileMenuOpen = false;
    const element = document.getElementById(sectionId);
    if (element) {
      element.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  }

  getParallaxStyle(factor: number = 1) {
    return {
      transform: `translate(${this.mouseX * factor}px, ${this.mouseY * factor}px)`
    };
  }
}

