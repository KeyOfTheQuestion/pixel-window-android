const prefix = 'pixel-window-iphone:';
const version = prefix + '1';
const files = [
  './', './index.html', './style.css', './model.mjs', './app.mjs', './actors.mjs', './door.mjs',
  './manifest.webmanifest', './assets/day.jpg', './assets/night.jpg', './assets/readers.png',
  './assets/cat.png', './assets/sleep.png', './assets/icon-192.png', './assets/icon-512.png',
];

self.addEventListener('install', event => {
  event.waitUntil((async () => {
    const cache = await caches.open(version);
    await cache.addAll(files);
    await self.skipWaiting();
  })());
});

self.addEventListener('activate', event => {
  event.waitUntil((async () => {
    const keys = await caches.keys();
    await Promise.all(keys.filter(key => key.startsWith(prefix) && key !== version).map(key => caches.delete(key)));
    await self.clients.claim();
  })());
});

self.addEventListener('fetch', event => {
  const request = event.request;
  if (request.method !== 'GET' || !request.url.startsWith(self.registration.scope)) {
    return;
  }
  event.respondWith((async () => {
    const cache = await caches.open(version);
    if (request.mode === 'navigate') {
      try {
        const online = await fetch(request);
        if (online.ok) {
          return online;
        }
      } catch {
        // Serve the installed scene from the previous successful load.
      }
      return (await cache.match('./')) ?? Response.error();
    }
    return (await cache.match(request)) ?? fetch(request);
  })());
});
