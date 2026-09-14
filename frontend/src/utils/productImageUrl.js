export const getDisplayProductImageUrl = (imageUrl) => {
  if (!imageUrl) return null;

  try {
    const url = new URL(imageUrl);

    if (url.hostname === 'image.aladin.co.kr') {
      url.pathname = url.pathname.replace('/coversum/', '/cover500/');
    } else if (url.hostname.endsWith('.kakaocdn.net')) {
      url.pathname = url.pathname.replace(
        /\/thumb\/R\d+x\d+(?:\.q\d+)?\//,
        '/thumb/R500x0.q85/'
      );
    } else if (url.hostname === 'books.google.com' && url.searchParams.get('zoom') === '1') {
      url.searchParams.set('zoom', '3');
    }

    return url.toString();
  } catch {
    return imageUrl;
  }
};
