import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

export interface Article {
  id: number;
  title: string;
  summary: string;
  content: string;
  imageUrl: string;
  category: string;
  readTime: string;
  date: string;
  externalUrl: string;
}

@Component({
  selector: 'app-education',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './education.component.html',
  styleUrl: './education.component.css'
})
export class EducationComponent {
  categories = ['Tous', 'Prévention', 'Dépistage', 'Traitements', 'Bien-être', 'Nutrition'];
  selectedCategory = 'Tous';
  searchQuery = '';

  allArticles: Article[] = [
    {
      id: 1,
      title: 'Prévention : Les gestes essentiels',
      summary: 'Adopter une hygiène de vie saine et pratiquer l\'auto-examen peut sauver des vies. Découvrez les fondamentaux.',
      content: '', 
      imageUrl: 'assets/articles/prevention.png',
      category: 'Prévention',
      readTime: '5 min',
      date: '12 Mars 2024',
      externalUrl: 'https://www.ligue-cancer.net/comprendre-la-maladie/les-cancers/cancer-du-sein'
    },
    {
      id: 2,
      title: 'Le Dépistage Organisé',
      summary: 'Dès 50 ans, la mammographie de dépistage est un rendez-vous gratuit et indispensable pour une détection précoce.',
      content: '',
      imageUrl: 'assets/articles/screening.png',
      category: 'Dépistage',
      readTime: '4 min',
      date: '10 Mars 2024',
      externalUrl: 'https://www.e-cancer.fr/Patients-et-proches/Les-cancers/Cancer-du-sein/Depistage'
    },
    {
      id: 3,
      title: 'Comprendre les Traitements',
      summary: 'Chirurgie, chimiothérapie, radiothérapie : un tour d\'horizon complet des protocoles médicaux modernes.',
      content: '',
      imageUrl: 'assets/articles/treatments.png',
      category: 'Traitements',
      readTime: '7 min',
      date: '08 Mars 2024',
      externalUrl: 'https://curie.fr/dossier-pedagogique/les-traitements-du-cancer-du-sein'
    },
    {
      id: 4,
      title: 'Soutien et Bien-être',
      summary: 'L\'aspect psychologique et le confort de vie sont fondamentaux dans le parcours de soin et la guérison.',
      content: '',
      imageUrl: 'assets/articles/wellbeing.png',
      category: 'Bien-être',
      readTime: '6 min',
      date: '05 Mars 2024',
      externalUrl: 'https://www.rose-up.fr/magazine/cancer-sein-bien-etre/'
    },
    {
      id: 5,
      title: 'Nutrition & Cancer',
      summary: 'L\'alimentation joue un rôle clé. Apprenez quels aliments privilégier pendant les traitements.',
      content: '',
      imageUrl: 'assets/articles/wellbeing.png', // Fallback
      category: 'Nutrition',
      readTime: '8 min',
      date: '02 Mars 2024',
      externalUrl: 'https://www.e-cancer.fr/Patients-et-proches/Bien-vivre-pendant-et-apres-un-cancer/Alimentation'
    },
    {
      id: 6,
      title: 'Activité Physique Adaptée',
      summary: 'Le sport est un allié précieux pour réduire la fatigue et les risques de récidive.',
      content: '',
      imageUrl: 'assets/articles/prevention.png',
      category: 'Bien-être',
      readTime: '6 min',
      date: '28 Fév 2024',
      externalUrl: 'https://www.camira.fr/sport-et-cancer/'
    }
  ];

  get filteredArticles() {
    return this.allArticles.filter(article => {
      const matchesCategory = this.selectedCategory === 'Tous' || article.category === this.selectedCategory;
      const matchesSearch = article.title.toLowerCase().includes(this.searchQuery.toLowerCase()) || 
                           article.summary.toLowerCase().includes(this.searchQuery.toLowerCase());
      return matchesCategory && matchesSearch;
    });
  }

  get recommendedArticles() {
    return this.allArticles.slice(0, 2);
  }

  get popularArticles() {
    return this.allArticles.slice(2, 5);
  }

  setCategory(category: string) {
    this.selectedCategory = category;
  }

  onSearch(event: any) {
    this.searchQuery = event.target.value;
  }

  openArticle(article: Article) {
    window.open(article.externalUrl, '_blank');
  }
}
