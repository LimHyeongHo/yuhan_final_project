const SAFE_METHODS = new Set(['GET', 'HEAD', 'OPTIONS']);

const getRequestUrl = (input) => (
  typeof input === 'string' ? input : input?.url
);

const isBackendApiRequest = (input, pageOrigin) => {
  const requestUrl = getRequestUrl(input);
  if (!requestUrl) return false;

  try {
    const url = new URL(requestUrl, pageOrigin);
    return url.port === '8080' && url.pathname.startsWith('/api/');
  } catch {
    return false;
  }
};

const getRequestMethod = (input, options) => (
  options?.method || input?.method || 'GET'
).toUpperCase();

export const createApiFetch = (nativeFetch, pageOrigin) => {
  const csrfTokens = new Map();
  const pendingTokens = new Map();

  const loadCsrfToken = async (apiOrigin) => {
    if (csrfTokens.has(apiOrigin)) return csrfTokens.get(apiOrigin);
    if (pendingTokens.has(apiOrigin)) return pendingTokens.get(apiOrigin);

    const request = nativeFetch(`${apiOrigin}/api/csrf`, {
      credentials: 'include',
    })
      .then(async (response) => {
        if (!response.ok) {
          throw new Error('CSRF 토큰을 발급받지 못했습니다.');
        }
        const data = await response.json();
        if (!data?.token || !data?.headerName) {
          throw new Error('CSRF 토큰 응답이 올바르지 않습니다.');
        }
        const csrf = { token: data.token, headerName: data.headerName };
        csrfTokens.set(apiOrigin, csrf);
        return csrf;
      })
      .finally(() => pendingTokens.delete(apiOrigin));

    pendingTokens.set(apiOrigin, request);
    return request;
  };

  return async (input, options = {}) => {
    if (!isBackendApiRequest(input, pageOrigin)) {
      return nativeFetch(input, options);
    }

    const requestUrl = new URL(getRequestUrl(input), pageOrigin);
    const method = getRequestMethod(input, options);
    const requestOptions = {
      ...options,
      credentials: 'include',
    };

    if (!SAFE_METHODS.has(method)) {
      const { token, headerName } = await loadCsrfToken(requestUrl.origin);
      const isRequest = typeof Request !== 'undefined' && input instanceof Request;
      const headers = new Headers(isRequest ? input.headers : undefined);
      new Headers(options.headers).forEach((value, name) => headers.set(name, value));
      headers.set(headerName, token);
      requestOptions.headers = headers;
    }

    return nativeFetch(input, requestOptions);
  };
};

export const installApiFetch = () => {
  if (window.fetch.__yuBookApiFetch) return;

  const nativeFetch = window.fetch.bind(window);
  const apiFetch = createApiFetch(nativeFetch, window.location.origin);
  apiFetch.__yuBookApiFetch = true;
  window.fetch = apiFetch;
};
