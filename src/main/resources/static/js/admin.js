document.addEventListener("DOMContentLoaded", () => {
    initTabs();
    initGraficos();
    initBuscaUsuarios();
    initAcoesUsuarios();
    initAcoesConquistas();
    initFechamentoModal();
    initAuditoria();
    initSistema();
});

// Função auxiliar para coletar os tokens CSRF das metatags
function getCsrfHeaders() {
    const tokenMeta = document.querySelector('meta[name="_csrf"]');
    const headerMeta = document.querySelector('meta[name="_csrf_header"]');
    if (!tokenMeta || !headerMeta) {
        return { 'Content-Type': 'application/json' };
    }
    return {
        'Content-Type': 'application/json',
        [headerMeta.getAttribute('content')]: tokenMeta.getAttribute('content')
    };
}

// --- ABAS ---
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

// --- GRÁFICOS COM FILTROS ---

// Instâncias dos gráficos (mantidas para destroy/rebuild)
const chartInstances = { tipo: null, status: null, notas: null, tempo: null };

// Estado atual de filtros de cada gráfico
const chartFilters = {
    tipo:   { tipos: ["FILME","SERIE","JOGO"], usuario: "" },
    status: { status: ["CONCLUIDO","ASSISTINDO","BACKLOG","DROPADO"], tipos: ["FILME","SERIE","JOGO"], usuario: "" },
    notas:  { tipos: ["FILME","SERIE","JOGO"], status: ["CONCLUIDO","ASSISTINDO","BACKLOG","DROPADO"], usuario: "", de: 0, ate: 10 },
    tempo:  { usuario: "" }
};

function isDarkMode() {
    return document.documentElement.getAttribute("data-theme") === "dark"
        || document.body.classList.contains("dark");
}

function getChartTheme() {
    const isDark = isDarkMode();
    return {
        textColor: isDark ? "#e0e0e0" : "#2c3e50",
        gridColor: isDark ? "#2a2a3a" : "#eee"
    };
}

// --- Construção dos gráficos ---

function buildGraficoTipo(data) {
    const { textColor } = getChartTheme();
    const allLabels = ["🎬 Filmes","📺 Séries","🎮 Jogos"];
    const allColors = ["#e94560","#10b981","#7c3aed"];
    const allKeys   = ["FILME","SERIE","JOGO"];

    const tipos = chartFilters.tipo.tipos;
    const labels = [], values = [], colors = [];
    allKeys.forEach((k, i) => {
        if (tipos.includes(k)) {
            let val = 0;
            if (data) {
                // backend retorna: filmes, series, jogos
                const key = k === "FILME" ? "filmes" : k === "SERIE" ? "series" : "jogos";
                val = data[key] ?? 0;
            } else {
                val = k === "FILME" ? STATS.filmes : k === "SERIE" ? STATS.series : STATS.jogos;
            }
            labels.push(allLabels[i]);
            values.push(val);
            colors.push(allColors[i]);
        }
    });

    const canvas = document.getElementById("graficoTipo");
    const isEmpty = values.every(v => v === 0);

    // Destroi instância anterior antes de alterar visibilidade do container
    if (chartInstances.tipo) { chartInstances.tipo.destroy(); chartInstances.tipo = null; }

    setChartEmptyState(canvas, isEmpty);
    if (isEmpty) return;

    chartInstances.tipo = new Chart(canvas.getContext("2d"), {
        type: "doughnut",
        data: { labels, datasets: [{ data: values, backgroundColor: colors, borderWidth: 0 }] },
        options: {
            responsive: true, maintainAspectRatio: false,
            plugins: { legend: { labels: { color: textColor, font: { family: "Poppins" } } } }
        }
    });
    // Força redimensionamento para corrigir dimensões após o container ter sido ocultado
    requestAnimationFrame(() => chartInstances.tipo && chartInstances.tipo.resize());
}

function buildGraficoStatus(data) {
    const { textColor, gridColor } = getChartTheme();
    const allLabels  = ["✅ Concluídos","▶️ Em Progresso","📋 Backlog","❌ Dropados"];
    const allColors  = ["#10b981","#3b82f6","#f59e0b","#e94560"];
    const allKeys    = ["CONCLUIDO","ASSISTINDO","BACKLOG","DROPADO"];
    const backendKeys = ["assistidos","assistindo","backlog","dropados"];
    const statsKeys  = ["assistidos","assistindo","backlog","dropados"];

    const statusAtivos = chartFilters.status.status;
    const labels = [], values = [], colors = [];
    allKeys.forEach((k, i) => {
        if (statusAtivos.includes(k)) {
            const val = data ? (data[backendKeys[i]] ?? 0) : (STATS[statsKeys[i]] ?? 0);
            labels.push(allLabels[i]);
            values.push(val);
            colors.push(allColors[i]);
        }
    });

    const canvas = document.getElementById("graficoStatus");
    const isEmpty = values.every(v => v === 0);

    // Destroi instância anterior antes de alterar visibilidade do container
    if (chartInstances.status) { chartInstances.status.destroy(); chartInstances.status = null; }

    setChartEmptyState(canvas, isEmpty);
    if (isEmpty) return;

    chartInstances.status = new Chart(canvas.getContext("2d"), {
        type: "bar",
        data: { labels, datasets: [{ data: values, backgroundColor: colors, borderRadius: 8, borderSkipped: false }] },
        options: {
            responsive: true, maintainAspectRatio: false,
            plugins: { legend: { display: false } },
            scales: {
                x: { ticks: { color: textColor, font: { family: "Poppins" } }, grid: { display: false } },
                y: {
                    ticks: { color: textColor, font: { family: "Poppins" }, precision: 0, stepSize: 1 },
                    grid: { color: gridColor },
                    beginAtZero: true
                }
            }
        }
    });
    // Força redimensionamento para corrigir dimensões após o container ter sido ocultado
    requestAnimationFrame(() => chartInstances.status && chartInstances.status.resize());
}

function buildGraficoNotas(data) {
    const { textColor, gridColor } = getChartTheme();
    const labels = data ? Object.keys(data) : Array.from(STATS.notasLabels);
    const values = data ? Object.values(data) : Array.from(STATS.notasValues);

    const canvas = document.getElementById("graficoNotas");
    const isEmpty = !labels || labels.length === 0;

    // Destroi instância anterior antes de alterar visibilidade do container
    if (chartInstances.notas) { chartInstances.notas.destroy(); chartInstances.notas = null; }

    setChartEmptyState(canvas, isEmpty);
    if (isEmpty) return;

    chartInstances.notas = new Chart(canvas.getContext("2d"), {
        type: "bar",
        data: {
            labels,
            datasets: [{
                label: "Quantidade",
                data: values,
                backgroundColor: "rgba(124,58,237,0.7)",
                borderRadius: 6, borderSkipped: false
            }]
        },
        options: {
            responsive: true, maintainAspectRatio: false,
            plugins: { legend: { display: false } },
            scales: {
                x: { ticks: { color: textColor, font: { family: "Poppins" } }, grid: { display: false } },
                y: {
                    ticks: { color: textColor, font: { family: "Poppins" }, precision: 0, stepSize: 1 },
                    grid: { color: gridColor },
                    beginAtZero: true
                }
            }
        }
    });
    // Força redimensionamento para corrigir dimensões após o container ter sido ocultado
    requestAnimationFrame(() => chartInstances.notas && chartInstances.notas.resize());
}

// Formata minutos exatamente como pedido:
//  - menos de 60min  -> "30 min"
//  - múltiplo de 60  -> "2h"
//  - resto           -> "1h 30min"
function formatarTempoAdmin(minutosTotais) {
    const minutos = Math.round(minutosTotais || 0);
    if (minutos < 60) return `${minutos} min`;

    const horas = Math.floor(minutos / 60);
    const restoMin = minutos % 60;
    return restoMin === 0 ? `${horas}h` : `${horas}h ${restoMin}min`;
}

function buildGraficoTempo(data) {
    const { textColor, gridColor } = getChartTheme();

    const minutosFilmes = data ? (data.minutosFilmes ?? 0) : (STATS.minutosFilmes ?? 0);
    const minutosJogos  = data ? (data.minutosJogos  ?? 0) : (STATS.minutosJogos  ?? 0);

    const elFilmes = document.getElementById("admTempoFilmesValor");
    const elJogos  = document.getElementById("admTempoJogosValor");
    if (elFilmes) elFilmes.textContent = formatarTempoAdmin(minutosFilmes);
    if (elJogos)  elJogos.textContent  = formatarTempoAdmin(minutosJogos);

    const canvas = document.getElementById("graficoTempo");
    const isEmpty = (minutosFilmes + minutosJogos) === 0;

    if (chartInstances.tempo) { chartInstances.tempo.destroy(); chartInstances.tempo = null; }

    setChartEmptyState(canvas, isEmpty);
    if (isEmpty) return;

    const ctx = canvas.getContext("2d");
    const gradFilmes = ctx.createLinearGradient(0, 0, 400, 0);
    gradFilmes.addColorStop(0, "#e94560");
    gradFilmes.addColorStop(1, "#ff6b9d");

    const gradJogos = ctx.createLinearGradient(0, 0, 400, 0);
    gradJogos.addColorStop(0, "#08d9d6");
    gradJogos.addColorStop(1, "#0f766e");

    chartInstances.tempo = new Chart(ctx, {
        type: "bar",
        data: {
            labels: ["🎬 Filmes", "🎮 Jogos"],
            datasets: [{
                label: "Tempo",
                // A barra usa horas (fração) só pra ficar visualmente proporcional;
                // o texto exibido (tooltip/cards) sempre usa formatarTempoAdmin()
                data: [minutosFilmes / 60, minutosJogos / 60],
                backgroundColor: [gradFilmes, gradJogos],
                borderRadius: 10, borderSkipped: false, barThickness: 42
            }]
        },
        options: {
            indexAxis: "y",
            responsive: true, maintainAspectRatio: false,
            plugins: {
                legend: { display: false },
                tooltip: {
                    callbacks: {
                        label: (c) => {
                            const minutosOriginais = c.datasetIndex === 0 ? minutosFilmes : minutosJogos;
                            return formatarTempoAdmin(minutosOriginais) + " investidos";
                        }
                    }
                }
            },
            scales: {
                x: {
                    beginAtZero: true,
                    title: { display: true, text: "Horas", color: textColor, font: { family: "Poppins", size: 12, weight: "bold" } },
                    ticks: { color: textColor, font: { family: "Poppins" } },
                    grid: { color: gridColor }
                },
                y: { ticks: { color: textColor, font: { family: "Poppins", size: 13 } }, grid: { display: false } }
            }
        }
    });
    requestAnimationFrame(() => chartInstances.tempo && chartInstances.tempo.resize());
}

// Exibe ou oculta o estado vazio no canvas
function setChartEmptyState(canvas, isEmpty) {
    const card = canvas.closest(".chart-card");
    let emptyEl = card.querySelector(".chart-empty-state");
    const containerCanvas = canvas.parentElement;

    if (isEmpty) {
        // Oculta o container do canvas para ceder espaço ao empty state
        containerCanvas.style.visibility = "hidden";
        containerCanvas.style.position = "absolute";

        if (!emptyEl) {
            emptyEl = document.createElement("div");
            emptyEl.className = "chart-empty-state";
            emptyEl.innerHTML = `
                <i class="fa-solid fa-chart-pie"></i>
                <p>Sem dados para os filtros selecionados</p>
            `;
            card.appendChild(emptyEl);
        }
        emptyEl.style.display = "flex";
    } else {
        // Restaura o container do canvas antes do Chart.js renderizar
        containerCanvas.style.visibility = "";
        containerCanvas.style.position = "";
        if (emptyEl) {
            emptyEl.style.display = "none";
        }
    }
}

// --- Busca filtrada no backend ---

async function fetchChartData(chartName) {
    const f = chartFilters[chartName];
    const params = new URLSearchParams();

    if (chartName === "tipo") {
        f.tipos.forEach(t => params.append("tipo", t));
        if (f.usuario) params.set("usuarioId", f.usuario);
    }
    if (chartName === "status") {
        f.status.forEach(s => params.append("status", s));
        f.tipos.forEach(t => params.append("tipo", t));
        if (f.usuario) params.set("usuarioId", f.usuario);
    }
    if (chartName === "notas") {
        f.tipos.forEach(t => params.append("tipo", t));
        f.status.forEach(s => params.append("status", s));
        if (f.usuario) params.set("usuarioId", f.usuario);
        params.set("de", f.de);
        params.set("ate", f.ate);
    }
    if (chartName === "tempo") {
        if (f.usuario) params.set("usuarioId", f.usuario);
    }

    try {
        const res = await fetch(`/admin/api/stats/${chartName}?${params.toString()}`);
        if (!res.ok) return null;
        return await res.json();
    } catch (e) {
        console.warn("Erro ao buscar dados filtrados:", e);
        return null;
    }
}

// --- Renderização das tags de filtros ativos ---

const LABEL_MAP = {
    FILME: "🎬 Filmes", SERIE: "📺 Séries", JOGO: "🎮 Jogos",
    CONCLUIDO: "✅ Concluídos", ASSISTINDO: "▶️ Em Progresso",
    BACKLOG: "📋 Backlog", DROPADO: "❌ Dropados"
};

function renderFilterTags(chartName) {
    const container = document.getElementById(`filter-tags-${chartName}`);
    if (!container) return;

    const f = chartFilters[chartName];
    const tags = [];

    if (chartName === "tipo" || chartName === "status" || chartName === "notas") {
        const allTipos = ["FILME","SERIE","JOGO"];
        if (f.tipos && f.tipos.length < allTipos.length) {
            f.tipos.forEach(t => tags.push({ label: LABEL_MAP[t], key: "tipos", val: t, chart: chartName }));
        }
    }
    if (chartName === "status" || chartName === "notas") {
        const allStatus = ["CONCLUIDO","ASSISTINDO","BACKLOG","DROPADO"];
        if (f.status && f.status.length < allStatus.length) {
            f.status.forEach(s => tags.push({ label: LABEL_MAP[s], key: "status", val: s, chart: chartName }));
        }
    }
    if (f.usuario) {
        const sel = document.querySelector(`[name="${chartName}-usuario"]`);
        const label = sel ? (sel.selectedOptions[0]?.text ?? "Usuário") : "Usuário";
        tags.push({ label: `👤 ${label}`, key: "usuario", val: "", chart: chartName });
    }
    if (chartName === "notas" && (f.de > 0 || f.ate < 10)) {
        tags.push({ label: `⭐ ${f.de}–${f.ate}`, key: "range", val: "", chart: chartName });
    }

    container.innerHTML = tags.map(tag =>
        `<span class="filter-tag">${tag.label}</span>`
    ).join("");
}

// --- Aplicar filtros ---

async function applyFilters(chartName) {
    const popover = document.getElementById(`filter-popover-${chartName}`);

    if (chartName === "tipo") {
        chartFilters.tipo.tipos   = [...popover.querySelectorAll('[name="tipo-tipo"]:checked')].map(c => c.value);
        chartFilters.tipo.usuario = popover.querySelector('[name="tipo-usuario"]').value;
    }
    if (chartName === "status") {
        chartFilters.status.status  = [...popover.querySelectorAll('[name="status-status"]:checked')].map(c => c.value);
        chartFilters.status.tipos   = [...popover.querySelectorAll('[name="status-tipo"]:checked')].map(c => c.value);
        chartFilters.status.usuario = popover.querySelector('[name="status-usuario"]').value;
    }
    if (chartName === "notas") {
        chartFilters.notas.tipos   = [...popover.querySelectorAll('[name="notas-tipo"]:checked')].map(c => c.value);
        chartFilters.notas.status  = [...popover.querySelectorAll('[name="notas-status"]:checked')].map(c => c.value);
        chartFilters.notas.usuario = popover.querySelector('[name="notas-usuario"]').value;
        chartFilters.notas.de      = parseFloat(popover.querySelector('[name="notas-de"]').value) || 0;
        chartFilters.notas.ate     = parseFloat(popover.querySelector('[name="notas-ate"]').value) || 10;
    }
    if (chartName === "tempo") {
        chartFilters.tempo.usuario = popover.querySelector('[name="tempo-usuario"]').value;
    }

    popover.classList.add("hidden");
    // Esconde o backdrop ao aplicar o filtro (mobile)
    const backdrop = document.querySelector(".filter-backdrop");
    if (backdrop) backdrop.style.display = "none";
    renderFilterTags(chartName);

    const data = await fetchChartData(chartName);
    if (chartName === "tipo")   buildGraficoTipo(data);
    if (chartName === "status") buildGraficoStatus(data);
    if (chartName === "notas")  buildGraficoNotas(data);
    if (chartName === "tempo")  buildGraficoTempo(data);
}

// --- Limpar filtros ---

function clearFilters(chartName) {
    const popover = document.getElementById(`filter-popover-${chartName}`);

    // Marca todos os checkboxes
    popover.querySelectorAll("input[type=checkbox]").forEach(cb => cb.checked = true);
    // Reset selects
    popover.querySelectorAll("select").forEach(s => s.value = "");
    // Reset ranges
    const de = popover.querySelector('[name="notas-de"]'); if (de) de.value = 0;
    const ate = popover.querySelector('[name="notas-ate"]'); if (ate) ate.value = 10;

    // Reset estado
    if (chartName === "tipo")   chartFilters.tipo   = { tipos: ["FILME","SERIE","JOGO"], usuario: "" };
    if (chartName === "status") chartFilters.status = { status: ["CONCLUIDO","ASSISTINDO","BACKLOG","DROPADO"], tipos: ["FILME","SERIE","JOGO"], usuario: "" };
    if (chartName === "notas")  chartFilters.notas  = { tipos: ["FILME","SERIE","JOGO"], status: ["CONCLUIDO","ASSISTINDO","BACKLOG","DROPADO"], usuario: "", de: 0, ate: 10 };
    if (chartName === "tempo")  chartFilters.tempo  = { usuario: "" };

    document.getElementById(`filter-tags-${chartName}`).innerHTML = "";

    // Fecha o popover e esconde o backdrop após limpar
    if (popover) popover.classList.add("hidden");
    const backdrop = document.querySelector(".filter-backdrop");
    if (backdrop) backdrop.style.display = "none";

    if (chartName === "tipo")   buildGraficoTipo(null);
    if (chartName === "status") buildGraficoStatus(null);
    if (chartName === "notas")  buildGraficoNotas(null);
    if (chartName === "tempo")  buildGraficoTempo(null);
}

// --- Init ---

function initGraficos() {
    // Constrói com dados padrão (sem filtro)
    buildGraficoTipo(null);
    buildGraficoStatus(null);
    buildGraficoNotas(null);
    buildGraficoTempo(null);
    initFilterControls();
}

function initFilterControls() {
    // Cria o backdrop uma única vez
    const backdrop = document.createElement("div");
    backdrop.className = "filter-backdrop";
    document.body.appendChild(backdrop);

    function closeAllPopovers() {
        document.querySelectorAll(".filter-popover").forEach(p => p.classList.add("hidden"));
        backdrop.style.display = "none";
    }

    // Botões de abrir/fechar popover
    document.querySelectorAll(".btn-chart-filter").forEach(btn => {
        btn.addEventListener("click", (e) => {
            e.stopPropagation();
            const chart = btn.dataset.chart;
            const popover = document.getElementById(`filter-popover-${chart}`);
            const isHidden = popover.classList.contains("hidden");

            // Fecha todos os outros
            closeAllPopovers();

            if (isHidden) {
                popover.classList.remove("hidden");
                // Mostra backdrop apenas no mobile
                if (window.innerWidth <= 900) {
                    backdrop.style.display = "block";
                }
            }
        });
    });

    // Fechar ao clicar no backdrop (mobile)
    backdrop.addEventListener("click", closeAllPopovers);

    // Botão aplicar
    document.querySelectorAll(".filter-apply-btn").forEach(btn => {
        btn.addEventListener("click", () => applyFilters(btn.dataset.chart));
    });

    // Botão limpar
    document.querySelectorAll(".filter-clear-btn").forEach(btn => {
        btn.addEventListener("click", () => clearFilters(btn.dataset.chart));
    });

    // Fechar popover ao clicar fora (desktop)
    document.addEventListener("click", (e) => {
        if (!e.target.closest(".chart-filter-wrap")) {
            closeAllPopovers();
        }
    });
}

// --- BUSCA DE USUÁRIOS ---
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

// --- AÇÕES DE USUÁRIOS ---
function initAcoesUsuarios() {
    // Usando delegação de eventos para garantir o mapeamento estável pós-renderização do Thymeleaf
    document.addEventListener("click", (e) => {
        const btnVer       = e.target.closest(".btn-ver-itens");
        const btnEdit      = e.target.closest(".btn-editar-usuario");
        const btnSenha     = e.target.closest(".btn-senha-usuario");
        const btnDel       = e.target.closest(".btn-deletar-usuario");
        const btnConquista = e.target.closest(".btn-conceder-conquista");

        if (btnVer)       abrirBacklogUsuario(btnVer.dataset.id, btnVer.dataset.nome);
        if (btnEdit)      editarUsuario(btnEdit.dataset);
        if (btnSenha)     redefinirSenha(btnSenha.dataset.id, btnSenha.dataset.nome);
        if (btnDel)       deletarUsuario(btnDel.dataset.id, btnDel.dataset.nome);
        if (btnConquista) abrirConcederConquista(btnConquista.dataset.id, btnConquista.dataset.nome);
    });
}

// ---------------------------------------------------------------
// Modal de gerenciamento de conquistas do usuário
// Mostra: conquistas desbloqueadas (com botão de revogar) +
//         conquistas bloqueadas (com botão de conceder)
// ---------------------------------------------------------------
async function abrirConcederConquista(userId, nomeUsuario) {
    const isDark = isDarkMode();
    await renderModalConquistas(userId, nomeUsuario, isDark);
}

async function renderModalConquistas(userId, nomeUsuario, isDark) {
    // Busca em paralelo: todas as conquistas do sistema + as do usuário
    let todasConquistas = [], conquistasDoUsuario = [];
    try {
        [todasConquistas, conquistasDoUsuario] = await Promise.all([
            fetch("/admin/api/conquistas").then(r => r.json()),
            fetch(`/admin/api/usuario/${userId}/conquistas`).then(r => r.json())
        ]);
    } catch (e) {
        toastErro("Erro ao carregar conquistas.");
        return;
    }

    // IDs das conquistas que o usuário já possui
    const idsDoUsuario = new Set(conquistasDoUsuario.map(c => c.id));

    const fmtData = iso => {
        if (!iso) return "";
        const d = new Date(iso);
        return d.toLocaleDateString("pt-BR") + " às " +
               d.toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" });
    };

    // --- Estilos embutidos no HTML do modal ---
    const styles = `
        <style>
            .gc-section-title {
                font-size: 0.7rem; font-weight: 700; text-transform: uppercase;
                letter-spacing: .6px; color: #7f8c8d; text-align: left;
                margin: 16px 0 8px;
            }
            .gc-section-title:first-child { margin-top: 4px; }
            .gc-list { max-height: 210px; overflow-y: auto; padding-right: 2px; }
            .gc-row {
                display: flex; align-items: center; gap: 10px;
                padding: 9px 12px; border-radius: 9px; margin-bottom: 6px;
                text-align: left; transition: opacity .2s;
            }
            .gc-row.desbloqueada {
                background: rgba(124,58,237,0.08);
                border: 1px solid rgba(124,58,237,0.2);
            }
            .gc-row.bloqueada {
                background: rgba(255,255,255,0.03);
                border: 1px solid rgba(255,255,255,0.07);
                opacity: .75;
            }
            .gc-icone { font-size: 1.35rem; flex-shrink: 0; line-height: 1; }
            .gc-row.bloqueada .gc-icone { filter: grayscale(80%); opacity: .6; }
            .gc-info { flex: 1; min-width: 0; }
            .gc-info strong {
                display: block; font-size: 0.84rem; font-weight: 600;
                white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
            }
            .gc-info small { font-size: 0.72rem; color: #7f8c8d; }
            .gc-btn {
                flex-shrink: 0; font-size: 0.75rem; font-weight: 700;
                padding: 5px 12px; border-radius: 7px; cursor: pointer;
                border: 1px solid; display: flex; align-items: center;
                gap: 5px; white-space: nowrap; transition: all .18s;
            }
            .gc-btn:disabled { opacity: .35; pointer-events: none; }
            .gc-btn-revogar {
                background: transparent; color: #e94560;
                border-color: rgba(233,69,96,0.35);
            }
            .gc-btn-revogar:hover { background: #e94560; color: #fff; }
            .gc-btn-conceder {
                background: rgba(124,58,237,0.12); color: #a78bfa;
                border-color: rgba(124,58,237,0.35);
            }
            .gc-btn-conceder:hover { background: #7c3aed; color: #fff; }
            .gc-empty { font-size: 0.82rem; color: #7f8c8d; text-align: left; padding: 6px 2px; }
        </style>
    `;

    // --- Seção: desbloqueadas ---
    const desbloqueadasHtml = conquistasDoUsuario.length
        ? conquistasDoUsuario.map(c => `
            <div class="gc-row desbloqueada" id="gc-row-${c.id}">
                <span class="gc-icone">${escapeHtml(c.icone)}</span>
                <div class="gc-info">
                    <strong>${escapeHtml(c.nome)}</strong>
                    <small>+${c.xp} XP · ${fmtData(c.desbloqueadaEm)}</small>
                </div>
                <button class="gc-btn gc-btn-revogar"
                        onclick="gcRevogar(${userId}, ${c.id}, '${escapeHtml(c.nome)}', ${c.xp}, '${escapeHtml(c.icone)}', this)"
                        title="Revogar conquista (deduz ${c.xp} XP)">
                    <i class="fa fa-times"></i> Revogar
                </button>
            </div>`).join("")
        : `<p class="gc-empty">Nenhuma conquista ainda.</p>`;

    // --- Seção: bloqueadas ---
    const bloqueadasHtml = todasConquistas.filter(c => !idsDoUsuario.has(c.id)).length
        ? todasConquistas.filter(c => !idsDoUsuario.has(c.id)).map(c => `
            <div class="gc-row bloqueada" id="gc-row-${c.id}">
                <span class="gc-icone">${escapeHtml(c.icone)}</span>
                <div class="gc-info">
                    <strong>${escapeHtml(c.nome)}</strong>
                    <small>+${c.xp} XP · ${escapeHtml(c.descricao || "")}</small>
                </div>
                <button class="gc-btn gc-btn-conceder"
                        onclick="gcConceder(${userId}, ${c.id}, '${escapeHtml(c.nome)}', ${c.xp}, '${escapeHtml(c.icone)}', this)"
                        title="Conceder conquista (+${c.xp} XP)">
                    <i class="fa fa-plus"></i> Dar
                </button>
            </div>`).join("")
        : `<p class="gc-empty">Usuário já possui todas as conquistas! 🏆</p>`;

    const totalDesbloqueadas = conquistasDoUsuario.length;
    const totalConquistas    = todasConquistas.length;

    Swal.fire({
        title: "🏆 Conquistas do Usuário",
        width: "540px",
        html: `
            ${styles}
            <p style="font-size:0.84rem;color:var(--text-muted,#aaa);margin-bottom:2px;text-align:left">
                <strong style="color:inherit">${escapeHtml(nomeUsuario)}</strong>
                &nbsp;·&nbsp;
                <span id="gc-contador" style="color:#a78bfa;font-weight:600">
                    ${totalDesbloqueadas} / ${totalConquistas} desbloqueadas
                </span>
            </p>

            <p class="gc-section-title" id="gc-title-des">
                ✅ Desbloqueadas (<span id="gc-count-des">${totalDesbloqueadas}</span>)
            </p>
            <div class="gc-list" id="gc-list-des">${desbloqueadasHtml}</div>

            <p class="gc-section-title" id="gc-title-blo">
                🔒 Bloqueadas (<span id="gc-count-blo">${totalConquistas - totalDesbloqueadas}</span>)
            </p>
            <div class="gc-list" id="gc-list-blo">${bloqueadasHtml}</div>
        `,
        showConfirmButton: false,
        showCancelButton: true,
        cancelButtonText: "Fechar",
        background: isDark ? "#1e1e1e" : "#fff",
        color: isDark ? "#e0e0e0" : "#2c3e50",
    });
}

// Chamado pelo botão "Revogar" dentro do modal
async function gcRevogar(userId, conquistaId, nome, xp, icone, btn) {
    const isDark = isDarkMode();
    const confirm = await Swal.fire({
        title: "Revogar conquista?",
        html: `Deseja remover <strong>${escapeHtml(nome)}</strong> deste usuário?<br>
               <small style="color:#e94560">−${xp} XP serão descontados do total.</small>`,
        icon: "warning",
        showCancelButton: true,
        confirmButtonText: "Sim, revogar",
        cancelButtonText: "Cancelar",
        confirmButtonColor: "#e94560",
        background: isDark ? "#1e1e1e" : "#fff",
        color: isDark ? "#e0e0e0" : "#2c3e50",
    });
    if (!confirm.isConfirmed) return;

    btn.disabled = true;
    try {
        const resp = await fetch(`/admin/usuario/${userId}/revogar-conquista/${conquistaId}`, {
            method: "DELETE",
            headers: getCsrfHeaders()
        });
        const data = await resp.json();
        if (data.sucesso) {
            // Move a linha da seção "desbloqueadas" para "bloqueadas" sem fechar o modal
            const row = document.getElementById(`gc-row-${conquistaId}`);
            if (row) row.remove();

            // Remove mensagem de vazio se existia
            const listDes = document.getElementById("gc-list-des");
            const listBlo = document.getElementById("gc-list-blo");

            // Atualiza contadores
            const cntDes = document.getElementById("gc-count-des");
            const cntBlo = document.getElementById("gc-count-blo");
            const ctGeral = document.getElementById("gc-contador");
            const novasDes = (parseInt(cntDes?.textContent) || 1) - 1;
            const novasBlo = (parseInt(cntBlo?.textContent) || 0) + 1;
            if (cntDes) cntDes.textContent = novasDes;
            if (cntBlo) cntBlo.textContent = novasBlo;
            if (ctGeral) {
                const total = novasDes + novasBlo;
                ctGeral.textContent = `${novasDes} / ${total} desbloqueadas`;
            }

            // Se ficou vazio, mostra placeholder
            if (listDes && !listDes.querySelector(".gc-row")) {
                listDes.innerHTML = `<p class="gc-empty">Nenhuma conquista ainda.</p>`;
            }

            // Insere o card na seção bloqueadas
            if (listBlo) {
                listBlo.querySelector(".gc-empty")?.remove();
                listBlo.insertAdjacentHTML("afterbegin", `
                    <div class="gc-row bloqueada" id="gc-row-${conquistaId}">
                        <span class="gc-icone" style="filter:grayscale(80%);opacity:.6">${escapeHtml(icone)}</span>
                        <div class="gc-info">
                            <strong>${escapeHtml(nome)}</strong>
                            <small>+${xp} XP</small>
                        </div>
                        <button class="gc-btn gc-btn-conceder"
                                onclick="gcConceder(${userId}, ${conquistaId}, '${escapeHtml(nome)}', ${xp}, '${escapeHtml(icone)}', this)"
                                title="Conceder conquista (+${xp} XP)">
                            <i class="fa fa-plus"></i> Dar
                        </button>
                    </div>`);
            }

            toastSucesso(`🗑 "${nome}" revogada. −${data.xpDeduzido || xp} XP descontados.`);
        } else {
            btn.disabled = false;
            toastErro(data.mensagem || data.erro || "Erro ao revogar.");
        }
    } catch {
        btn.disabled = false;
        toastErro("Erro de comunicação.");
    }
}

// Chamado pelo botão "Dar" dentro do modal
async function gcConceder(userId, conquistaId, nome, xp, icone, btn) {
    btn.disabled = true;
    try {
        const resp = await fetch(`/admin/usuario/${userId}/conceder-conquista/${conquistaId}`, {
            method: "POST",
            headers: getCsrfHeaders()
        });
        const data = await resp.json();
        if (data.sucesso) {
            // Move da seção "bloqueadas" para "desbloqueadas"
            const row = document.getElementById(`gc-row-${conquistaId}`);
            if (row) row.remove();

            const listDes = document.getElementById("gc-list-des");
            const listBlo = document.getElementById("gc-list-blo");

            // Atualiza contadores
            const cntDes = document.getElementById("gc-count-des");
            const cntBlo = document.getElementById("gc-count-blo");
            const ctGeral = document.getElementById("gc-contador");
            const novasDes = (parseInt(cntDes?.textContent) || 0) + 1;
            const novasBlo = (parseInt(cntBlo?.textContent) || 1) - 1;
            if (cntDes) cntDes.textContent = novasDes;
            if (cntBlo) cntBlo.textContent = novasBlo;
            if (ctGeral) {
                const total = novasDes + novasBlo;
                ctGeral.textContent = `${novasDes} / ${total} desbloqueadas`;
            }

            // Se bloqueadas ficou vazio
            if (listBlo && !listBlo.querySelector(".gc-row")) {
                listBlo.innerHTML = `<p class="gc-empty">Usuário já possui todas as conquistas! 🏆</p>`;
            }

            const agora = new Date();
            const dataFmt = agora.toLocaleDateString("pt-BR") + " às " +
                            agora.toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" });

            // Insere na seção desbloqueadas
            if (listDes) {
                listDes.querySelector(".gc-empty")?.remove();
                listDes.insertAdjacentHTML("afterbegin", `
                    <div class="gc-row desbloqueada" id="gc-row-${conquistaId}">
                        <span class="gc-icone">${escapeHtml(icone)}</span>
                        <div class="gc-info">
                            <strong>${escapeHtml(nome)}</strong>
                            <small>+${xp} XP · ${dataFmt}</small>
                        </div>
                        <button class="gc-btn gc-btn-revogar"
                                onclick="gcRevogar(${userId}, ${conquistaId}, '${escapeHtml(nome)}', ${xp}, '${escapeHtml(icone)}', this)"
                                title="Revogar conquista (deduz ${xp} XP)">
                            <i class="fa fa-times"></i> Revogar
                        </button>
                    </div>`);
            }

            toastSucesso(`🏆 "${nome}" concedida! +${xp} XP adicionados.`);
        } else {
            btn.disabled = false;
            toastErro(data.mensagem || data.erro || "Erro ao conceder.");
        }
    } catch {
        btn.disabled = false;
        toastErro("Erro de comunicação.");
    }
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
        headers: getCsrfHeaders(),
        body: JSON.stringify(resultado)
    });
    const data = await resp.json();

    if (data.sucesso) {
        toastSucesso("Item atualizado!");
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

    const resp = await fetch(`/admin/item/${itemId}`, {
        method: "DELETE",
        headers: getCsrfHeaders()
    });
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
            headers: getCsrfHeaders(),
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
        width: "460px",
        html: `
            <div style="display:flex;flex-direction:column;gap:14px;text-align:left; max-width:100%;">
                <div>
                    <label style="font-size:0.75rem;font-weight:700;color:#7f8c8d;text-transform:uppercase;letter-spacing:.5px">NOVA SENHA</label>
                    <div style="position:relative; display:flex; align-items:center;">
                        <input id="swal-nova-senha" class="swal2-input senha-verifica" type="password" placeholder="Nova senha" style="margin:6px 0 0 0; width:100%; padding-right:40px;">
                        <button type="button" onclick="togglePasswordAdmin(this)" style="position:absolute; right:10px; top:65%; transform:translateY(-50%); background:none; border:none; color:#7f8c8d; cursor:pointer;">
                            <i class="fa-solid fa-eye"></i>
                        </button>
                    </div>
                </div>

                <div>
                    <label style="font-size:0.75rem;font-weight:700;color:#7f8c8d;text-transform:uppercase;letter-spacing:.5px">CONFIRMAR</label>
                    <div style="position:relative; display:flex; align-items:center;">
                        <input id="swal-confirma-senha" class="swal2-input" type="password" placeholder="Confirmar nova senha" style="margin:6px 0 0 0; width:100%; padding-right:40px;">
                        <button type="button" onclick="togglePasswordAdmin(this)" style="position:absolute; right:10px; top:65%; transform:translateY(-50%); background:none; border:none; color:#7f8c8d; cursor:pointer;">
                            <i class="fa-solid fa-eye"></i>
                        </button>
                    </div>
                </div>

                <div id="box-requisitos" class="box-requisitos-adm" style="background:rgba(0,0,0,0.1); padding:10px; border-radius:8px; font-size:0.8rem; border:1px solid var(--border-color);">
                    <div id="req-tamanho" class="invalido" style="color:#e74c3c; margin-bottom:4px;"><i class="fa-solid fa-circle-xmark"></i> Mínimo 8 caracteres</div>
                    <div id="req-maiuscula" class="invalido" style="color:#e74c3c; margin-bottom:4px;"><i class="fa-solid fa-circle-check"></i> Uma letra maiúscula</div>
                    <div id="req-numero" class="invalido" style="color:#e74c3c; margin-bottom:4px;"><i class="fa-solid fa-circle-xmark"></i> Um número</div>
                    <div id="req-especial" class="invalido" style="color:#e74c3c;"><i class="fa-solid fa-circle-xmark"></i> Um caractere especial (!@#$%^&*)</div>
                </div>
            </div>
        `,
        showCancelButton: true,
        confirmButtonText: "Redefinir",
        cancelButtonText: "Cancelar",
        confirmButtonColor: "#f59e0b",
        background: isDark ? "#1e1e1e" : "#fff",
        color: isDark ? "#e0e0e0" : "#2c3e50",
        didOpen: () => {
            const inputSenha = document.getElementById('swal-nova-senha');
            const reqTamanho = document.getElementById('req-tamanho');
            const reqMaiuscula = document.getElementById('req-maiuscula');
            const reqNumero = document.getElementById('req-numero');
            const reqEspecial = document.getElementById('req-especial');

            inputSenha.addEventListener('input', () => {
                const valor = inputSenha.value;
                validarItemLinha(reqTamanho, valor.length >= 8);
                validarItemLinha(reqMaiuscula, /[A-Z]/.test(valor));
                validarItemLinha(reqNumero, /[0-9]/.test(valor));
                validarItemLinha(reqEspecial, /[!@#$%^&*(),.?":{}|<>]/.test(valor)); // Regex de símbolo especial
            });
        },
        preConfirm: () => {
            const nova = document.getElementById("swal-nova-senha").value;
            const confirma = document.getElementById("swal-confirma-senha").value;

            if (nova.length < 8 || !/[A-Z]/.test(nova) || !/[0-9]/.test(nova) || !/[!@#$%^&*(),.?":{}|<>]/.test(nova)) {
                Swal.showValidationMessage("A senha não atende aos requisitos mínimos.");
                return false;
            }
            if (nova !== confirma) {
                Swal.showValidationMessage("As senhas não coincidem.");
                return false;
            }
            return nova;
        }
    }).then(async result => {
        if (!result.isConfirmed) return;

        const resp = await fetch(`/admin/usuario/${userId}/senha`, {
            method: "POST",
            headers: getCsrfHeaders(),
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

        const resp = await fetch(`/admin/usuario/${userId}`, {
            method: "DELETE",
            headers: getCsrfHeaders()
        });
        const data = await resp.json();
        if (data.sucesso) {
            toastSucesso("Usuário deletado!");
            document.querySelector(`tr[data-id="${userId}"]`)?.remove();
        } else toastErro(data.erro || "Erro ao deletar.");
    });
}

// --- AÇÕES DE CONQUISTAS ---
function initAcoesConquistas() {
    document.getElementById("btn-nova-conquista")?.addEventListener("click", () => abrirFormConquista(null));

    // Delegação de eventos estável para os botões da lista de conquistas
    document.addEventListener("click", (e) => {
        const btnEditConq = e.target.closest(".btn-editar-conquista");
        const btnDelConq = e.target.closest(".btn-deletar-conquista");

        if (btnEditConq) abrirFormConquista({
            ...btnEditConq.dataset,
            criterioTipo:  btnEditConq.dataset.criteriotipo  || "MANUAL",
            criterioValor: btnEditConq.dataset.criteriovalor || ""
        });
        if (btnDelConq) deletarConquista(btnDelConq.dataset.id, btnDelConq.dataset.nome);
    });
}

function abrirFormConquista(dados) {
    const isEdicao = !!dados;
    const isDark = isDarkMode();

    const CRITERIOS = [
        { value: "MANUAL",           label: "Manual (concedida pelo admin)" },
        { value: "TOTAL_ITENS",      label: "Total de itens cadastrados" },
        { value: "TOTAL_CONCLUIDOS", label: "Total de itens concluídos" },
        { value: "TOTAL_DROPADOS",   label: "Total de itens dropados" },
        { value: "JOGOS_ZERADOS",    label: "Jogos zerados" },
        { value: "FILMES_ASSISTIDOS",label: "Filmes assistidos" },
        { value: "SERIES_ASSISTIDAS",label: "Séries assistidas" },
        { value: "NOTA10_FILMES",    label: "Filmes com nota 10" },
        { value: "NOTA10_JOGOS",     label: "Jogos com nota 10" },
        { value: "NOTA10_TOTAL",     label: "Itens com nota 10 (qualquer tipo)" },
        { value: "TMDB_CAPA",        label: "Cadastrou item com capa do TMDB" },
        { value: "SHARE_LINK_CRIADO",label: "Criou link de compartilhamento" },
        { value: "AI_USADA",         label: "Usou a IA pela primeira vez" },
    ];

    const criterioAtual = isEdicao ? (dados.criterioTipo || "MANUAL") : "MANUAL";
    const valorAtual    = isEdicao ? (dados.criterioValor || "") : "";

    const optionsHtml = CRITERIOS.map(c =>
        `<option value="${c.value}"${criterioAtual === c.value ? " selected" : ""}>${c.label}</option>`
    ).join("");

    const labelStyle = "display:block;font-size:0.75rem;font-weight:700;color:#7f8c8d;text-transform:uppercase;letter-spacing:.5px;margin-bottom:6px";
    const hintStyle  = "font-size:0.72rem;color:#7f8c8d;margin-top:4px";

    Swal.fire({
        title: isEdicao ? "✏️ Editar Conquista" : "🏆 Nova Conquista",
        width: "540px",
        html: `
            <div style="display:flex;flex-direction:column;gap:18px;text-align:left;padding:4px 0">
                <div style="display:grid;grid-template-columns:90px 1fr;gap:14px">
                    <div>
                        <label style="${labelStyle}">ÍCONE</label>
                        <input id="sc-icone" class="swal2-input" value="${isEdicao ? escapeHtml(dados.icone) : "🏆"}" maxlength="5" style="text-align:center;font-size:1.6rem;padding:8px;height:52px;margin:0;width:100%">
                    </div>
                    <div>
                        <label style="${labelStyle}">XP GANHO</label>
                        <input id="sc-xp" class="swal2-input" type="number" value="${isEdicao ? dados.xp : "50"}" min="1" placeholder="50" style="margin:0;width:100%;height:52px">
                    </div>
                </div>
                <div>
                    <label style="${labelStyle}">CHAVE ÚNICA</label>
                    <input id="sc-chave" class="swal2-input" value="${isEdicao ? escapeHtml(dados.chave) : ""}" placeholder="Ex: PRIMEIRO_FILME" style="text-transform:uppercase;margin:0;width:100%">
                </div>
                <div>
                    <label style="${labelStyle}">NOME</label>
                    <input id="sc-nome" class="swal2-input" value="${isEdicao ? escapeHtml(dados.nome) : ""}" placeholder="Nome da conquista" style="margin:0;width:100%">
                </div>
                <div>
                    <label style="${labelStyle}">DESCRIÇÃO</label>
                    <textarea id="sc-desc" class="swal2-textarea" placeholder="Descrição exibida ao usuário" style="margin:0;width:100%;min-height:80px;resize:vertical;box-sizing:border-box">${isEdicao ? escapeHtml(dados.descricao) : ""}</textarea>
                </div>
                <div style="border-top:1px solid rgba(127,140,141,0.2);padding-top:14px">
                    <label style="${labelStyle}">🎯 CRITÉRIO DE DESBLOQUEIO</label>
                    <select id="sc-criterio" class="swal2-select" style="margin:0;width:100%;height:44px;border-radius:8px">
                        ${optionsHtml}
                    </select>
                    <p style="${hintStyle}">Define quando o sistema desbloqueia essa conquista automaticamente.</p>
                </div>
                <div id="sc-valor-wrap">
                    <label style="${labelStyle}">QUANTIDADE MÍNIMA</label>
                    <input id="sc-valor" class="swal2-input" type="number" value="${valorAtual}" min="1" placeholder="Ex: 10" style="margin:0;width:100%">
                    <p style="${hintStyle}">Número de itens necessários para atingir o critério acima.</p>
                </div>
            </div>
        `,
        showCancelButton: true,
        confirmButtonText: isEdicao ? "Salvar Edição" : "Criar Conquista",
        cancelButtonText: "Cancelar",
        confirmButtonColor: "#7c3aed",
        background: isDark ? "#1e1e1e" : "#fff",
        color: isDark ? "#e0e0e0" : "#2c3e50",
        didOpen: () => {
            const sel  = document.getElementById("sc-criterio");
            const wrap = document.getElementById("sc-valor-wrap");
            const CRITERIOS_SEM_VALOR = new Set(["MANUAL", "AI_USADA", "TMDB_CAPA", "SHARE_LINK_CRIADO"]);
            const toggle = () => { wrap.style.display = CRITERIOS_SEM_VALOR.has(sel.value) ? "none" : ""; };
            toggle();
            sel.addEventListener("change", toggle);
        },
        preConfirm: () => {
            const icone      = document.getElementById("sc-icone").value.trim();
            const chave      = document.getElementById("sc-chave").value.trim().toUpperCase();
            const nome       = document.getElementById("sc-nome").value.trim();
            const desc       = document.getElementById("sc-desc").value.trim();
            const xp         = parseInt(document.getElementById("sc-xp").value);
            const criterio   = document.getElementById("sc-criterio").value;
            const valorRaw   = document.getElementById("sc-valor")?.value;
            const criteriosSemValor = new Set(["MANUAL", "AI_USADA", "TMDB_CAPA", "SHARE_LINK_CRIADO"]);
            const valor      = criteriosSemValor.has(criterio) ? null : parseInt(valorRaw);

            if (!icone || !chave || !nome || !desc || isNaN(xp) || xp < 1) {
                Swal.showValidationMessage("Preencha todos os campos corretamente.");
                return false;
            }
            if (!criteriosSemValor.has(criterio) && (isNaN(valor) || valor < 1)) {
                Swal.showValidationMessage("Informe a quantidade mínima para o critério escolhido.");
                return false;
            }
            return { icone, chave, nome, descricao: desc, xp, criterioTipo: criterio, criterioValor: valor };
        }
    }).then(async result => {
        if (!result.isConfirmed) return;
        const url = isEdicao ? `/admin/conquista/${dados.id}/editar` : `/admin/conquista/criar`;

        const resp = await fetch(url, {
            method: "POST",
            headers: getCsrfHeaders(),
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

        const resp = await fetch(`/admin/conquista/${id}`, {
            method: "DELETE",
            headers: getCsrfHeaders()
        });
        const data = await resp.json();
        if (data.sucesso) { toastSucesso("Conquista deletada!"); setTimeout(() => location.reload(), 1200); }
        else toastErro(data.erro || "Erro ao deletar conquista.");
    });
}

// --- MODAL FECHAR ---
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

// --- UTILITÁRIOS ---
function isDarkMode() {
    return document.documentElement.getAttribute("data-theme") === "dark";
}

function toastSucesso(msg) {
    Swal.fire({ toast: true, position: "bottom-end", icon: "success", title: msg, showConfirmButton: false, timer: 2500, timerProgressBar: true });
}

function togglePassword(btn) {} // mantendo coerência com assinatura do front

function toastErro(msg) {
    Swal.fire({ toast: true, position: "bottom-end", icon: "error", title: msg, showConfirmButton: false, timer: 3000 });
}

function escapeHtml(text) {
    if (text == null) return "";
    return String(text)
        .replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;").replace(/'/g, "&#39;").replace(/`/g, "&#96;");
}

function togglePasswordAdmin(botao) {
    const input = botao.previousElementSibling;
    const icone = botao.querySelector('i');
    if (input.type === "password") {
        input.type = "text";
        icone.className = 'fa-solid fa-eye-slash';
    } else {
        input.type = "password";
        icone.className = 'fa-solid fa-eye';
    }
}

function validarItemLinha(elemento, valido) {
    if (!elemento) return;
    const icone = elemento.querySelector('i');
    if (valido) {
        elemento.style.color = "#2ecc71";
        icone.className = 'fa-solid fa-circle-check';
    } else {
        elemento.style.color = "#e74c3c";
        icone.className = 'fa-solid fa-circle-xmark';
    }
}

// --- ABA DE AUDITORIA ---

const auditState = {
    pagina: 0,
    totalPaginas: 0,
    acao: "",
    alvoNome: "",
    carregando: false
};

function initAuditoria() {
    // Carrega ações distintas para o dropdown quando a aba for aberta pela primeira vez
    document.querySelector('[data-tab="auditoria"]')?.addEventListener("click", () => {
        if (!auditState._inicializado) {
            auditState._inicializado = true;
            carregarAcoesDistintas();
            buscarAuditLog(0);
        }
    });

    document.getElementById("btn-audit-buscar")?.addEventListener("click", () => {
        auditState.acao     = document.getElementById("audit-filter-acao")?.value || "";
        auditState.alvoNome = document.getElementById("audit-filter-alvo")?.value.trim() || "";
        buscarAuditLog(0);
    });

    document.getElementById("btn-audit-limpar")?.addEventListener("click", () => {
        document.getElementById("audit-filter-acao").value  = "";
        document.getElementById("audit-filter-alvo").value  = "";
        auditState.acao     = "";
        auditState.alvoNome = "";
        buscarAuditLog(0);
    });

    // Permite buscar com Enter no campo de texto
    document.getElementById("audit-filter-alvo")?.addEventListener("keydown", (e) => {
        if (e.key === "Enter") document.getElementById("btn-audit-buscar")?.click();
    });

    // Auto-refresh silencioso a cada 30 segundos enquanto a aba estiver visível
    setInterval(() => {
        const abaAtiva = document.getElementById("tab-auditoria");
        if (abaAtiva && !abaAtiva.classList.contains("hidden") && !auditState.carregando) {
            buscarAuditLog(auditState.pagina, /* silencioso */ true);
        }
    }, 30000);
}

async function carregarAcoesDistintas() {
    try {
        const res  = await fetch("/admin/api/audit-log/acoes");
        const acoes = await res.json();
        const sel  = document.getElementById("audit-filter-acao");
        if (!sel) return;
        acoes.forEach(a => {
            const opt = document.createElement("option");
            opt.value = a;
            opt.textContent = formatarAcao(a);
            sel.appendChild(opt);
        });
    } catch (err) {
        console.error("[Auditoria] Erro ao carregar ações:", err);
    }
}

async function buscarAuditLog(pagina, silencioso = false) {
    if (auditState.carregando) return;
    auditState.carregando = true;

    const tbody = document.getElementById("audit-tbody");
    if (!silencioso) {
        tbody.innerHTML = `<tr id="audit-loading"><td colspan="4" class="audit-estado"><i class="fa fa-spinner fa-spin"></i> Carregando...</td></tr>`;
    }

    const params = new URLSearchParams({ page: pagina, size: 30 });
    if (auditState.acao)     params.set("acao", auditState.acao);
    if (auditState.alvoNome) params.set("alvoNome", auditState.alvoNome);

    try {
        const res  = await fetch("/admin/api/audit-log?" + params.toString());
        const data = await res.json();

        auditState.pagina       = data.paginaAtual;
        auditState.totalPaginas = data.totalPaginas;
        auditState.carregando   = false;

        renderizarTabelaAudit(data.registros, data.totalItens);
        renderizarPaginacaoAudit(data.paginaAtual, data.totalPaginas);
    } catch (err) {
        auditState.carregando = false;
        tbody.innerHTML = `<tr><td colspan="5" class="audit-estado">❌ Erro ao carregar logs. Tente novamente.</td></tr>`;
        console.error("[Auditoria] Erro:", err);
    }
}

function renderizarTabelaAudit(registros, total) {
    const tbody = document.getElementById("audit-tbody");

    if (!registros || registros.length === 0) {
        tbody.innerHTML = `<tr><td colspan="5" class="audit-estado">📭 Nenhum registro encontrado para os filtros aplicados.</td></tr>`;
        return;
    }

    tbody.innerHTML = registros.map(r => `
        <tr>
            <td class="audit-data">${formatarDataAudit(r.criadoEm)}</td>
            <td><span class="audit-acao-badge ${categoriaAcao(r.acao)}">${formatarAcao(r.acao)}</span></td>
            <td class="audit-descricao">${escapeHtml(r.descricao)}</td>
            <td class="audit-detalhe">${r.detalhe ? escapeHtml(r.detalhe) : '<span style="opacity:0.4">—</span>'}</td>
        </tr>
    `).join("");
}

function renderizarPaginacaoAudit(paginaAtual, totalPaginas) {
    const container = document.getElementById("audit-paginacao");
    if (!container) return;

    if (totalPaginas <= 1) {
        container.innerHTML = "";
        return;
    }

    let html = "";

    // Botão anterior
    html += `<button class="audit-pag-btn" onclick="buscarAuditLog(${paginaAtual - 1})" ${paginaAtual === 0 ? "disabled" : ""}>
        <i class="fa fa-chevron-left"></i>
    </button>`;

    // Páginas numeradas (janela de 5 ao redor da atual)
    const inicio = Math.max(0, paginaAtual - 2);
    const fim    = Math.min(totalPaginas - 1, paginaAtual + 2);

    if (inicio > 0) {
        html += `<button class="audit-pag-btn" onclick="buscarAuditLog(0)">1</button>`;
        if (inicio > 1) html += `<span style="padding: 0 4px; color: var(--adm-muted)">…</span>`;
    }

    for (let i = inicio; i <= fim; i++) {
        html += `<button class="audit-pag-btn ${i === paginaAtual ? 'active' : ''}" onclick="buscarAuditLog(${i})">${i + 1}</button>`;
    }

    if (fim < totalPaginas - 1) {
        if (fim < totalPaginas - 2) html += `<span style="padding: 0 4px; color: var(--adm-muted)">…</span>`;
        html += `<button class="audit-pag-btn" onclick="buscarAuditLog(${totalPaginas - 1})">${totalPaginas}</button>`;
    }

    // Botão próximo
    html += `<button class="audit-pag-btn" onclick="buscarAuditLog(${paginaAtual + 1})" ${paginaAtual >= totalPaginas - 1 ? "disabled" : ""}>
        <i class="fa fa-chevron-right"></i>
    </button>`;

    container.innerHTML = html;
}

// Mapeia código da ação para texto legível no dropdown e na badge
function formatarAcao(acao) {
    const mapa = {
        "CONTA_CRIADA":         "Conta criada",
        "CONTA_DELETADA":       "Conta deletada",
        "SENHA_REDEFINIDA":     "Senha redefinida",
        "SENHA_ALTERADA":       "Senha alterada",
        "USUARIO_EDITADO":      "Usuário editado",
        "CONQUISTA_CRIADA":     "Conquista criada",
        "CONQUISTA_EDITADA":    "Conquista editada",
        "CONQUISTA_DELETADA":   "Conquista deletada",
        "CONQUISTA_CONCEDIDA":  "Conquista concedida",
        "CONQUISTA_REVOGADA":   "Conquista revogada",
        "ITEM_DELETADO":        "Item deletado",
    };
    return mapa[acao] || acao;
}

// Retorna a classe CSS de cor conforme categoria da ação
function categoriaAcao(acao) {
    if (!acao) return "cat-default";
    if (acao.startsWith("CONTA_"))     return "cat-conta";
    if (acao.startsWith("SENHA_"))     return "cat-senha";
    if (acao.startsWith("CONQUISTA_")) return "cat-conquista";
    if (acao.startsWith("ITEM_"))      return "cat-item";
    if (acao.startsWith("USUARIO_"))   return "cat-usuario";
    return "cat-default";
}

function formatarDataAudit(isoStr) {
    if (!isoStr) return "—";
    try {
        const d = new Date(isoStr);
        return d.toLocaleDateString("pt-BR", { day: "2-digit", month: "2-digit", year: "numeric" })
             + " " + d.toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" });
    } catch { return isoStr; }
}

function escapeHtml(str) {
    if (!str) return "";
    return String(str)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;");
}

// --- ABA SISTEMA — Alavancas de controle ---

function initSistema() {
    // Toggles de alavancas booleanas
    document.querySelectorAll(".sistema-toggle").forEach(input => {
        input.addEventListener("change", async (e) => {
            const chave = e.target.dataset.chave;
            const novoEstado = e.target.checked;

            const nomes = {
                MODO_MANUTENCAO:  "Modo Manutenção",
                MODO_INSTAVEL:    "Modo Instabilidade",
                SISTEMA_BLOQUEADO:"Bloqueio Total do Sistema",
                MODO_READONLY:    "Modo Somente-Leitura",
            };

            const icons = {
                MODO_MANUTENCAO:  "🛠️",
                MODO_INSTAVEL:    "⚠️",
                SISTEMA_BLOQUEADO:"🔒",
                MODO_READONLY:    "📖",
            };

            const textoAcao = novoEstado ? "ATIVAR" : "DESATIVAR";
            const corBotao  = novoEstado
                ? (chave === "SISTEMA_BLOQUEADO" ? "#dc2626" : "#7c3aed")
                : "#6b7280";

            // Reverte o toggle visualmente enquanto aguarda confirmação
            e.target.checked = !novoEstado;

            const { isConfirmed } = await Swal.fire({
                title: `${icons[chave] || "⚙️"} Confirmar ação`,
                html: `Você deseja <strong>${textoAcao}</strong> a alavanca<br><em>${nomes[chave] || chave}</em>?`,
                icon: novoEstado && chave === "SISTEMA_BLOQUEADO" ? "warning" : "question",
                showCancelButton: true,
                confirmButtonText: textoAcao,
                cancelButtonText: "Cancelar",
                confirmButtonColor: corBotao,
                background: "#161620",
                color: "#f1f1f5",
            });

            if (!isConfirmed) return;

            try {
                const res = await fetch(`/admin/sistema/toggle/${chave}`, {
                    method: "POST",
                    headers: getCsrfHeaders(),
                });
                const data = await res.json();

                if (data.sucesso) {
                    // Aplica o novo estado
                    e.target.checked = data.ativo;
                    atualizarBadgeSistema(chave, data.ativo);

                    Swal.fire({
                        toast: true,
                        position: "top-end",
                        icon: "success",
                        title: data.ativo ? `${nomes[chave]} ativado` : `${nomes[chave]} desativado`,
                        showConfirmButton: false,
                        timer: 2500,
                        background: "#161620",
                        color: "#f1f1f5",
                    });
                } else {
                    throw new Error(data.erro || "Erro desconhecido");
                }
            } catch (err) {
                Swal.fire({
                    icon: "error",
                    title: "Erro",
                    text: err.message || "Não foi possível alterar a configuração.",
                    background: "#161620",
                    color: "#f1f1f5",
                });
            }
        });
    });

    // Salvar novidades
    const btnSalvar = document.getElementById("btn-salvar-novidades");
    if (btnSalvar) {
        btnSalvar.addEventListener("click", async () => {
            const texto = document.getElementById("textarea-novidades")?.value?.trim() || "";

            const { isConfirmed } = await Swal.fire({
                title: "📢 Salvar comunicado?",
                html: texto
                    ? `O seguinte comunicado será exibido para todos os usuários:<br><br><em style="color:#d1d5db">"${texto.substring(0, 80)}${texto.length > 80 ? '…' : ''}"</em>`
                    : "O campo está vazio. Isso <strong>removerá</strong> o comunicado ativo.",
                icon: "question",
                showCancelButton: true,
                confirmButtonText: "Salvar",
                cancelButtonText: "Cancelar",
                confirmButtonColor: "#7c3aed",
                background: "#161620",
                color: "#f1f1f5",
            });

            if (!isConfirmed) return;

            try {
                const res = await fetch("/admin/sistema/novidades", {
                    method: "POST",
                    headers: getCsrfHeaders(),
                    body: JSON.stringify({ texto }),
                });
                const data = await res.json();

                if (data.sucesso) {
                    atualizarBadgeSistema("NOVIDADES", data.ativo);
                    Swal.fire({
                        toast: true,
                        position: "top-end",
                        icon: "success",
                        title: data.ativo ? "Comunicado salvo e ativo" : "Comunicado removido",
                        showConfirmButton: false,
                        timer: 2500,
                        background: "#161620",
                        color: "#f1f1f5",
                    });
                }
            } catch (err) {
                Swal.fire({ icon: "error", title: "Erro", text: err.message, background: "#161620", color: "#f1f1f5" });
            }
        });
    }

    // Disparar novidades por e-mail — abre seletor de destinatários
    const btnEmail = document.getElementById("btn-email-novidades");
    if (btnEmail) {
        btnEmail.addEventListener("click", async () => {
            const texto = document.getElementById("textarea-novidades")?.value?.trim() || "";

            if (!texto) {
                Swal.fire({
                    icon: "warning",
                    title: "Campo vazio",
                    text: "Escreva o comunicado no campo de texto antes de disparar o e-mail.",
                    background: "#161620",
                    color: "#f1f1f5",
                });
                return;
            }

            abrirModalDestinatarios(texto);
        });
    }

    // Limpar novidades
    const btnLimpar = document.getElementById("btn-limpar-novidades");
    if (btnLimpar) {
        btnLimpar.addEventListener("click", async () => {
            const { isConfirmed } = await Swal.fire({
                title: "Remover comunicado?",
                text: "O comunicado será apagado e não aparecerá mais para os usuários.",
                icon: "warning",
                showCancelButton: true,
                confirmButtonText: "Remover",
                cancelButtonText: "Cancelar",
                confirmButtonColor: "#dc2626",
                background: "#161620",
                color: "#f1f1f5",
            });

            if (!isConfirmed) return;

            const textarea = document.getElementById("textarea-novidades");
            if (textarea) textarea.value = "";

            try {
                const res = await fetch("/admin/sistema/novidades", {
                    method: "POST",
                    headers: getCsrfHeaders(),
                    body: JSON.stringify({ texto: "" }),
                });
                const data = await res.json();
                if (data.sucesso) {
                    atualizarBadgeSistema("NOVIDADES", false);
                    Swal.fire({
                        toast: true,
                        position: "top-end",
                        icon: "success",
                        title: "Comunicado removido",
                        showConfirmButton: false,
                        timer: 2000,
                        background: "#161620",
                        color: "#f1f1f5",
                    });
                }
            } catch (err) {
                Swal.fire({ icon: "error", title: "Erro", text: err.message, background: "#161620", color: "#f1f1f5" });
            }
        });
    }
}

// Atualiza visualmente o badge de status de uma alavanca
function atualizarBadgeSistema(chave, ativo) {
    // Badge dentro do card
    const card = document.getElementById(`card-${chave}`);
    if (!card) return;

    const badge = card.querySelector(".sistema-badge");
    if (!badge) return;

    badge.classList.remove("ativo", "inativo", "danger");

    if (ativo) {
        badge.classList.add("ativo");
        badge.textContent = chave === "SISTEMA_BLOQUEADO" ? "● BLOQUEADO" : "● Ativo";
        if (chave === "SISTEMA_BLOQUEADO") badge.classList.add("danger");
    } else {
        badge.classList.add("inativo");
        badge.textContent = "○ Inativo";
    }
}

// --- MODAL: SELETOR DE DESTINATÁRIOS DE E-MAIL ---

let _emailUsuariosCached = null; // cache para evitar fetch repetido na mesma sessão

async function abrirModalDestinatarios(textoComunicado) {
    const modal   = document.getElementById("modal-email-destinatarios");
    const lista   = document.getElementById("email-sel-lista");
    const busca   = document.getElementById("email-sel-busca");
    const contador = document.getElementById("email-sel-contador");
    const badge   = document.getElementById("email-sel-badge");
    const aviso   = document.getElementById("email-sel-vazio-aviso");

    // Abre o modal e zera estado
    modal.classList.remove("hidden");
    busca.value = "";
    aviso.classList.add("hidden");
    lista.innerHTML = `<p class="empty-modal">⏳ Carregando usuários…</p>`;
    atualizarContador(0);

    // Carrega lista de usuários (com cache)
    try {
        if (!_emailUsuariosCached) {
            const res = await fetch("/admin/api/usuarios-para-email");
            _emailUsuariosCached = await res.json();
        }
    } catch (err) {
        lista.innerHTML = `<p class="empty-modal">❌ Erro ao carregar usuários. Tente novamente.</p>`;
        return;
    }

    const usuarios = _emailUsuariosCached;
    if (!usuarios || usuarios.length === 0) {
        lista.innerHTML = `<p class="empty-modal">Nenhum usuário ativo encontrado.</p>`;
        return;
    }

    // Renderiza os itens
    renderizarLista(usuarios, lista);

    // Evento: pesquisa em tempo real
    busca.oninput = () => filtrarLista(busca.value.trim().toLowerCase());

    // Evento: selecionar todos
    document.getElementById("email-sel-todos").onclick = () => {
        document.querySelectorAll(".email-sel-item:not(.oculto)").forEach(el => marcarItem(el, true));
        atualizarContador();
    };

    // Evento: tirar todos
    document.getElementById("email-sel-nenhum").onclick = () => {
        document.querySelectorAll(".email-sel-item:not(.oculto)").forEach(el => marcarItem(el, false));
        atualizarContador();
    };

    // Evento: confirmar
    document.getElementById("email-sel-confirmar").onclick = () =>
        confirmarEnvio(textoComunicado, aviso);
}

function renderizarLista(usuarios, container) {
    container.innerHTML = "";
    usuarios.forEach(u => {
        const item = document.createElement("div");
        item.className = "email-sel-item";
        item.dataset.email = u.email;
        item.dataset.login = u.login.toLowerCase();
        item.innerHTML = `
            <div class="email-sel-check"><i class="fa fa-check"></i></div>
            <div class="email-sel-info">
                <span class="email-sel-login">${escapeHtml(u.login)}</span>
                <span class="email-sel-email">${escapeHtml(u.email)}</span>
            </div>`;
        item.addEventListener("click", () => {
            const selecionado = item.classList.contains("selecionado");
            marcarItem(item, !selecionado);
            atualizarContador();
        });
        container.appendChild(item);
    });
}

function marcarItem(el, selecionado) {
    el.classList.toggle("selecionado", selecionado);
}

function filtrarLista(termo) {
    document.querySelectorAll(".email-sel-item").forEach(el => {
        const login = el.dataset.login || "";
        const email = el.dataset.email?.toLowerCase() || "";
        const visivel = !termo || login.includes(termo) || email.includes(termo);
        el.classList.toggle("oculto", !visivel);
    });

    // Aviso de nenhum resultado
    const lista = document.getElementById("email-sel-lista");
    const vazio = lista.querySelector(".email-sel-vazio");
    const temVisivel = [...document.querySelectorAll(".email-sel-item:not(.oculto)")].length > 0;
    if (!temVisivel && !vazio) {
        const p = document.createElement("p");
        p.className = "email-sel-vazio";
        p.textContent = "Nenhum usuário encontrado para esta pesquisa.";
        lista.appendChild(p);
    } else if (temVisivel && vazio) {
        vazio.remove();
    }
}

function atualizarContador(n) {
    const total = n !== undefined
        ? n
        : document.querySelectorAll(".email-sel-item.selecionado").length;
    const el = document.getElementById("email-sel-contador");
    if (!el) return;
    el.textContent = total === 0 ? "0 selecionados"
        : total === 1 ? "1 selecionado"
        : `${total} selecionados`;
    el.classList.toggle("tem-selecao", total > 0);
}

async function confirmarEnvio(textoComunicado, avisoEl) {
    const selecionados = [...document.querySelectorAll(".email-sel-item.selecionado")]
        .map(el => el.dataset.email);

    if (selecionados.length === 0) {
        avisoEl.classList.remove("hidden");
        return;
    }
    avisoEl.classList.add("hidden");

    // Fecha o modal de seleção
    document.getElementById("modal-email-destinatarios").classList.add("hidden");

    // Monta texto do resumo de destinatários para o SweetAlert
    const totalAtivos = _emailUsuariosCached?.length || 0;
    const todosSelecionados = selecionados.length === totalAtivos;

    const resumoDestinatarios = todosSelecionados
        ? `<strong>todos os ${selecionados.length} usuários ativos</strong>`
        : selecionados.length <= 5
            ? selecionados.map(e => `<code style="font-size:0.8rem">${escapeHtml(e)}</code>`).join(", ")
            : `<strong>${selecionados.length} usuários selecionados</strong>`;

    const { isConfirmed } = await Swal.fire({
        title: "📧 Confirmar disparo de e-mail?",
        html: `O comunicado será enviado para ${resumoDestinatarios}:<br><br>`
            + `<em style="color:#d1d5db">"${escapeHtml(textoComunicado.substring(0, 120))}${textoComunicado.length > 120 ? '…' : ''}"</em>`
            + `<br><br><span style="font-size:0.82rem;color:#9ca3af">O envio roda em segundo plano. Pode levar alguns minutos.</span>`,
        icon: "question",
        showCancelButton: true,
        confirmButtonText: "Disparar e-mail",
        cancelButtonText: "Voltar",
        confirmButtonColor: "#0ea5e9",
        background: "#161620",
        color: "#f1f1f5",
    });

    if (!isConfirmed) {
        // Reabre o modal se o admin voltar
        document.getElementById("modal-email-destinatarios").classList.remove("hidden");
        return;
    }

    // Dispara
    const btnEmail = document.getElementById("btn-email-novidades");
    if (btnEmail) {
        btnEmail.disabled = true;
        btnEmail.innerHTML = '<i class="fa fa-spinner fa-spin"></i> Enviando…';
    }

    try {
        const res = await fetch("/admin/sistema/novidades/email", {
            method: "POST",
            headers: getCsrfHeaders(),
            body: JSON.stringify({ texto: textoComunicado, destinatarios: selecionados }),
        });
        const data = await res.json();

        if (data.sucesso) {
            Swal.fire({
                icon: "success",
                title: "E-mail em disparo!",
                html: `Comunicado enviado em segundo plano para <strong>${data.totalDestinatarios}</strong> usuário(s).`,
                background: "#161620",
                color: "#f1f1f5",
            });
        } else {
            Swal.fire({
                icon: "error",
                title: "Não foi possível disparar",
                text: data.erro || "Tente novamente em instantes.",
                background: "#161620",
                color: "#f1f1f5",
            });
        }
    } catch (err) {
        Swal.fire({ icon: "error", title: "Erro", text: err.message, background: "#161620", color: "#f1f1f5" });
    } finally {
        if (btnEmail) {
            btnEmail.disabled = false;
            btnEmail.innerHTML = '<i class="fa fa-paper-plane"></i> Disparar por e-mail';
        }
    }
}

function escapeHtml(str) {
    return String(str)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;");
}
