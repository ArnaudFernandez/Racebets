import { Routes } from '@angular/router';

import { adminGuard } from './core/auth/admin.guard';
import { bettingFeatureGuard, quizFeatureGuard } from './core/features/app-feature.guard';

export const routes: Routes = [
  {
    path: '',
    canActivate: [bettingFeatureGuard],
    loadComponent: () =>
      import('./features/betting/live-betting-board/live-betting-board.component').then(
        (component) => component.LiveBettingBoardComponent
      )
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
