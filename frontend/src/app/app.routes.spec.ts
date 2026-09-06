import { routes } from './app.routes';

describe('application routes', () => {
  it('declares the public word cloud outside the authenticated route tree', () => {
    const publicRouteIndex = routes.findIndex((route) => route.path === 'publicWordCloud');
    const authenticatedTreeIndex = routes.findIndex((route) => route.path === '');
    const publicRoute = routes[publicRouteIndex];

    expect(publicRouteIndex).toBeGreaterThanOrEqual(0);
    expect(publicRouteIndex).toBeLessThan(authenticatedTreeIndex);
    expect(publicRoute.canActivate).toBeUndefined();
    expect(publicRoute.canActivateChild).toBeUndefined();
  });
});
