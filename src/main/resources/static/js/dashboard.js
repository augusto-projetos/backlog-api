document.addEventListener('DOMContentLoaded', () => {
    carregarDashboard();
});

async function carregarDashboard() {
    try {
        // 1. Busca os dados no Backend
        const response = await fetch('/api/dashboard');
        if (!response.ok) throw new Error('Erro ao buscar dados');

        const dados = await response.json();

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

        // --- GRÁFICO 3: NOTAS ---

        // 1. Convertemos o Objeto em um Array de pares [ ["10", 1], ["9.5", 2] ]
        const entradasNotas = Object.entries(dados.notas);

        // 2. Ordenamos esse array na força bruta (Maior -> Menor)
        entradasNotas.sort((a, b) => parseFloat(b[0]) - parseFloat(a[0]));

        // 3. Separamos de volta para usar no gráfico
        const labelsNotas = entradasNotas.map(e => e[0]);  // ["10", "9.5", "8", ...]
        const valoresNotas = entradasNotas.map(e => e[1]); // [1, 2, 5, ...]

        // 4. Lógica de Cores (Agora aplicada na lista já ordenada)
        const coresNotas = labelsNotas.map(notaStr => {
            const nota = parseFloat(notaStr);
            if (nota >= 9) return '#2ecc71';      // Verde (Excelente)
            if (nota >= 7) return '#3498db';      // Azul (Bom)
            if (nota >= 5) return '#f1c40f';      // Amarelo (Médio)
            return '#e74c3c';                     // Vermelho (Ruim)
        });

        const ctxNotas = document.getElementById('graficoNotas').getContext('2d');
        new Chart(ctxNotas, {
            type: 'bar',
            data: {
                labels: labelsNotas,
                datasets: [{
                    label: 'Itens',
                    data: valoresNotas,
                    backgroundColor: coresNotas,
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
                                return context.raw + ' itens com nota ' + context.label;
                            }
                        }
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        title: {
                            display: true,
                            text: 'Quantidade',
                            color: '#7f8c8d',
                            font: { size: 12, weight: 'bold' }
                        },
                        ticks: { color: '#7f8c8d', stepSize: 1 },
                        grid: { color: 'rgba(255,255,255,0.05)' }
                    },
                    x: {
                        title: {
                            display: true,
                            text: 'Notas ( 10 a 0 )',
                            color: '#7f8c8d',
                            font: { size: 12, weight: 'bold' }
                        },
                        ticks: { color: '#7f8c8d' },
                        grid: { display: false }
                    }
                }
            }
        });

    } catch (erro) {
        console.error("Erro no dashboard:", erro);
    }
}