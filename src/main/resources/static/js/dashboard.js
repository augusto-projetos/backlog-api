document.addEventListener('DOMContentLoaded', () => {
    carregarDashboard();
});

async function carregarDashboard() {
    try {
        // 1. Busca os dados no seu Backend
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
            type: 'doughnut', // Tipo "Rosquinha"
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
                        labels: { color: '#7f8c8d' } // Cor da legenda
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
                        '#2ecc71', // Verde (Sucesso)
                        '#3498db', // Azul (Andamento)
                        '#95a5a6', // Cinza (Fila)
                        '#e74c3c'  // Vermelho (Drop)
                    ],
                    borderRadius: 5
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { display: false } // Esconde legenda pois já tem label embaixo
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        ticks: { color: '#7f8c8d' },
                        grid: { color: 'rgba(255,255,255,0.05)' } // Linhas sutis
                    },
                    x: {
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