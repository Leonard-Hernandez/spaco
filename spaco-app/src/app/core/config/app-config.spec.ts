import { absoluteApiUrl, loadAppConfig } from './app-config';

async function withApiUrl(apiUrl: string) {
  spyOn(window, 'fetch').and.resolveTo(new Response(JSON.stringify({
    apiUrl, authUrl: 'https://auth.example.com', clientId: 'c', resource: 'r', tenant: 'miel-spa'
  })));
  await loadAppConfig();
}

describe('absoluteApiUrl', () => {
  it('resolves a relative API url against the origin', async () => {
    await withApiUrl('/spaco-api');
    expect(absoluteApiUrl()).toBe(`${window.location.origin}/spaco-api`);
  });

  it('keeps an absolute API url as is, without doubling the origin', async () => {
    await withApiUrl('https://spaco-staging.example.com/spaco-api');
    expect(absoluteApiUrl()).toBe('https://spaco-staging.example.com/spaco-api');
  });
});
