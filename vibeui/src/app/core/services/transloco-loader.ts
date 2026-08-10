import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { TranslocoLoader, Translation } from '@jsverse/transloco';
import { environment } from '../../../environments/environment';
import { catchError, forkJoin, map, Observable, of } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class TranslocoHttpLoader implements TranslocoLoader {
  private http = inject(HttpClient);

  getTranslation(lang: string): Observable<Translation> {
    const local$ = this.http.get<Translation>(`${environment.i18n.localUrl}${lang}.json`).pipe(
      catchError(() => {
        console.error(`Could not load local translation for ${lang}`);
        return of({});
      })
    );

    if (environment.i18n.remoteUrl) {
      const remote$ = this.http.get<Translation>(`${environment.i18n.remoteUrl}${lang}.json`).pipe(
        catchError(() => {
          console.warn(`Could not load remote translation for ${lang}`);
          return of({});
        })
      );

      return forkJoin([local$, remote$]).pipe(
        map(([local, remote]) => ({ ...local, ...remote }))
      );
    }

    return local$;
  }
}
