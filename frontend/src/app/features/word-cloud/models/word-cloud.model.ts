export type WordCloudQuestionStatus = 'DRAFT' | 'OPEN' | 'REVEALED' | 'CLOSED';

export interface WordCloudQuestionListItem {
  readonly id: number;
  readonly text: string;
  readonly status: WordCloudQuestionStatus;
  readonly submissionCount: number;
  readonly createdAt: string;
  readonly openedAt: string | null;
  readonly revealedAt: string | null;
  readonly closedAt: string | null;
}

export interface WordCloudWord {
  readonly text: string;
  readonly count: number;
}

export interface WordCloudAdminResponse {
  readonly text: string;
  readonly count: number;
  readonly censored: boolean;
}

export interface WordCloudAdminSnapshot {
  readonly id: number;
  readonly text: string;
  readonly status: WordCloudQuestionStatus;
  readonly createdAt: string;
  readonly openedAt: string | null;
  readonly revealedAt: string | null;
  readonly closedAt: string | null;
  readonly submissionCount: number;
  readonly responses: readonly WordCloudAdminResponse[];
}

export interface WordCloudSnapshot {
  readonly id: number;
  readonly text: string;
  readonly status: WordCloudQuestionStatus;
  readonly openedAt: string | null;
  readonly submissionCount: number;
  readonly currentUserResponse: string | null;
  readonly words: readonly WordCloudWord[];
}

export interface WordCloudQuestionRequest {
  readonly text: string;
}

export interface WordCloudResponseRequest {
  readonly text: string;
}
