export type AppBrandingTheme = 'DEFAULT' | 'OLIFAN_GROUP';

export interface AppBrandingSettings {
  readonly appName: string;
  readonly imageUrl: string;
  readonly loginTitle: string;
  readonly loginSubtitle: string;
  readonly passwordlessLoginEnabled: boolean;
  readonly theme: AppBrandingTheme;
}
