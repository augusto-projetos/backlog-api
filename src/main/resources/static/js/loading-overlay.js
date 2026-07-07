(function () {
  var OVERLAY_ID = "app-loading-overlay";
  var SLOW_HINT_DELAY_MS = 4000;
  var hintTimer = null;

  function injectStyles() {
	if (document.getElementById("app-loading-styles")) return;
	var style = document.createElement("style");
	style.id = "app-loading-styles";
	style.textContent =
	  "#" + OVERLAY_ID + "{position:fixed;inset:0;z-index:99999;display:none;" +
	  "align-items:center;justify-content:center;flex-direction:column;gap:16px;" +
	  "background:rgba(15,15,26,.88);}" +
	  "#" + OVERLAY_ID + ".is-visible{display:flex;}" +
	  "#" + OVERLAY_ID + " .app-spinner{width:42px;height:42px;border-radius:50%;" +
	  "border:4px solid rgba(255,255,255,.15);border-top-color:#ff2e63;" +
	  "animation:app-spin .8s linear infinite;}" +
	  "#" + OVERLAY_ID + " .app-loading-hint{color:#e6e8ee;" +
	  "font:14px -apple-system,'Segoe UI',Roboto,sans-serif;opacity:0;" +
	  "transition:opacity .4s ease;text-align:center;max-width:260px;padding:0 20px;}" +
	  "#" + OVERLAY_ID + " .app-loading-hint.is-shown{opacity:.85;}" +
	  "@keyframes app-spin{to{transform:rotate(360deg);}}";
	document.head.appendChild(style);
  }

  function getOverlay() {
	var existing = document.getElementById(OVERLAY_ID);
	if (existing) return existing;

	var overlay = document.createElement("div");
	overlay.id = OVERLAY_ID;
	overlay.innerHTML =
	  '<div class="app-spinner"></div>' +
	  '<div class="app-loading-hint">Conectando ao servidor...<br>pode demorar um pouco na primeira vez.</div>';
	document.body.appendChild(overlay);
	return overlay;
  }

  function showLoading() {
	injectStyles();
	var overlay = getOverlay();
	overlay.classList.add("is-visible");

	clearTimeout(hintTimer);
	var hint = overlay.querySelector(".app-loading-hint");
	hintTimer = setTimeout(function () {
	  hint.classList.add("is-shown");
	}, SLOW_HINT_DELAY_MS);
  }

  function hideLoading() {
	var overlay = document.getElementById(OVERLAY_ID);
	if (overlay) overlay.classList.remove("is-visible");
	clearTimeout(hintTimer);
  }

  // Se o usuário voltar via cache do navegador (bfcache), garante que não fique um
  // overlay "preso" na tela.
  window.addEventListener("pageshow", hideLoading);

  document.addEventListener("click", function (event) {
	var link = event.target.closest && event.target.closest("a[href]");
	if (!link) return;
	if (link.target === "_blank" || link.hasAttribute("download")) return;
	if (event.defaultPrevented || event.metaKey || event.ctrlKey || event.shiftKey || event.button !== 0) return;

	var url;
	try {
	  url = new URL(link.href, window.location.href);
	} catch (err) {
	  return;
	}
	if (url.origin !== window.location.origin) return;
	// Âncora dentro da própria página (ex: "#topo") não recarrega nada.
	if (url.pathname === window.location.pathname && url.hash) return;

	showLoading();
  });

  document.addEventListener("submit", function (event) {
	var form = event.target;
	if (!form || form.tagName !== "FORM" || form.target === "_blank") return;
	showLoading();
  });
})();
