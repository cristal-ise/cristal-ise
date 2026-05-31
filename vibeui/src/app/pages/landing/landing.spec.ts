import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { Configuration } from '../../api';
import { MessageService } from 'primeng/api';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TranslocoTestingModule } from '@jsverse/transloco';

import { Landing } from './landing';

describe('Landing', () => {
  let component: Landing;
  let fixture: ComponentFixture<Landing>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), Configuration, MessageService],
      imports: [
        Landing,
        TranslocoTestingModule.forRoot({
          langs: {},
          translocoConfig: {
            defaultLang: 'en',
            fallbackLang: 'en',
          },
        }),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Landing);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
