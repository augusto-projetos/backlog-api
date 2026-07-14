// Variáveis Globais
let graficoNotasInstance = null;
let graficoStatusInstance = null;
let dadosGlobaisDashboard = null;
let dadosNotasAtuais = null;

// Função auxiliar para pegar o token CSRF das metatags
function getCsrfHeaders() {
    const tokenMeta = document.querySelector('meta[name="_csrf"]');
    const headerMeta = document.querySelector('meta[name="_csrf_header"]');

    if (!tokenMeta || !headerMeta) {
        console.warn("CSRF Tokens não encontrados. Verifique o <head> do HTML.");
        return { 'Content-Type': 'application/json' };
    }

    const token = tokenMeta.getAttribute('content');
    const header = headerMeta.getAttribute('content');

    return {
        'Content-Type': 'application/json',
        [header]: token
    };
}

document.addEventListener('DOMContentLoaded', () => {
    carregarDashboard();
    initFiltrosGraficos();
});

window.addEventListener('ratingModeChanged', () => {
    if (dadosNotasAtuais) {
        renderizarGraficoNotas(dadosNotasAtuais);
    }
});

async function carregarDashboard() {
    try {
        // 1. Busca os dados no Backend
        const response = await fetch('/api/dashboard');
        if (!response.ok) throw new Error('Erro ao buscar dados');

        const dados = await response.json();
        dadosGlobaisDashboard = dados; // Salva os dados globalmente

        // 2. Configura as cores do tema
        const corJogos = '#08d9d6';   // Azul Neon
        const corFilmes = '#ff2e63';  // Rosa Neon
        const corSeries = '#fcd34d';  // Amarelo

        // --- GRÁFICO 1: PIZZA (Tipos) ---
        const ctxTipo = document.getElementById('graficoTipo').getContext('2d');
        new Chart(ctxTipo, {
            type: 'doughnut',
            data: {
                labels: ['Jogos 🎮', 'Filmes 🎬', 'Séries 📺'],
                datasets: [{
                    data: [dados.totalJogos, dados.totalFilmes, dados.totalSeries],
                    backgroundColor: [corJogos, corFilmes, corSeries],
                    borderWidth: 0
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'bottom',
                        labels: { color: '#7f8c8d' }
                    }
                }
            }
        });

        // --- GRÁFICO 2: BARRAS (Status) ---
        renderizarGraficoStatus({
            zerados: dados.totalZerados,
            jogando: dados.totalJogando,
            backlog: dados.totalBacklog,
            dropados: dados.totalDropados
        });

        // --- GRÁFICO 3: DISTRIBUIÇÃO DE NOTAS/ESTRELAS ---
        dadosNotasAtuais = dados;
        renderizarGraficoNotas(dados);

        // --- TEMPO INVESTIDO (Filmes/Jogos) ---
        renderizarTempoGasto(dados);

    } catch (erro) {
        console.error("Erro no dashboard:", erro);
    }
}

// --- GRÁFICO DE STATUS (com suporte a filtro por tipo) ---
function renderizarGraficoStatus(dadosStatus) {
    const canvasStatus = document.getElementById('graficoStatus');
    if (!canvasStatus) return;
    const ctxStatus = canvasStatus.getContext('2d');

    if (graficoStatusInstance) {
        graficoStatusInstance.destroy();
    }

    graficoStatusInstance = new Chart(ctxStatus, {
        type: 'bar',
        data: {
            labels: ['✅ Zerado / Assistido', '▶️ Jogando / Assistindo', '📋 Backlog', '❌ Dropado'],
            datasets: [{
                label: 'Quantidade',
                data: [dadosStatus.zerados, dadosStatus.jogando, dadosStatus.backlog, dadosStatus.dropados],
                backgroundColor: [
                    '#2ecc71', // Verde
                    '#3498db', // Azul
                    '#95a5a6', // Cinza
                    '#e74c3c'  // Vermelho
                ],
                borderRadius: 5
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    ticks: { color: '#7f8c8d', precision: 0, stepSize: 1 },
                    grid: { color: 'rgba(255,255,255,0.05)' }
                },
                x: {
                    ticks: { color: '#7f8c8d' },
                    grid: { display: false }
                }
            }
        }
    });
}

// --- FILTROS DE TIPO DOS GRÁFICOS (Status dos Itens / Distribuição de Notas) ---
function initFiltrosGraficos() {
    const filtroStatus = document.getElementById('filtroTipoStatus');
    const filtroNotas = document.getElementById('filtroTipoNotas');

    if (filtroStatus) {
        filtroStatus.addEventListener('change', async () => {
            const tipo = filtroStatus.value;
            try {
                const url = tipo ? `/api/dashboard/status?tipo=${encodeURIComponent(tipo)}` : '/api/dashboard/status';
                const resposta = await fetch(url);
                if (!resposta.ok) throw new Error('Erro ao buscar status filtrado');
                const dadosStatus = await resposta.json();
                renderizarGraficoStatus(dadosStatus);
            } catch (erro) {
                console.error('Erro ao filtrar gráfico de status:', erro);
            }
        });
    }

    if (filtroNotas) {
        filtroNotas.addEventListener('change', async () => {
            const tipo = filtroNotas.value;
            try {
                const url = tipo ? `/api/dashboard/notas?tipo=${encodeURIComponent(tipo)}` : '/api/dashboard/notas';
                const resposta = await fetch(url);
                if (!resposta.ok) throw new Error('Erro ao buscar notas filtradas');
                const notas = await resposta.json();
                // renderizarGraficoNotas espera um objeto com a chave "notas"
                const payload = { notas };
                dadosNotasAtuais = payload;
                renderizarGraficoNotas(payload);
            } catch (erro) {
                console.error('Erro ao filtrar gráfico de notas:', erro);
            }
        });
    }
}

// --- FUNÇÃO PARA DESENHAR O CARD/GRÁFICO DE TEMPO GASTO ---
let graficoTempoInstance = null;

// Formata minutos exatamente como pedido:
//  - menos de 60min  -> "30 min"
//  - múltiplo de 60  -> "2h"
//  - resto           -> "1h 30min"
function formatarTempo(minutosTotais) {
    const minutos = Math.round(minutosTotais || 0);
    if (minutos < 60) return `${minutos} min`;

    const horas = Math.floor(minutos / 60);
    const restoMin = minutos % 60;
    return restoMin === 0 ? `${horas}h` : `${horas}h ${restoMin}min`;
}

function renderizarTempoGasto(dados) {
    const minutosFilmes = dados.minutosFilmes || 0;
    const minutosJogos = dados.minutosJogos || 0;
    const minutosSeries = dados.minutosSeries || 0;

    const elFilmes = document.getElementById('tempoFilmesValor');
    const elJogos = document.getElementById('tempoJogosValor');
    const elSeries = document.getElementById('tempoSeriesValor');
    const elFrase = document.getElementById('tempoFrase');

    if (elFilmes) elFilmes.textContent = formatarTempo(minutosFilmes);
    if (elJogos) elJogos.textContent = formatarTempo(minutosJogos);
    if (elSeries) elSeries.textContent = formatarTempo(minutosSeries);

    // Frase "premium" que engaja o usuário, escolhida com base em quem lidera
    if (elFrase) {
        if (minutosFilmes === 0 && minutosJogos === 0 && minutosSeries === 0) {
            elFrase.textContent = '📈 Adicione filmes, séries e jogos para começar a acompanhar seu tempo investido!';
        } else if (minutosJogos >= minutosFilmes && minutosJogos >= minutosSeries) {
            elFrase.textContent = `🎮 Você já passou ${formatarTempo(minutosJogos)} jogando!`;
        } else if (minutosFilmes >= minutosSeries) {
            elFrase.textContent = `🍿 Você já passou ${formatarTempo(minutosFilmes)} assistindo filmes!`;
        } else {
            elFrase.textContent = `📺 Você já passou ${formatarTempo(minutosSeries)} assistindo séries!`;
        }
    }

    const canvasTempo = document.getElementById('graficoTempo');
    if (!canvasTempo) return;
    const ctxTempo = canvasTempo.getContext('2d');

    if (graficoTempoInstance) {
        graficoTempoInstance.destroy();
    }

    // Gradientes combinando com os cards acima
    const gradFilmes = ctxTempo.createLinearGradient(0, 0, 400, 0);
    gradFilmes.addColorStop(0, '#ff2e63');
    gradFilmes.addColorStop(1, '#ff6b9d');

    const gradJogos = ctxTempo.createLinearGradient(0, 0, 400, 0);
    gradJogos.addColorStop(0, '#08d9d6');
    gradJogos.addColorStop(1, '#0f766e');

    const gradSeries = ctxTempo.createLinearGradient(0, 0, 400, 0);
    gradSeries.addColorStop(0, '#fcd34d');
    gradSeries.addColorStop(1, '#f59e0b');

    graficoTempoInstance = new Chart(ctxTempo, {
        type: 'bar',
        data: {
            labels: ['🎬 Filmes', '🎮 Jogos', '📺 Séries'],
            datasets: [{
                label: 'Tempo',
                data: [minutosFilmes / 60, minutosJogos / 60, minutosSeries / 60],
                backgroundColor: [gradFilmes, gradJogos, gradSeries],
                borderRadius: 10,
                borderSkipped: false,
                barThickness: 42
            }]
        },
        options: {
            indexAxis: 'y',
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false },
                tooltip: {
                    callbacks: {
                        label: (context) => {
                            const minutosPorIndice = [minutosFilmes, minutosJogos, minutosSeries];
                            const minutosOriginais = minutosPorIndice[context.dataIndex];
                            return formatarTempo(minutosOriginais) + ' investidos';
                        }
                    }
                }
            },
            scales: {
                x: {
                    beginAtZero: true,
                    title: { display: true, text: 'Horas', color: '#7f8c8d', font: { size: 12, weight: 'bold' } },
                    ticks: { color: '#7f8c8d' },
                    grid: { color: 'rgba(255,255,255,0.05)' }
                },
                y: {
                    ticks: { color: '#7f8c8d', font: { size: 13 } },
                    grid: { display: false }
                }
            }
        }
    });
}

// --- FUNÇÃO PARA DESENHAR O GRÁFICO DE NOTAS/ESTRELAS ---
function renderizarGraficoNotas(dados) {
    const ratingMode = localStorage.getItem('ratingMode') || 'nota';
    const ctxNotas = document.getElementById('graficoNotas').getContext('2d');

    // Se o gráfico já existir, destrói para evitar sobreposição
    if (graficoNotasInstance) {
        graficoNotasInstance.destroy();
    }

    let labels = [];
    let dataValues = [];
    let coresBarras = [];
    let textoEixoX = '';

    const notasBackend = dados.notas || {};

    if (ratingMode === 'estrela') {
        // --- MODO ESTRELA (5 a 0) ---
        textoEixoX = 'Estrelas ( 5 a 0 )';

        // Agrupamos os dados brutos do backend na matemática de estrelas
        let contagemEstrelas = { '5':0, '4.5':0, '4':0, '3.5':0, '3':0, '2.5':0, '2':0, '1.5':0, '1':0, '0.5':0, '0':0 };

        for (let [notaStr, qtd] of Object.entries(notasBackend)) {
            let notaNum = parseFloat(notaStr);
            let notaArredondada = Math.ceil(notaNum);
            let numEstrelas = notaArredondada / 2;

            let chave = numEstrelas.toString();
            if (contagemEstrelas[chave] !== undefined) {
                contagemEstrelas[chave] += qtd;
            }
        }

        // 1. Transformamos o objeto em array e filtramos quem tem 0 itens
        const estrelasAtivas = Object.entries(contagemEstrelas)
            .filter(e => e[1] > 0); // Mantém só os que têm quantidade maior que 0

        // 2. Ordenamos do maior para o menor
        estrelasAtivas.sort((a, b) => parseFloat(b[0]) - parseFloat(a[0]));

        // 3. Separamos em labels e valores para o gráfico
        labels = estrelasAtivas.map(e => e[0]);
        dataValues = estrelasAtivas.map(e => e[1]);

        // Aplicamos exatamente o mesmo sistema de cores proporcional para as estrelas
        coresBarras = labels.map(estrelaStr => {
            const estrela = parseFloat(estrelaStr);
            if (estrela >= 4.5) return '#2ecc71'; // Equivalente a Nota >= 9 (Excelente)
            if (estrela >= 3.5) return '#3498db'; // Equivalente a Nota >= 7 (Bom)
            if (estrela >= 2.5) return '#f1c40f'; // Equivalente a Nota >= 5 (Médio)
            return '#e74c3c';                     // Ruim
        });

    } else {
        // --- MODO NOTA ---
        textoEixoX = 'Notas ( 10 a 0 )';

        // 1. Convertemos o Objeto em um Array de pares [ ["10", 1], ["9.5", 2] ]
        const entradasNotas = Object.entries(notasBackend);

        // 2. Ordenamos esse array na força bruta (Maior -> Menor)
        entradasNotas.sort((a, b) => parseFloat(b[0]) - parseFloat(a[0]));

        // 3. Separamos de volta para usar no gráfico com precisão total de decimais
        labels = entradasNotas.map(e => e[0]);
        dataValues = entradasNotas.map(e => e[1]);

        // 4. Lógica de cores aplicada na lista ordenada
        coresBarras = labels.map(notaStr => {
            const nota = parseFloat(notaStr);
            if (nota >= 9) return '#2ecc71';      // Verde (Excelente)
            if (nota >= 7) return '#3498db';      // Azul (Bom)
            if (nota >= 5) return '#f1c40f';      // Amarelo (Médio)
            return '#e74c3c';                     // Vermelho (Ruim)
        });
    }

    // --- CRIAÇÃO DO GRÁFICO COM CHART.JS ---
    graficoNotasInstance = new Chart(ctxNotas, {
        type: 'bar',
        data: {
            labels: labels,
            datasets: [{
                label: 'Itens',
                data: dataValues,
                backgroundColor: coresBarras,
                borderRadius: 4
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false },
                tooltip: {
                    callbacks: {
                        label: function(context) {
                            let complemento = ratingMode === 'estrela' ? ' estrelas' : ' de nota';
                            return context.raw + ' itens com ' + context.label + complemento;
                        }
                    }
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    title: { display: true, text: 'Quantidade', color: '#7f8c8d', font: { size: 12, weight: 'bold' } },
                    ticks: { color: '#7f8c8d', stepSize: 1 },
                    grid: { color: 'rgba(255,255,255,0.05)' }
                },
                x: {
                    title: { display: true, text: textoEixoX, color: '#7f8c8d', font: { size: 12, weight: 'bold' } },
                    ticks: { color: '#7f8c8d' },
                    grid: { display: false }
                }
            }
        }
    });
}

// ===================================================================
// PREENCHIMENTO EM LOTE DA DURAÇÃO DOS FILMES
// (o usuário decide, item por item, se aceita a sugestão do TMDB ou não)
// ===================================================================

let filmesSemDuracaoCache = [];

function escaparHtml(texto) {
    const div = document.createElement('div');
    div.textContent = texto ?? '';
    return div.innerHTML;
}

async function abrirModalTempoLote() {
    const overlay = document.getElementById('modalTempoLote');
    overlay.classList.add('aberto');

    document.getElementById('tempoLoteLoading').style.display = 'block';
    document.getElementById('tempoLoteVazio').style.display = 'none';
    document.getElementById('tempoLoteAcoes').style.display = 'none';
    document.getElementById('tempoLoteLista').innerHTML = '';

    try {
        const resposta = await fetch('/itens/filmes-sem-duracao');
        if (!resposta.ok) throw new Error('Erro ao buscar filmes sem duração');

        filmesSemDuracaoCache = await resposta.json();
        document.getElementById('tempoLoteLoading').style.display = 'none';

        if (filmesSemDuracaoCache.length === 0) {
            document.getElementById('tempoLoteVazio').style.display = 'block';
            return;
        }

        renderizarListaTempoLote(filmesSemDuracaoCache);
        document.getElementById('tempoLoteAcoes').style.display = 'flex';
    } catch (erro) {
        console.error(erro);
        document.getElementById('tempoLoteLoading').textContent = 'Erro ao carregar filmes. Tente novamente.';
    }
}

function fecharModalTempoLote() {
    document.getElementById('modalTempoLote').classList.remove('aberto');
}

function renderizarListaTempoLote(filmes) {
    const lista = document.getElementById('tempoLoteLista');
    lista.innerHTML = filmes.map(item => `
        <div class="tempo-lote-item" data-id="${item.id}">
            <label class="tempo-lote-checkbox">
                <input type="checkbox" class="chk-aplicar">
            </label>
            <div class="tempo-lote-info">
                <span class="tempo-lote-titulo">${escaparHtml(item.titulo)}</span>
                <span class="tempo-lote-status">Digite os minutos ou busque no TMDB</span>
            </div>
            <input type="number" class="tempo-lote-input" min="0" step="1" placeholder="min">
        </div>
    `).join('');
}

function alternarSelecionarTodos(checkboxMestre) {
    document.querySelectorAll('#tempoLoteLista .chk-aplicar').forEach(chk => {
        chk.checked = checkboxMestre.checked;
    });
}

async function buscarSugestoesTmdb() {
    const btn = document.getElementById('btnBuscarTmdbLote');
    const idsPendentes = filmesSemDuracaoCache.map(f => f.id);
    if (idsPendentes.length === 0) return;

    btn.disabled = true;
    btn.textContent = '🔍 Buscando no TMDB...';

    try {
        const resposta = await fetch('/itens/sugestoes-duracao', {
            method: 'POST',
            headers: getCsrfHeaders(),
            body: JSON.stringify(idsPendentes)
        });

        if (!resposta.ok) throw new Error('Erro ao buscar sugestões (status ' + resposta.status + ')');
        const sugestoes = await resposta.json();

        sugestoes.forEach(sugestao => {
            const linha = document.querySelector(`.tempo-lote-item[data-id="${sugestao.id}"]`);
            if (!linha) return;

            const input = linha.querySelector('.tempo-lote-input');
            const checkbox = linha.querySelector('.chk-aplicar');
            const status = linha.querySelector('.tempo-lote-status');

            if (sugestao.minutosSugeridos != null) {
                input.value = sugestao.minutosSugeridos;
                checkbox.checked = true;
                status.textContent = `✅ Sugestão do TMDB: ${formatarTempo(sugestao.minutosSugeridos)}`;
                linha.classList.add('aplicado');
            } else {
                status.textContent = '❓ Não encontrado no TMDB — digite manualmente';
            }
        });
    } catch (erro) {
        console.error(erro);
        if (window.Swal) {
            Swal.fire('Ops!', 'Não foi possível buscar as sugestões agora. Tente novamente.', 'error');
        }
    } finally {
        btn.disabled = false;
        btn.textContent = '🌐 Buscar automaticamente no TMDB';
    }
}

async function atualizarTempoGastoAposSalvar() {
    try {
        const resposta = await fetch('/api/dashboard');
        if (!resposta.ok) return;
        const dados = await resposta.json();
        dadosGlobaisDashboard = dados;
        renderizarTempoGasto(dados);
    } catch (erro) {
        console.error('Erro ao atualizar o card de Tempo Investido:', erro);
    }
}

async function salvarDuracaoLote() {
    const linhas = document.querySelectorAll('#tempoLoteLista .tempo-lote-item');
    const selecionados = [];

    linhas.forEach(linha => {
        const checkbox = linha.querySelector('.chk-aplicar');
        const input = linha.querySelector('.tempo-lote-input');
        if (checkbox.checked && input.value !== '' && Number(input.value) >= 0) {
            selecionados.push({
                id: Number(linha.dataset.id),
                duracaoMinutos: parseInt(input.value, 10)
            });
        }
    });

    if (selecionados.length === 0) {
        if (window.Swal) {
            Swal.fire('Nada selecionado', 'Marque a caixinha dos filmes que você quer salvar.', 'info');
        }
        return;
    }

    const btn = document.getElementById('btnSalvarLote');
    const label = document.getElementById('btnSalvarLoteLabel');
    const progresso = document.getElementById('btnSalvarLoteProgresso');
    const btnBuscar = document.getElementById('btnBuscarTmdbLote');

    const total = selecionados.length;
    let salvosComSucesso = 0;
    const idsComSucesso = new Set();

    // Trava os botões e zera a barra antes de começar
    btn.disabled = true;
    if (btnBuscar) btnBuscar.disabled = true;
    progresso.style.width = '0%';
    label.textContent = `💾 Salvando... 0/${total}`;

    // Salva um filme por vez (em sequência), atualizando a barra a cada resposta
    for (let i = 0; i < total; i++) {
        const item = selecionados[i];
        try {
            const resposta = await fetch(`/itens/${item.id}/duracao`, {
                method: 'PUT',
                headers: getCsrfHeaders(),
                body: JSON.stringify({ duracaoMinutos: item.duracaoMinutos })
            });

            if (resposta.ok) {
                salvosComSucesso++;
                idsComSucesso.add(item.id);
            }
        } catch (erro) {
            console.error('Erro ao salvar o filme', item.id, erro);
        }

        const feitos = i + 1;
        const percentual = Math.round((feitos / total) * 100);
        progresso.style.width = percentual + '%';
        label.textContent = `💾 Salvando... ${feitos}/${total}`;
    }

    // Remove da lista/cache só os que realmente foram salvos com sucesso
    filmesSemDuracaoCache = filmesSemDuracaoCache.filter(f => !idsComSucesso.has(f.id));

    if (filmesSemDuracaoCache.length === 0) {
        document.getElementById('tempoLoteLista').innerHTML = '';
        document.getElementById('tempoLoteAcoes').style.display = 'none';
        document.getElementById('tempoLoteVazio').style.display = 'block';
    } else {
        renderizarListaTempoLote(filmesSemDuracaoCache);
    }

    // Atualiza só o card/gráfico de Tempo Investido
    atualizarTempoGastoAposSalvar();

    // Restaura o botão ao estado normal
    btn.disabled = false;
    if (btnBuscar) btnBuscar.disabled = false;
    progresso.style.width = '0%';
    label.textContent = '💾 Salvar selecionados';

    if (window.Swal) {
        if (salvosComSucesso === total) {
            Swal.fire('Feito! 🎬', `${salvosComSucesso} filme(s) atualizado(s) com sucesso.`, 'success');
        } else {
            Swal.fire(
                'Concluído parcialmente',
                `${salvosComSucesso} de ${total} filme(s) foram salvos. Os que falharam continuam na lista para você tentar de novo.`,
                'warning'
            );
        }
    }
}
