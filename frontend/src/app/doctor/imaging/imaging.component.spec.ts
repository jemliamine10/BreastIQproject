import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ImagingComponent } from './imaging.component';

describe('ImagingComponent', () => {
  let component: ImagingComponent;
  let fixture: ComponentFixture<ImagingComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ImagingComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ImagingComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
