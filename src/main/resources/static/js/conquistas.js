(function () {
    'use strict';

    // --- Barra de XP no Header ---

    // Busca dados de XP da API e atualiza a barra no header.
    async function carregarXpHeader() {
        const container = document.getElementById('xp-bar-container');
        if (!container) return;

        try {
            const res = await fetch('/api/conquistas/meu-perfil');
            if (!res.ok) return;

            const data = await res.json();

            const nivelEl    = document.getElementById('xp-nivel-num');
            const totalEl    = document.getElementById('xp-total-display');
            const fillEl     = document.getElementById('xp-bar-fill');

            if (nivelEl)    nivelEl.textContent   = data.nivel;
            if (totalEl)    totalEl.textContent   = data.xpTotal + ' XP';
            if (fillEl) {
                // Pequeno delay para a animação funcionar
                setTimeout(() => { fillEl.style.width = data.progresso + '%'; }, 100);
            }

            container.style.display = 'block';
        } catch (err) {
            // Falha silenciosa - a barra simplesmente não aparece
            console.warn('[conquistas] Falha ao carregar XP:', err);
        }
    }

    // --- Toast ---

    let toastQueue = [];
    let toastAtivo = false;

    // Exibe uma sequência de toasts para cada conquista desbloqueada.
    function exibirToasts(conquistas) {
        if (!conquistas || conquistas.length === 0) return;

        toastQueue.push(...conquistas);
        if (!toastAtivo) proximoToast();
    }

    function proximoToast() {
        if (toastQueue.length === 0) { toastAtivo = false; return; }
        toastAtivo = true;

        const c = toastQueue.shift();
        mostrarToast(c, () => {
            setTimeout(proximoToast, 400);
        });
    }

    function mostrarToast(conquista, onClose) {
        const toast = document.getElementById('conquista-toast');
        if (!toast) return;

        document.getElementById('ct-icone').textContent = conquista.icone || '🏆';
        document.getElementById('ct-nome').textContent  = conquista.nome;
        document.getElementById('ct-desc').textContent  = conquista.descricao;
        document.getElementById('ct-xp').textContent    = '+' + conquista.xp + ' XP';

        toast.classList.remove('hide');
        toast.style.display = 'block';

        // Também atualiza a barra de XP em background
        carregarXpHeader();

        const timer = setTimeout(() => {
            fecharToast(toast, onClose);
        }, 4500);

        // Clique fecha imediatamente
        toast.onclick = () => {
            clearTimeout(timer);
            fecharToast(toast, onClose);
        };
    }

    function fecharToast(toast, onClose) {
        toast.classList.add('hide');
        setTimeout(() => {
            toast.style.display = 'none';
            toast.classList.remove('hide');
            if (typeof onClose === 'function') onClose();
        }, 380);
    }

    // --- API Pública ---

    window.Conquistas = {
        carregarXpHeader,
        exibirToasts
    };

    // Auto-inicializa a barra de XP na home
    document.addEventListener('DOMContentLoaded', () => {
        if (document.getElementById('xp-bar-container')) {
            carregarXpHeader();
        }
    });
})();

// Verifica conquistas salvas no sessionStorage (vindas do cadastro.html)
document.addEventListener('DOMContentLoaded', () => {
    const pendentes = sessionStorage.getItem('conquistas_pendentes');
    if (pendentes) {
        sessionStorage.removeItem('conquistas_pendentes');
        try {
            const lista = JSON.parse(pendentes);
            if (lista.length > 0) {
                // Pequeno delay para a página carregar antes do toast
                setTimeout(() => window.Conquistas.exibirToasts(lista), 800);
            }
        } catch (e) { /* ignora */ }
    }
});
