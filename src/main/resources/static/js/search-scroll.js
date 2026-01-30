document.addEventListener('DOMContentLoaded', () => {

    // Elementos
    const searchBar = document.getElementById('search-bar');
    const filterSelect = document.getElementById('filter-status');
    const msgNaoEncontrado = document.getElementById('msg-nao-encontrado');

    // --- FUNÇÃO MESTRA DE FILTRAGEM ---
    function aplicarFiltros() {
        const termo = searchBar.value.toLowerCase();
        const statusSelecionado = filterSelect.value;

        // Pega todos os cards da tela
        const cards = document.querySelectorAll('.card');
        let visiveis = 0;

        cards.forEach(card => {
            // 1. Pega os dados do Card
            const titulo = card.querySelector('h3').textContent.toLowerCase();
            // Pega o texto do status (ex: "Status: Zerado") e limpa ele
            const statusTexto = card.querySelector('small').textContent.toLowerCase();

            // 2. Verifica a PESQUISA (Nome)
            const matchPesquisa = titulo.includes(termo);

            // 3. Verifica o SELECT (Status)
            let matchFiltro = true; // Assume que passou, a não ser que falhe abaixo

            if (statusSelecionado !== 'todos') {
                if (statusSelecionado === 'concluido') {
                    // Aceita tanto "Zerado" quanto "Assistido"
                    matchFiltro = statusTexto.includes('zerado') || statusTexto.includes('assistido');
                }
                else if (statusSelecionado === 'andamento') {
                    // Aceita tanto "Jogando" quanto "Assistindo"
                    matchFiltro = statusTexto.includes('jogando') || statusTexto.includes('assistindo');
                }
                else {
                    // Para Backlog e Dropado, busca direto a palavra
                    matchFiltro = statusTexto.includes(statusSelecionado);
                }
            }

            // 4. Decisão Final: Tem que passar nos DOIS testes
            if (matchPesquisa && matchFiltro) {
                card.style.display = ''; // Mostra (deixa o CSS decidir se é flex ou block)
                visiveis++;
            } else {
                card.style.display = 'none'; // Esconde
            }
        });

        // Controle da mensagem de "Nada encontrado"
        if (msgNaoEncontrado) {
            // Só mostra a mensagem se a lista NÃO estava vazia originalmente (tem cards) mas o filtro escondeu tudo
            if (visiveis === 0 && cards.length > 0) {
                msgNaoEncontrado.style.display = 'block';
            } else {
                msgNaoEncontrado.style.display = 'none';
            }
        }
    }

    // --- EVENTOS ---

    // 1. Quando digitar na busca
    if (searchBar) {
        searchBar.addEventListener('input', aplicarFiltros);
    }

    // 2. Quando mudar o select
    if (filterSelect) {
        filterSelect.addEventListener('change', aplicarFiltros);
    }

    // --- LÓGICA DO BOTÃO VOLTAR AO TOPO  ---
    const btnTopo = document.getElementById('btn-topo');
    if (btnTopo) {
        window.addEventListener('scroll', () => {
            if (window.scrollY > 300) {
                btnTopo.classList.add('visivel');
            } else {
                btnTopo.classList.remove('visivel');
            }
        });

        btnTopo.addEventListener('click', () => {
            window.scrollTo({ top: 0, behavior: 'smooth' });
        });
    }
});