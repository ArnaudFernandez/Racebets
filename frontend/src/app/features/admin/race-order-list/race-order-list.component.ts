import { ChangeDetectionStrategy, Component, input, output, signal } from '@angular/core';
import { TuiIcon } from '@taiga-ui/core';
import { TuiTiles } from '@taiga-ui/kit';

export interface RaceOrderItem {
  readonly entryId: number;
  readonly horseName: string;
  readonly horseNumber?: number;
  readonly betCount?: number;
  readonly voteCount?: number;
}

@Component({
  selector: 'app-race-order-list',
  imports: [TuiIcon, TuiTiles],
  templateUrl: './race-order-list.component.html',
  styleUrl: './race-order-list.component.less',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class RaceOrderListComponent {
  readonly entries = input.required<readonly RaceOrderItem[]>();
  readonly editable = input(false);
  readonly disabled = input(false);
  readonly reordered = output<readonly number[]>();
  readonly announcement = signal<string | null>(null);

  protected readonly order = signal(new Map<number, number>());

  protected reorder(order: Map<number, number>): void {
    if (this.disabled()) return;

    const reordered = this.entries()
      .map((entry, index) => ({entry, position: order.get(index) ?? index}))
      .sort((left, right) => left.position - right.position)
      .map(({entry}) => entry);

    this.emitOrder(reordered);
    this.order.set(new Map());
  }

  protected reorderWithKeyboard(event: KeyboardEvent, index: number): void {
    if (this.disabled() || (event.key !== 'ArrowUp' && event.key !== 'ArrowDown')) return;
    const target = index + (event.key === 'ArrowUp' ? -1 : 1);
    if (target < 0 || target >= this.entries().length) return;

    event.preventDefault();
    const reordered = [...this.entries()];
    [reordered[index], reordered[target]] = [reordered[target], reordered[index]];
    this.emitOrder(reordered, reordered[target]);
  }

  protected voteCount(entry: RaceOrderItem): number | null {
    return entry.betCount ?? entry.voteCount ?? null;
  }

  protected displayedPosition(index: number): number {
    return (this.order().get(index) ?? index) + 1;
  }

  private emitOrder(entries: readonly RaceOrderItem[], moved?: RaceOrderItem): void {
    this.reordered.emit(entries.map((entry) => entry.entryId));
    this.announcement.set(
      moved
        ? `${moved.horseName}, position ${entries.indexOf(moved) + 1}`
        : `Classement mis à jour. ${entries[0]?.horseName ?? 'Aucun cheval'} en première position.`
    );
  }
}
