// Variáveis Globais
let graficoNotasInstance = null;
let dadosGlobaisDashboard = null;

document.addEventListener('DOMContentLoaded', () => {
    carregarDashboard();
});

window.addEventListener('ratingModeChanged', () => {
    if (dadosGlobaisDashboard) {
        renderizarGraficoNotas(dadosGlobaisDashboard);
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
        const ctxStatus = document.getElementById('graficoStatus').getContext('2d');
        new Chart(ctxStatus, {
            type: 'bar',
            data: {
                labels: ['Zerado / Assistido', 'Jogando / Assistindo', 'Backlog', 'Dropado'],
                datasets: [{
                    label: 'Quantidade',
                    data: [dados.totalZerados, dados.totalJogando, dados.totalBacklog, dados.totalDropados],
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
                        ticks: { color: '#7f8c8d' },
                        grid: { color: 'rgba(255,255,255,0.05)' }
                    },
                    x: {
                        ticks: { color: '#7f8c8d' },
                        grid: { display: false }
                    }
                }
            }
        });

        // --- GRÁFICO 3: DISTRIBUIÇÃO DE NOTAS/ESTRELAS ---

        renderizarGraficoNotas(dados);

    } catch (erro) {
        console.error("Erro no dashboard:", erro);
    }
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