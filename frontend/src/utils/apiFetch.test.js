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

  test('adds a CSRF token to deployed Render mutation requests', async () => {
    const nativeFetch = jest.fn()
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({ headerName: 'X-XSRF-TOKEN', token: 'deployed-token' }),
      })
      .mockResolvedValue({ ok: true });
    const apiFetch = createApiFetch(
      nativeFetch,
      'https://yuhanfinalproject.vercel.app',
      'https://yuhan-final-project-api.onrender.com'
    );

    await apiFetch('https://yuhan-final-project-api.onrender.com/api/payment/prepare', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: '{"productId":1}',
    });

    expect(nativeFetch).toHaveBeenNthCalledWith(
      1,
      'https://yuhan-final-project-api.onrender.com/api/csrf',
      { credentials: 'include' }
    );
    const mutationOptions = nativeFetch.mock.calls[1][1];
    expect(mutationOptions.credentials).toBe('include');
    expect(mutationOptions.headers.get('X-XSRF-TOKEN')).toBe('deployed-token');
    expect(mutationOptions.headers.get('Content-Type')).toBe('application/json');
  });

  test('does not modify requests to external services', async () => {
    const nativeFetch = jest.fn().mockResolvedValue({ ok: true });
    const apiFetch = createApiFetch(nativeFetch, 'http://localhost:3000');
    const options = { method: 'POST', body: '{}' };

    await apiFetch('https://example.com/api/payment', options);

    expect(nativeFetch).toHaveBeenCalledWith('https://example.com/api/payment', options);
  });
});
