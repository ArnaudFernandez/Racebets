import { WordCloudWord } from './models/word-cloud.model';

export interface WordCloudWordPresentation extends WordCloudWord {
  readonly color: string;
  readonly fontSize: string;
  readonly rotation: string;
}

const COLORS = [
  'var(--rb-brand-deep)',
  'var(--rb-brand)',
  'var(--rb-ink)',
  'var(--rb-muted)',
  'color-mix(in srgb, var(--rb-coral) 68%, var(--rb-ink))',
  'color-mix(in srgb, var(--rb-amber) 62%, var(--rb-ink))'
] as const;
const ROTATIONS = [-4, -2, 0, 2, 4, 1] as const;

export function stableWordHash(text: string): number {
  let hash = 2166136261;
  for (const character of text) {
    hash ^= character.codePointAt(0) ?? 0;
    hash = Math.imul(hash, 16777619);
  }
  return hash >>> 0;
}

export function presentWord(word: WordCloudWord, maxCount: number): WordCloudWordPresentation {
  const safeMax = Math.max(1, maxCount);
  const ratio = Math.max(0, Math.min(1, word.count / safeMax));
  const hash = stableWordHash(word.text);

  return {
    ...word,
    color: COLORS[hash % COLORS.length],
    fontSize: `clamp(1rem, ${(1 + ratio * 2.8).toFixed(2)}rem, 3.8rem)`,
    rotation: `rotate(${ROTATIONS[hash % ROTATIONS.length]}deg)`
  };
}
