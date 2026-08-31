import { presentWord, stableWordHash } from './word-cloud-presentation';

describe('word cloud presentation', () => {
  it('produces stable visual properties for the same word', () => {
    const first = presentWord({ text: 'Ourasi', count: 3 }, 6);
    const second = presentWord({ text: 'Ourasi', count: 3 }, 6);

    expect(stableWordHash('Ourasi')).toBe(stableWordHash('Ourasi'));
    expect(second).toEqual(first);
  });

  it('bounds font size and scales it by occurrence', () => {
    const smallest = presentWord({ text: 'Trot', count: 0 }, 10);
    const largest = presentWord({ text: 'Galop', count: 10 }, 10);

    expect(smallest.fontSize).toBe('clamp(1rem, 1.00rem, 3.8rem)');
    expect(largest.fontSize).toBe('clamp(1rem, 3.80rem, 3.8rem)');
  });
});
