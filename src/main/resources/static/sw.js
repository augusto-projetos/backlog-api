/*
 * Service Worker - Meu Backlog PWA
 *
 * Estratégia:
 * - App shell (CSS/JS/ícones/manifest): "cache first, fallback to network"
 * - Páginas HTML (navegação): "network first, fallback to cache" - toda página
 *   aqui é dinâmica/pessoal (home, timeline, dashboard...), então sempre tenta
 *   buscar a versão mais recente na rede primeiro; só usa o cache se estiver
 *   offline. Isso evita telas desatualizadas (ex: linha do tempo sem o
 *   registro mais recente até dar F5).
 * - Chamadas de API (/api/**) e autenticação: NUNCA são interceptadas - sempre vão direto à rede,
 *   para não servir dados desatualizados nem quebrar CSRF/sessão.
 */

const CACHE_VERSION = "v3";
const CACHE_NAME = `backlog-shell-${CACHE_VERSION}`;

const APP_SHELL = [
  "/",
  "/manifest.webmanifest",
  "/css/style.css",
  "/css/variaveis.css",
  "/css/index.css",
  "/img/logo.png",
  "/img/icons/icon-192x192.png",
  "/img/icons/icon-512x512.png",
];

self.addEventListener("install", (event) => {
  event.waitUntil(
    caches
      .open(CACHE_NAME)
      .then((cache) => cache.addAll(APP_SHELL))
      .then(() => self.skipWaiting())
  );
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches
      .keys()
      .then((keys) =>
        Promise.all(
          keys
            .filter((key) => key.startsWith("backlog-shell-") && key !== CACHE_NAME)
            .map((key) => caches.delete(key))
        )
      )
      .then(() => self.clients.claim())
  );
});

self.addEventListener("fetch", (event) => {
  const { request } = event;

  if (request.method !== "GET") return;

  const url = new URL(request.url);
  if (url.origin !== self.location.origin) return;

  // Nunca cachear API, autenticação ou o próprio service worker.
  if (
    url.pathname.startsWith("/api/") ||
    url.pathname.startsWith("/auth/") ||
    url.pathname.startsWith("/admin") ||
    url.pathname === "/sw.js"
  ) {
    return;
  }

  const isNavigation = request.mode === "navigate";

  if (isNavigation) {
    event.respondWith(networkFirst(request));
  } else if (isStaticAsset(url.pathname)) {
    event.respondWith(cacheFirst(request));
  }
});

function isStaticAsset(pathname) {
  return (
    pathname.startsWith("/css/") ||
    pathname.startsWith("/js/") ||
    pathname.startsWith("/img/") ||
    pathname === "/manifest.webmanifest"
  );
}

async function cacheFirst(request) {
  const cached = await caches.match(request);
  if (cached) return cached;

  try {
    const response = await fetch(request);
    const cache = await caches.open(CACHE_NAME);
    cache.put(request, response.clone());
    return response;
  } catch (err) {
    return cached || Response.error();
  }
}

async function networkFirst(request) {
  const cache = await caches.open(CACHE_NAME);

  try {
    const response = await fetch(request);
    if (response && response.ok) {
      cache.put(request, response.clone());
    }
    return response;
  } catch (err) {
    // Offline (ou rede falhou): cai pro que tiver em cache, senão pro shell "/"
    const cached = await cache.match(request);
    return cached || cache.match("/");
  }
}
