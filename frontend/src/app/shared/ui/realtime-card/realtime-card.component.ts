import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { TuiTitle } from '@taiga-ui/core';
import { TuiCard, TuiHeader } from '@taiga-ui/layout';

@Component({
  selector: 'app-realtime-card',
  imports: [TuiCard, TuiHeader, TuiTitle],
  templateUrl: './realtime-card.component.html',
  styleUrl: './realtime-card.component.less',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class RealtimeCardComponent {
  readonly title = input.required<string>();
  readonly subtitle = input<string>('');
}
