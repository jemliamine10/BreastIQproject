import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DocumentsComponent } from './documents.component';
import { AuthService } from '../../services/auth.service';
import { DocumentService } from '../../services/document.service';
import { UserService } from '../../services/user.service';
import { LinkService } from '../../services/link.service';
import { WebSocketService } from '../../services/websocket.service';

describe('DocumentsComponent', () => {
  let component: DocumentsComponent;
  let fixture: ComponentFixture<DocumentsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DocumentsComponent],
      providers: [
        { provide: AuthService, useValue: { currentUser: null } },
        { provide: DocumentService, useValue: {} },
        { provide: UserService, useValue: {} },
        { provide: LinkService, useValue: {} },
        { provide: WebSocketService, useValue: {} }
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
