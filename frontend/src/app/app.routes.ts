import { Routes } from '@angular/router';

import { adminGuard } from './core/auth/admin.guard';
import { authenticatedGuard } from './core/auth/authenticated.guard';
import {
  bettingFeatureGuard,
  quizFeatureGuard,
  wordCloudFeatureGuard
} from './core/features/app-feature.guard';
import { tutorialGuard } from './core/tutorial/tutorial.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/login-page/login-page.component').then(
        (component) => component.LoginPageComponent
      )
  },
  {
    path: 'publicWordCloud',
    loadComponent: () =>
      import('./features/word-cloud/public-word-cloud-page/public-word-cloud-page.component').then(
        (component) => component.PublicWordCloudPageComponent
      )
  },
  {
    path: '',
    canActivateChild: [authenticatedGuard, tutorialGuard],
    children: [
      {
        path: '',
        pathMatch: 'full',
        canActivate: [bettingFeatureGuard],
        loadComponent: () =>
          import('./features/betting/live-betting-board/live-betting-board.component').then(
            (component) => component.LiveBettingBoardComponent
          )
      },
      {
        path: 'admin/history/:raceId',
        canActivate: [adminGuard],
        loadComponent: () =>
          import('./features/admin/race-history-detail-page/race-history-detail-page.component').then(
            (component) => component.RaceHistoryDetailPageComponent
          )
      },
      {
        path: 'history',
        canActivate: [bettingFeatureGuard],
        loadComponent: () =>
          import('./features/betting/bet-history-page/bet-history-page.component').then(
            (component) => component.BetHistoryPageComponent
          )
      },
      {
        path: 'tutorial',
        loadComponent: () =>
          import('./features/tutorial/tutorial-page/tutorial-page.component').then(
            (component) => component.TutorialPageComponent
          )
      },
      {
        path: 'admin/races/:raceId',
        canActivate: [adminGuard],
        loadComponent: () =>
          import('./features/admin/race-control-page/race-control-page.component').then(
            (component) => component.RaceControlPageComponent
          )
      },
      {
        path: 'admin/quizzes/new',
        canActivate: [adminGuard],
        loadComponent: () =>
          import('./features/quiz/quiz-editor-page/quiz-editor-page.component').then(
            (component) => component.QuizEditorPageComponent
          )
      },
      {
        path: 'admin/quizzes/:quizSetId/edit',
        canActivate: [adminGuard],
        loadComponent: () =>
          import('./features/quiz/quiz-editor-page/quiz-editor-page.component').then(
            (component) => component.QuizEditorPageComponent
          )
      },
      {
        path: 'admin/quizzes/sessions/:sessionId',
        canActivate: [adminGuard],
        loadComponent: () =>
          import('./features/quiz/quiz-control-page/quiz-control-page.component').then(
            (component) => component.QuizControlPageComponent
          )
      },
      {
        path: 'admin/word-cloud/questions/:questionId',
        canActivate: [adminGuard],
        loadComponent: () =>
          import('./features/word-cloud/admin-word-cloud-control-page/admin-word-cloud-control-page.component').then(
            (component) => component.AdminWordCloudControlPageComponent
          )
      },
      {
        path: 'admin',
        canActivate: [adminGuard],
        loadComponent: () =>
          import('./features/admin/admin-dashboard/admin-dashboard.component').then(
            (component) => component.AdminDashboardComponent
          )
      },
      {
        path: 'quiz',
        canActivate: [quizFeatureGuard],
        loadComponent: () =>
          import('./features/quiz/quiz-play-page/quiz-play-page.component').then(
            (component) => component.QuizPlayPageComponent
          )
      },
      {
        path: 'word-cloud',
        canActivate: [wordCloudFeatureGuard],
        loadComponent: () =>
          import('./features/word-cloud/word-cloud-page/word-cloud-page.component').then(
            (component) => component.WordCloudPageComponent
          )
      },
      {
        path: '**',
        redirectTo: ''
      }
    ]
  }
];
