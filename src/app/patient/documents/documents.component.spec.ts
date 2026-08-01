import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DocumentsComponent } from './documents.component';
import { AuthService } from '../../services/auth.service';
import { DocumentService } from '../../services/document.service';
import { WebSocketService } from '../../services/websocket.service';
import { PatientProfileService } from '../../services/patient-profile.service';
import { LinkService } from '../../services/link.service';
import { UserService } from '../../services/user.service';

describe('DocumentsComponent', () => {
  let component: DocumentsComponent;
  let fixture: ComponentFixture<DocumentsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DocumentsComponent],
      providers: [
        { provide: AuthService, useValue: { currentUser: null } },
        { provide: DocumentService, useValue: {} },
        { provide: WebSocketService, useValue: {} },
        { provide: PatientProfileService, useValue: {} },
        { provide: LinkService, useValue: {} },
        { provide: UserService, useValue: {} }
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DocumentsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
