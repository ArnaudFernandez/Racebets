import { Routes } from '@angular/router';

import { adminGuard } from './core/auth/admin.guard';
import { authenticatedGuard } from './core/auth/authenticated.guard';
import { bettingFeatureGuard, quizFeatureGuard } from './core/features/app-feature.guard';

export const routes: Routes = [
  {
    path: '',
    canActivate: [bettingFeatureGuard, authenticatedGuard],
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
    canActivate: [authenticatedGuard],
    loadComponent: () =>
      import('./features/betting/bet-history-page/bet-history-page.component').then(
        (component) => component.BetHistoryPageComponent
      )
  },
  {
    path: 'admin/races/:raceId',
    canActivate: [adminGuard],
    loadComponent: () =>
      import('./features/admin/race-control-page/race-control-page.component').then((component) => component.RaceControlPageComponent)
  },
  {
    path: 'admin',
    canActivate: [adminGuard],
    loadComponent: () =>
      import('./features/admin/admin-dashboard/admin-dashboard.component').then((component) => component.AdminDashboardComponent)
  },
  {
    path: 'quiz',
    canActivate: [quizFeatureGuard],
    loadComponent: () => import('./features/quiz/quiz-play-page/quiz-play-page.component').then((component) => component.QuizPlayPageComponent)
  },
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login-page/login-page.component').then((component) => component.LoginPageComponent)
  }
];
