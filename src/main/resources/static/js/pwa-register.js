/*
 * Registro do Service Worker e captura do evento de instalação do PWA.
 * Incluído em todas as páginas para que o app fique instalável em qualquer ponto de entrada.
 */
(function () {
  if ("serviceWorker" in navigator) {
    window.addEventListener("load", () => {
      navigator.serviceWorker
        .register("/sw.js")
        .catch((err) => console.warn("Falha ao registrar o Service Worker:", err));
    });
  }

  // Guarda o evento de instalação para poder disparar um botão customizado
  // (ex: <button id="btn-instalar-app">) em qualquer página que tiver esse elemento.
  window.deferredPwaInstallPrompt = null;

  window.addEventListener("beforeinstallprompt", (event) => {
    event.preventDefault();
    window.deferredPwaInstallPrompt = event;

    const btnInstalar = document.getElementById("btn-instalar-app");
    if (btnInstalar) {
      btnInstalar.hidden = false;
      btnInstalar.addEventListener("click", async () => {
        if (!window.deferredPwaInstallPrompt) return;
        window.deferredPwaInstallPrompt.prompt();
        await window.deferredPwaInstallPrompt.userChoice;
        window.deferredPwaInstallPrompt = null;
        btnInstalar.hidden = true;
      });
    }
  });

  window.addEventListener("appinstalled", () => {
    window.deferredPwaInstallPrompt = null;
  });

  // Ao fazer logout, limpa o cache de páginas do Service Worker (stale-while-revalidate).
  // Sem isso, no mesmo aparelho, a próxima conta que logar poderia ver por um instante
  // a última tela em cache da conta anterior antes da rede atualizar.
  document.addEventListener("submit", (event) => {
    const form = event.target;
    if (form && form.action && form.action.indexOf("/logout") !== -1 && "caches" in window) {
      caches.keys().then((keys) => keys.forEach((key) => caches.delete(key)));
    }
  });
})();
