import { TestBed } from '@angular/core/testing';
import { provideTaiga } from '@taiga-ui/core';

import { RaceOrderListComponent } from './race-order-list.component';

describe('RaceOrderListComponent', () => {
  it('emits the complete order produced by Taiga UI tiles', async () => {
    await TestBed.configureTestingModule({
      imports: [RaceOrderListComponent],
      providers: [provideTaiga()]
    }).compileComponents();

    const fixture = TestBed.createComponent(RaceOrderListComponent);
    fixture.componentRef.setInput('entries', [
      {entryId: 10, horseName: 'Ourasi'},
      {entryId: 11, horseName: 'Bellino II'}
    ]);
    fixture.componentRef.setInput('editable', true);
    const emitted: (readonly number[])[] = [];
    fixture.componentInstance.reordered.subscribe((order) => emitted.push(order));
    fixture.detectChanges();

    const actions = fixture.componentInstance as unknown as {
      order: {set(order: Map<number, number>): void};
      reorder(order: Map<number, number>): void;
    };
    const switchedOrder = new Map([[0, 1], [1, 0]]);
    actions.order.set(switchedOrder);
    fixture.detectChanges();

    const ranks = [...fixture.nativeElement.querySelectorAll('.rank')] as HTMLElement[];
    expect(ranks.map((rank) => rank.textContent?.trim())).toEqual(['2', '1']);
    expect(fixture.nativeElement.querySelector('.order-card[tuiTileHandle]')).not.toBeNull();

    actions.reorder(switchedOrder);

    expect(emitted).toEqual([[11, 10]]);
    expect(fixture.componentInstance.announcement()).toContain('Bellino II');
  });
});
