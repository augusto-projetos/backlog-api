// ═══════════════════════════════════════════════════════
//  PAINEL ADM — admin.js
// ═══════════════════════════════════════════════════════

document.addEventListener("DOMContentLoaded", () => {
    initTabs();
    initGraficos();
    initBuscaUsuarios();
    initAcoesUsuarios();
    initAcoesConquistas();
    initFechamentoModal();
});

// ─── ABAS ─────────────────────────────────────────────

function initTabs() {
    const tabs = document.querySelectorAll(".adm-tab");
    tabs.forEach(tab => {
        tab.addEventListener("click", () => {
            tabs.forEach(t => t.classList.remove("active"));
            document.querySelectorAll(".adm-section").forEach(s => s.classList.remove("active"));
            tab.classList.add("active");
            document.getElementById("tab-" + tab.dataset.tab).classList.add("active");
        });
    });
}

// ─── GRÁFICOS ─────────────────────────────────────────

function initGraficos() {
    const isDark = document.documentElement.getAttribute("data-theme") === "dark";
    const textColor = isDark ? "#e0e0e0" : "#2c3e50";
    const gridColor = isDark ? "#333" : "#eee";

    new Chart(document.getElementById("graficoTipo").getContext("2d"), {
        type: "doughnut",
        data: {
            labels: ["🎬 Filmes", "📺 Séries", "🎮 Jogos"],
            datasets: [{
                data: [STATS.filmes, STATS.series, STATS.jogos],
                backgroundColor: ["#e94560", "#10b981", "#7c3aed"],
                borderWidth: 0,
            }]
        },
        options: {
            responsive: true, maintainAspectRatio: false,
            plugins: { legend: { labels: { color: textColor, font: { family: "Poppins" } } } }
        }
    });

    new Chart(document.getElementById("graficoStatus").getContext("2d"), {
        type: "bar",
        data: {
            labels: ["✅ Concluídos", "▶️ Em Progresso", "📋 Backlog", "❌ Dropados"],
            datasets: [{
                data: [STATS.assistidos, STATS.assistindo, STATS.backlog, STATS.dropados],
                backgroundColor: ["#10b981", "#3b82f6", "#f59e0b", "#e94560"],
                borderRadius: 8, borderSkipped: false,
            }]
        },
        options: {
            responsive: true, maintainAspectRatio: false,
            plugins: { legend: { display: false } },
            scales: {
                x: { ticks: { color: textColor, font: { family: "Poppins" } }, grid: { display: false } },
                y: { ticks: { color: textColor, font: { family: "Poppins" } }, grid: { color: gridColor } }
            }
        }
    });

    if (STATS.notasLabels && STATS.notasLabels.length > 0) {
        new Chart(document.getElementById("graficoNotas").getContext("2d"), {
            type: "bar",
            data: {
                labels: Array.from(STATS.notasLabels),
                datasets: [{
                    label: "Quantidade",
                    data: Array.from(STATS.notasValues),
                    backgroundColor: "rgba(124, 58, 237, 0.7)",
                    borderRadius: 6, borderSkipped: false,
                }]
            },
            options: {
                responsive: true, maintainAspectRatio: false,
                plugins: { legend: { display: false } },
                scales: {
                    x: { ticks: { color: textColor, font: { family: "Poppins" } }, grid: { display: false } },
                    y: { ticks: { color: textColor, font: { family: "Poppins" } }, grid: { color: gridColor } }
                }
            }
        });
    }
}

// ─── BUSCA DE USUÁRIOS ────────────────────────────────

function initBuscaUsuarios() {
    const input = document.getElementById("busca-usuarios");
    if (!input) return;
    input.addEventListener("input", () => {
        const termo = input.value.toLowerCase();
        document.querySelectorAll("#tabela-usuarios tbody tr[data-id]").forEach(row => {
            row.style.display = row.textContent.toLowerCase().includes(termo) ? "" : "none";
        });
    });
}

// ─── AÇÕES DE USUÁRIOS ────────────────────────────────

function initAcoesUsuarios() {
    document.querySelectorAll(".btn-ver-itens").forEach(btn =>
        btn.addEventListener("click", () => abrirBacklogUsuario(btn.dataset.id, btn.dataset.nome)));

    document.querySelectorAll(".btn-editar-usuario").forEach(btn =>
        btn.addEventListener("click", () => editarUsuario(btn.dataset)));

    document.querySelectorAll(".btn-senha-usuario").forEach(btn =>
        btn.addEventListener("click", () => redefinirSenha(btn.dataset.id, btn.dataset.nome)));

    document.querySelectorAll(".btn-deletar-usuario").forEach(btn =>
        btn.addEventListener("click", () => deletarUsuario(btn.dataset.id, btn.dataset.nome)));
}

async function abrirBacklogUsuario(userId, nome) {
    const modal = document.getElementById("modal-itens");
    const titulo = document.getElementById("modal-itens-titulo");
    const corpo = document.getElementById("modal-itens-corpo");

    titulo.textContent = `Backlog de ${nome}`;
    corpo.innerHTML = `<p class="empty-modal">⏳ Carregando...</p>`;
    modal.classList.remove("hidden");

    try {
        const resp = await fetch(`/admin/api/usuario/${userId}/itens`);
        const itens = await resp.json();

        if (itens.length === 0) {
            corpo.innerHTML = `<p class="empty-modal">📋 Este usuário não tem itens no backlog.</p>`;
            return;
        }

        const tipoIcon = { "Filme": "🎬", "Série": "📺", "Jogo": "🎮" };

        corpo.innerHTML = `<div class="item-lista">
            ${itens.map(item => `
                <div class="item-row" id="item-row-${item.id}">
                    ${item.imagemUrl
                        ? `<img class="item-thumb" src="${escapeHtml(item.imagemUrl)}" alt="${escapeHtml(item.titulo)}" onerror="this.style.display='none'">`
                        : `<div class="item-thumb-placeholder">${tipoIcon[item.tipo] || "📦"}</div>`
                    }
                    <div class="item-info">
                        <div class="item-titulo">${escapeHtml(item.titulo)}</div>
                        <div class="item-meta">
                            <span class="tag tag-tipo">${tipoIcon[item.tipo] || ""} ${escapeHtml(item.tipo)}</span>
                            <span class="tag tag-status">${escapeHtml(item.status)}</span>
                            ${item.nota != null ? `<span class="tag tag-nota">⭐ ${item.nota}</span>` : ""}
                        </div>
                        ${item.resenha ? `<div style="font-size:0.78rem;color:var(--text-muted);margin-top:4px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;max-width:280px">"${escapeHtml(item.resenha)}"</div>` : ""}
                    </div>
                    <div class="item-actions">
                        <button class="item-action-btn" onclick="editarItemAdmin(${item.id}, '${escapeHtml(item.titulo)}', '${escapeHtml(item.status)}', ${item.nota ?? 0}, \`${escapeHtml(item.resenha || '')}\`)" title="Editar item">
                            <i class="fa fa-pen"></i>
                        </button>
                        <button class="item-action-btn btn-danger" onclick="deletarItemAdmin(${item.id})" title="Remover item">
                            <i class="fa fa-trash"></i>
                        </button>
                    </div>
                </div>
            `).join("")}
        </div>`;
    } catch (e) {
        corpo.innerHTML = `<p class="empty-modal">❌ Erro ao carregar itens.</p>`;
    }
}

// FIX: editar item do backlog de outro usuário
async function editarItemAdmin(itemId, titulo, statusAtual, notaAtual, resenhaAtual) {
    const isDark = isDarkMode();
    const statusOptions = ["Backlog", "Assistindo", "Jogando", "Assistido", "Zerado", "Dropado"];

    const { value: resultado } = await Swal.fire({
        title: `✏️ Editar Item`,
        html: `
            <div style="text-align:left;margin-bottom:6px">
                <strong style="font-size:0.95rem">${escapeHtml(titulo)}</strong>
            </div>
            <div style="display:flex;flex-direction:column;gap:14px;text-align:left">
                <div>
                    <label style="font-size:0.75rem;font-weight:700;color:#7f8c8d;text-transform:uppercase;letter-spacing:.5px">STATUS</label>
                    <select id="swal-status" class="swal2-select" style="margin-top:6px;width:100%">
                        ${statusOptions.map(s => `<option value="${s}" ${s === statusAtual ? "selected" : ""}>${s}</option>`).join("")}
                    </select>
                </div>
                <div>
                    <label style="font-size:0.75rem;font-weight:700;color:#7f8c8d;text-transform:uppercase;letter-spacing:.5px">NOTA (0–10)</label>
                    <input id="swal-nota" class="swal2-input" type="number" min="0" max="10" step="0.5" value="${notaAtual}" style="margin-top:6px">
                </div>
                <div>
                    <label style="font-size:0.75rem;font-weight:700;color:#7f8c8d;text-transform:uppercase;letter-spacing:.5px">RESENHA</label>
                    <textarea id="swal-resenha" class="swal2-textarea" style="margin-top:6px;min-height:80px">${escapeHtml(resenhaAtual)}</textarea>
                </div>
            </div>
        `,
        showCancelButton: true,
        confirmButtonText: "Salvar",
        cancelButtonText: "Cancelar",
        confirmButtonColor: "#7c3aed",
        width: "500px",
        background: isDark ? "#1e1e1e" : "#fff",
        color: isDark ? "#e0e0e0" : "#2c3e50",
        preConfirm: () => {
            const nota = parseFloat(document.getElementById("swal-nota").value);
            if (isNaN(nota) || nota < 0 || nota > 10) {
                Swal.showValidationMessage("A nota deve ser entre 0 e 10.");
                return false;
            }
            return {
                status:  document.getElementById("swal-status").value,
                nota,
                resenha: document.getElementById("swal-resenha").value.trim()
            };
        }
    });

    if (!resultado) return;

    const resp = await fetch(`/admin/item/${itemId}/editar`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(resultado)
    });
    const data = await resp.json();

    if (data.sucesso) {
        toastSucesso("Item atualizado!");
        // Atualiza as tags inline sem recarregar o modal
        const row = document.getElementById(`item-row-${itemId}`);
        if (row) {
            row.querySelector(".tag-status").textContent = resultado.status;
            const tagNota = row.querySelector(".tag-nota");
            if (tagNota) tagNota.textContent = `⭐ ${resultado.nota}`;
        }
    } else {
        toastErro(data.erro || "Erro ao editar item.");
    }
}

async function deletarItemAdmin(itemId) {
    const confirm = await Swal.fire({
        title: "Remover item?",
        text: "Este item será removido do backlog do usuário.",
        icon: "warning",
        showCancelButton: true,
        confirmButtonColor: "#e94560",
        cancelButtonColor: "#6b7280",
        confirmButtonText: "Sim, remover",
        cancelButtonText: "Cancelar",
        background: isDarkMode() ? "#1e1e1e" : "#fff",
        color: isDarkMode() ? "#e0e0e0" : "#2c3e50",
    });

    if (!confirm.isConfirmed) return;

    const resp = await fetch(`/admin/item/${itemId}`, { method: "DELETE" });
    const data = await resp.json();

    if (data.sucesso) {
        document.getElementById(`item-row-${itemId}`)?.remove();
        toastSucesso("Item removido!");
    } else {
        toastErro(data.erro || "Erro ao remover item.");
    }
}

function editarUsuario(dataset) {
    const isDark = isDarkMode();
    Swal.fire({
        title: "✏️ Editar Usuário",
        html: `
            <div style="display:flex;flex-direction:column;gap:14px;text-align:left">
                <div>
                    <label style="font-size:0.75rem;font-weight:700;color:#7f8c8d;text-transform:uppercase;letter-spacing:.5px">NOME</label>
                    <input id="swal-login" class="swal2-input" value="${escapeHtml(dataset.login)}" placeholder="Nome do usuário" style="margin-top:6px">
                </div>
                <div>
                    <label style="font-size:0.75rem;font-weight:700;color:#7f8c8d;text-transform:uppercase;letter-spacing:.5px">@ USUÁRIO</label>
                    <input id="swal-social" class="swal2-input" value="${escapeHtml(dataset.social)}" placeholder="@usuário" style="margin-top:6px">
                </div>
                <div>
                    <label style="font-size:0.75rem;font-weight:700;color:#7f8c8d;text-transform:uppercase;letter-spacing:.5px">E-MAIL</label>
                    <input id="swal-email" class="swal2-input" type="email" value="${escapeHtml(dataset.email)}" placeholder="email@exemplo.com" style="margin-top:6px">
                </div>
            </div>
        `,
        showCancelButton: true,
        confirmButtonText: "Salvar",
        cancelButtonText: "Cancelar",
        confirmButtonColor: "#7c3aed",
        background: isDark ? "#1e1e1e" : "#fff",
        color: isDark ? "#e0e0e0" : "#2c3e50",
        preConfirm: () => {
            const login = document.getElementById("swal-login").value.trim();
            const social = document.getElementById("swal-social").value.trim();
            const email = document.getElementById("swal-email").value.trim();
            if (!login || !social || !email) {
                Swal.showValidationMessage("Preencha todos os campos.");
                return false;
            }
            return { login, socialUsername: social, email };
        }
    }).then(async result => {
        if (!result.isConfirmed) return;
        const resp = await fetch(`/admin/usuario/${dataset.id}/editar`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(result.value)
        });
        const data = await resp.json();
        if (data.sucesso) { toastSucesso("Usuário atualizado!"); setTimeout(() => location.reload(), 1200); }
        else toastErro(data.erro || "Erro ao editar.");
    });
}

function redefinirSenha(userId, nome) {
    const isDark = isDarkMode();
    Swal.fire({
        title: `🔑 Nova senha para ${escapeHtml(nome)}`,
        html: `
            <div style="display:flex;flex-direction:column;gap:12px;text-align:left">
                <div>
                    <label style="font-size:0.75rem;font-weight:700;color:#7f8c8d;text-transform:uppercase;letter-spacing:.5px">NOVA SENHA</label>
                    <input id="swal-nova-senha" class="swal2-input" type="password" placeholder="Nova senha" style="margin-top:6px">
                </div>
                <div>
                    <label style="font-size:0.75rem;font-weight:700;color:#7f8c8d;text-transform:uppercase;letter-spacing:.5px">CONFIRMAR</label>
                    <input id="swal-confirma-senha" class="swal2-input" type="password" placeholder="Confirmar nova senha" style="margin-top:6px">
                </div>
                <p style="font-size:0.78rem;color:#7f8c8d;margin:0">A senha será redefinida sem confirmação do usuário.</p>
            </div>
        `,
        showCancelButton: true,
        confirmButtonText: "Redefinir",
        cancelButtonText: "Cancelar",
        confirmButtonColor: "#f59e0b",
        background: isDark ? "#1e1e1e" : "#fff",
        color: isDark ? "#e0e0e0" : "#2c3e50",
        preConfirm: () => {
            const nova = document.getElementById("swal-nova-senha").value;
            const confirma = document.getElementById("swal-confirma-senha").value;
            if (!nova || nova.length < 6) { Swal.showValidationMessage("Mínimo 6 caracteres."); return false; }
            if (nova !== confirma) { Swal.showValidationMessage("As senhas não coincidem."); return false; }
            return nova;
        }
    }).then(async result => {
        if (!result.isConfirmed) return;
        const resp = await fetch(`/admin/usuario/${userId}/senha`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ novaSenha: result.value })
        });
        const data = await resp.json();
        if (data.sucesso) toastSucesso("Senha redefinida!");
        else toastErro(data.erro || "Erro ao redefinir senha.");
    });
}

function deletarUsuario(userId, nome) {
    const isDark = isDarkMode();
    Swal.fire({
        title: "⚠️ Deletar usuário?",
        html: `Tem certeza que deseja deletar <strong>${escapeHtml(nome)}</strong>?<br><br>
               <span style="color:#e94560">Todos os dados, backlog e conquistas serão removidos permanentemente.</span>`,
        icon: "warning",
        showCancelButton: true,
        confirmButtonColor: "#e94560",
        cancelButtonColor: "#6b7280",
        confirmButtonText: "Sim, deletar",
        cancelButtonText: "Cancelar",
        background: isDark ? "#1e1e1e" : "#fff",
        color: isDark ? "#e0e0e0" : "#2c3e50",
    }).then(async result => {
        if (!result.isConfirmed) return;
        const resp = await fetch(`/admin/usuario/${userId}`, { method: "DELETE" });
        const data = await resp.json();
        if (data.sucesso) {
            toastSucesso("Usuário deletado!");
            document.querySelector(`tr[data-id="${userId}"]`)?.remove();
        } else toastErro(data.erro || "Erro ao deletar.");
    });
}

// ─── AÇÕES DE CONQUISTAS ──────────────────────────────

function initAcoesConquistas() {
    document.getElementById("btn-nova-conquista")?.addEventListener("click", () => abrirFormConquista(null));
    document.querySelectorAll(".btn-editar-conquista").forEach(btn =>
        btn.addEventListener("click", () => abrirFormConquista(btn.dataset)));
    document.querySelectorAll(".btn-deletar-conquista").forEach(btn =>
        btn.addEventListener("click", () => deletarConquista(btn.dataset.id, btn.dataset.nome)));
}

// FIX: modal de conquista com mais espaço e campos bem separados
function abrirFormConquista(dados) {
    const isEdicao = !!dados;
    const isDark = isDarkMode();

    Swal.fire({
        title: isEdicao ? "✏️ Editar Conquista" : "🏆 Nova Conquista",
        width: "520px",
        html: `
            <div style="display:flex;flex-direction:column;gap:18px;text-align:left;padding:4px 0">

                <div style="display:grid;grid-template-columns:90px 1fr;gap:14px">
                    <div>
                        <label style="display:block;font-size:0.75rem;font-weight:700;color:#7f8c8d;text-transform:uppercase;letter-spacing:.5px;margin-bottom:6px">ÍCONE</label>
                        <input id="sc-icone" class="swal2-input" value="${isEdicao ? escapeHtml(dados.icone) : "🏆"}"
                               placeholder="🏆" maxlength="5"
                               style="text-align:center;font-size:1.6rem;padding:8px;height:52px;margin:0;width:100%">
                    </div>
                    <div>
                        <label style="display:block;font-size:0.75rem;font-weight:700;color:#7f8c8d;text-transform:uppercase;letter-spacing:.5px;margin-bottom:6px">XP GANHO</label>
                        <input id="sc-xp" class="swal2-input" type="number" value="${isEdicao ? dados.xp : "50"}"
                               min="1" placeholder="50"
                               style="margin:0;width:100%;height:52px">
                    </div>
                </div>

                <div>
                    <label style="display:block;font-size:0.75rem;font-weight:700;color:#7f8c8d;text-transform:uppercase;letter-spacing:.5px;margin-bottom:6px">CHAVE ÚNICA</label>
                    <input id="sc-chave" class="swal2-input" value="${isEdicao ? escapeHtml(dados.chave) : ""}"
                           placeholder="Ex: PRIMEIRO_FILME" style="text-transform:uppercase;margin:0;width:100%">
                </div>

                <div>
                    <label style="display:block;font-size:0.75rem;font-weight:700;color:#7f8c8d;text-transform:uppercase;letter-spacing:.5px;margin-bottom:6px">NOME</label>
                    <input id="sc-nome" class="swal2-input" value="${isEdicao ? escapeHtml(dados.nome) : ""}"
                           placeholder="Nome da conquista" style="margin:0;width:100%">
                </div>

                <div>
                    <label style="display:block;font-size:0.75rem;font-weight:700;color:#7f8c8d;text-transform:uppercase;letter-spacing:.5px;margin-bottom:6px">DESCRIÇÃO</label>
                    <textarea id="sc-desc" class="swal2-textarea"
                              placeholder="Descrição exibida ao usuário"
                              style="margin:0;width:100%;min-height:80px;resize:vertical;box-sizing:border-box">${isEdicao ? escapeHtml(dados.descricao) : ""}</textarea>
                </div>

            </div>
        `,
        showCancelButton: true,
        confirmButtonText: isEdicao ? "Salvar Edição" : "Criar Conquista",
        cancelButtonText: "Cancelar",
        confirmButtonColor: "#7c3aed",
        background: isDark ? "#1e1e1e" : "#fff",
        color: isDark ? "#e0e0e0" : "#2c3e50",
        preConfirm: () => {
            const icone = document.getElementById("sc-icone").value.trim();
            const chave = document.getElementById("sc-chave").value.trim().toUpperCase();
            const nome  = document.getElementById("sc-nome").value.trim();
            const desc  = document.getElementById("sc-desc").value.trim();
            const xp    = parseInt(document.getElementById("sc-xp").value);
            if (!icone || !chave || !nome || !desc || isNaN(xp) || xp < 1) {
                Swal.showValidationMessage("Preencha todos os campos corretamente.");
                return false;
            }
            return { icone, chave, nome, descricao: desc, xp };
        }
    }).then(async result => {
        if (!result.isConfirmed) return;
        const url = isEdicao ? `/admin/conquista/${dados.id}/editar` : `/admin/conquista/criar`;
        const resp = await fetch(url, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(result.value)
        });
        const data = await resp.json();
        if (data.sucesso) {
            toastSucesso(isEdicao ? "Conquista atualizada!" : "Conquista criada!");
            setTimeout(() => location.reload(), 1200);
        } else toastErro(data.erro || "Erro ao salvar conquista.");
    });
}

function deletarConquista(id, nome) {
    const isDark = isDarkMode();
    Swal.fire({
        title: "Deletar conquista?",
        html: `A conquista <strong>${escapeHtml(nome)}</strong> será removida de todos os usuários que a possuem.`,
        icon: "warning",
        showCancelButton: true,
        confirmButtonColor: "#e94560",
        cancelButtonColor: "#6b7280",
        confirmButtonText: "Deletar",
        cancelButtonText: "Cancelar",
        background: isDark ? "#1e1e1e" : "#fff",
        color: isDark ? "#e0e0e0" : "#2c3e50",
    }).then(async result => {
        if (!result.isConfirmed) return;
        const resp = await fetch(`/admin/conquista/${id}`, { method: "DELETE" });
        const data = await resp.json();
        if (data.sucesso) { toastSucesso("Conquista deletada!"); setTimeout(() => location.reload(), 1200); }
        else toastErro(data.erro || "Erro ao deletar conquista.");
    });
}

// ─── MODAL FECHAR ─────────────────────────────────────

function initFechamentoModal() {
    document.querySelectorAll(".modal-close").forEach(btn => {
        btn.addEventListener("click", () =>
            document.getElementById(btn.dataset.modal)?.classList.add("hidden"));
    });
    document.querySelectorAll(".adm-modal").forEach(modal => {
        modal.addEventListener("click", e => {
            if (e.target === modal) modal.classList.add("hidden");
        });
    });
}

// ─── UTILITÁRIOS ─────────────────────────────────────

function isDarkMode() {
    return document.documentElement.getAttribute("data-theme") === "dark";
}

function toastSucesso(msg) {
    Swal.fire({ toast: true, position: "bottom-end", icon: "success", title: msg, showConfirmButton: false, timer: 2500, timerProgressBar: true });
}

function toastErro(msg) {
    Swal.fire({ toast: true, position: "bottom-end", icon: "error", title: msg, showConfirmButton: false, timer: 3000 });
}

function escapeHtml(text) {
    if (text == null) return "";
    return String(text)
        .replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;").replace(/'/g, "&#39;").replace(/`/g, "&#96;");
}
