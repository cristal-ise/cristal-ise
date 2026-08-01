import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { CommonModule, JsonPipe } from '@angular/common';
import { PanelModule } from 'primeng/panel';
import { DividerModule } from 'primeng/divider';
import { OutcomeFieldSet } from './outcome-field-set/outcome-field-set';
import { DefaultService } from '../../api';
import { toSignal, toObservable } from '@angular/core/rxjs-interop';
import { switchMap, of, catchError, combineLatest, from } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { ApiErrorService } from '../../core/services/api-error.service';
import DynamicOutcomeViewConfig, {
  ComponentCatalogMap,
  OutcomeComponentType,
} from './dynamic-outcome-view-config';

@Component({
  selector: 'app-basic-outcome-view',
  imports: [CommonModule, JsonPipe, PanelModule, DividerModule, OutcomeFieldSet],
  templateUrl: './basic-outcome-view.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BasicOutcomeView {
  uuid = input.required<string>();
  eventId = input.required<string>();

  private itemService = inject(DefaultService);
  private apiErrorService = inject(ApiErrorService);

  private inputsCombined = combineLatest([toObservable(this.uuid), toObservable(this.eventId)]);

  outcome = toSignal(
    this.inputsCombined.pipe(
      switchMap(([uuid, eventId]) =>
        this.itemService.itemUuidHistoryEventIdDataGet({ uuid: uuid, eventId: eventId }).pipe(
          catchError((error: HttpErrorResponse) => {
            this.apiErrorService.handleError(error);
            return of(null);
          }),
        ),
      ),
    ),
  );

  schema = toSignal(
    toObservable(this.outcome).pipe(
      switchMap((outcomeJson) => {
        if (!outcomeJson) return of(null);
        return from(BasicOutcomeView.generateSchema(outcomeJson));
      }),
    ),
  );

  rootName = computed(() => {
    const outcomeJson = this.outcome() as any;
    if (!outcomeJson) return undefined;
    return Object.keys(outcomeJson)[0];
  });

  rootJson = computed(() => {
    const outcomeJson = this.outcome() as any;
    const rootName = this.rootName();
    if (!outcomeJson || !rootName) return undefined;
    return outcomeJson[rootName];
  });

  componentCatalog = computed<DynamicOutcomeViewConfig>(() => {
    return DynamicOutcomeViewConfig.createFromSchema(this.schema(), this.rootName());
  });

  isDisplayable = computed(() => {
    const mappings = this.componentCatalog().getMappings();
    return !Object.values(mappings).includes(OutcomeComponentType.UNIMPLEMENTED);
  });

  static async generateSchema(data: any) {
    const { quicktype, InputData, jsonInputForTargetLanguage } = await import('quicktype-core');
    const rootName = Object.keys(data)[0];
    const jsonString = JSON.stringify(data[rootName]);
    const jsonInput = jsonInputForTargetLanguage('schema');

    await jsonInput.addSource({name: rootName,samples: [jsonString]});

    const inputData = new InputData();
    inputData.addInput(jsonInput);

    const result = await quicktype({inputData, lang: 'schema'});
    return JSON.parse(result.lines.join('\n'));
  }
}
