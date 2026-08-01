import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CalendrierDocteurComponent } from './calendrierdocteur.component';

describe('CalendrierDocteurComponent', () => {
  let component: CalendrierDocteurComponent;
  let fixture: ComponentFixture<CalendrierDocteurComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ CalendrierDocteurComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CalendrierDocteurComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
