export type AppMode = 'BETTING' | 'QUIZ' | 'WORD_CLOUD';

export interface AppFeatureSettings {
  readonly activeMode: AppMode;
}
