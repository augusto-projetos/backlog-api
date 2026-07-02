(function () {
    'use strict';

    // --- Emojis por faixa de nível ---
    // A cada 5 níveis, muda o emoji exibido ao lado do texto "Nível X"
    const NIVEL_EMOJIS = [
        { minNivel: 1,  emoji: '🌱' }, // Nível 1–4: Iniciante
        { minNivel: 5,  emoji: '⚔️' }, // Nível 5–9: Guerreiro
        { minNivel: 10, emoji: '🔥' }, // Nível 10–14: Em chamas
        { minNivel: 15, emoji: '💎' }, // Nível 15–19: Diamante
        { minNivel: 20, emoji: '🏆' }, // Nível 20–24: Campeão
        { minNivel: 25, emoji: '⚡' }, // Nível 25–29: Relâmpago
        { minNivel: 30, emoji: '🌟' }, // Nível 30–34: Estrela
        { minNivel: 35, emoji: '🦾' }, // Nível 35–39: Máquina
        { minNivel: 40, emoji: '👑' }, // Nível 40–44: Rei
        { minNivel: 45, emoji: '🐉' }, // Nível 45–49: Dragão
        { minNivel: 50, emoji: '🌌' }, // Nível 50+: Lendário
    ];

    function getEmojiParaNivel(nivel) {
        let emoji = NIVEL_EMOJIS[0].emoji;
        for (const faixa of NIVEL_EMOJIS) {
            if (nivel >= faixa.minNivel) emoji = faixa.emoji;
            else break;
        }
        return emoji;
    }

    // --- Barra de XP no Header ---
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
            const emojiEl    = document.getElementById('xp-nivel-emoji');

            const emoji = getEmojiParaNivel(data.nivel);
            if (nivelEl)    nivelEl.textContent  = data.nivel;
            if (emojiEl)    emojiEl.textContent  = emoji;
            if (totalEl)    totalEl.textContent  = data.xpTotal + ' XP';
            if (fillEl) {
                setTimeout(() => { fillEl.style.width = data.progresso + '%'; }, 100);
            }

            container.style.display = 'block';
        } catch (err) {
            console.warn('[conquistas] Falha ao carregar XP:', err);
        }
    }

    // --- Toast ---

    let toastQueue = [];
    let toastAtivo = false;

    // Exibe uma sequência de toasts para cada conquista desbloqueada.
    function exibirToasts(conquistas) {
        if (!conquistas || conquistas.length === 0) return;
        // Aceita conquista única ou lista
        const lista = Array.isArray(conquistas) ? conquistas : [conquistas];
        toastQueue.push(...lista);
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
        if (!toast) {
            // Se não há toast no DOM (página que não é a home), ignora graciosamente
            if (typeof onClose === 'function') onClose();
            return;
        }

        document.getElementById('ct-icone').textContent = conquista.icone || '🏆';
        document.getElementById('ct-nome').textContent  = conquista.nome;
        document.getElementById('ct-desc').textContent  = conquista.descricao;
        document.getElementById('ct-xp').textContent    = '+' + conquista.xp + ' XP';

        toast.classList.remove('hide');
        toast.style.display = 'block';

        // Atualiza a barra de XP em background
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
        exibirToasts,
        getEmojiParaNivel,
    };

    // Auto-inicializa a barra de XP
    document.addEventListener('DOMContentLoaded', () => {
        if (document.getElementById('xp-bar-container')) {
            carregarXpHeader();
        }
    });
})();

// Verifica conquistas salvas no sessionStorage (vindas de outras páginas via redirect)
document.addEventListener('DOMContentLoaded', () => {
    const pendentes = sessionStorage.getItem('conquistas_pendentes');
    if (pendentes) {
        sessionStorage.removeItem('conquistas_pendentes');
        try {
            const lista = JSON.parse(pendentes);
            if (lista.length > 0) {
                setTimeout(() => window.Conquistas.exibirToasts(lista), 800);
            }
        } catch (e) { /* ignora */ }
    }
});
