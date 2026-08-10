import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { SelectModule } from 'primeng/select';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';

import { LanguageSelector } from '../../layout/components/language-selector/language-selector';

@Component({
  selector: 'app-landing',
  imports: [CommonModule, ButtonModule, RouterLink, TranslocoPipe, LanguageSelector],
  templateUrl: './landing.html',
  styleUrl: './landing.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Landing {
  features = [
    { title: 'landing.performance_title', desc: 'landing.performance_desc', icon: 'pi-bolt', color: 'from-blue-500/20 to-indigo-500/20' },
    { title: 'landing.dx_title', desc: 'landing.dx_desc', icon: 'pi-code', color: 'from-purple-500/20 to-pink-500/20' },
    { title: 'landing.enterprise_title', desc: 'landing.enterprise_desc', icon: 'pi-shield', color: 'from-emerald-500/20 to-teal-500/20' }
  ];
}
