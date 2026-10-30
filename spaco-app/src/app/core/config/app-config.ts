/**
 * Runtime configuration, fetched from /config.json before bootstrap so one image serves
 * every environment (the container writes the file from its env vars at start).
 */
export interface AppConfig {
  /** spaco API base, e.g. /spaco-api */
  apiUrl: string;
  /** authco issuer, e.g. https://demo.example/authco */
  authUrl: string;
  clientId: string;
  /** RFC 8707 resource: spaco audience in authco tokens */
  resource: string;
  /** Spa slug sent as X-Tenant */
  tenant: string;
}

let current: AppConfig | null = null;

export async function loadAppConfig(): Promise<void> {
  const response = await fetch('/config.json', { cache: 'no-store' });
  if (!response.ok) {
    throw new Error(`config.json: HTTP ${response.status}`);
  }
  current = await response.json();
}

export function appConfig(): AppConfig {
  if (!current) {
    throw new Error('App config not loaded');
  }
  return current;
}

export const redirectUri = () => `${window.location.origin}/auth/callback`;
export const postLogoutRedirectUri = () => `${window.location.origin}/`;
