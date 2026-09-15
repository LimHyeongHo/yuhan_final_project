import { createApiFetch } from './apiFetch';

describe('createApiFetch', () => {
  test('includes the session cookie on backend GET requests', async () => {
    const nativeFetch = jest.fn().mockResolvedValue({ ok: true });
    const apiFetch = createApiFetch(nativeFetch, 'http://localhost:3000');

    await apiFetch('http://localhost:8080/api/products/seller/orders');

    expect(nativeFetch).toHaveBeenCalledWith(
      'http://localhost:8080/api/products/seller/orders',
      { credentials: 'include' }
    );
  });

  test('adds one shared CSRF token to backend mutation requests', async () => {
    const nativeFetch = jest.fn()
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({ headerName: 'X-XSRF-TOKEN', token: 'csrf-token' }),
      })
      .mockResolvedValue({ ok: true });
    const apiFetch = createApiFetch(nativeFetch, 'http://localhost:3000');

    await apiFetch('http://localhost:8080/api/products/1', { method: 'DELETE' });
    await apiFetch('http://localhost:8080/api/products/2', { method: 'DELETE' });

    expect(nativeFetch).toHaveBeenNthCalledWith(1, 'http://localhost:8080/api/csrf', {
      credentials: 'include',
    });
    expect(nativeFetch).toHaveBeenCalledTimes(3);
    const firstMutationOptions = nativeFetch.mock.calls[1][1];
    expect(firstMutationOptions.credentials).toBe('include');
    expect(firstMutationOptions.headers.get('X-XSRF-TOKEN')).toBe('csrf-token');
  });

  test('does not modify requests to external services', async () => {
    const nativeFetch = jest.fn().mockResolvedValue({ ok: true });
    const apiFetch = createApiFetch(nativeFetch, 'http://localhost:3000');
    const options = { method: 'POST', body: '{}' };

    await apiFetch('https://example.com/api/payment', options);

    expect(nativeFetch).toHaveBeenCalledWith('https://example.com/api/payment', options);
  });
});
