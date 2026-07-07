import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';

import { AuthService } from './auth.service';

export const authTokenInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const header = auth.authorizationHeader();
  const shouldAttachToken = header !== null && request.url.startsWith('/api/') && request.url !== '/api/auth/login';

  if (!shouldAttachToken) {
    return next(request);
  }

  return next(
    request.clone({
      setHeaders: {
        Authorization: header
      }
    })
  );
};
