document.addEventListener('DOMContentLoaded', () => {

    // Elementos
    const searchBar = document.getElementById('search-bar');
    const filterSelect = document.getElementById('filter-status');
    const orderSelect = document.getElementById('filter-order'); // Novo elemento
    const msgNaoEncontrado = document.getElementById('msg-nao-encontrado');
    const listaItens = document.getElementById('lista-itens');

    // --- FUNÇÃO DE ORDENAÇÃO ---
    function ordenarItens() {
        const ordem = orderSelect ? orderSelect.value : 'padrao';
        const cards = Array.from(document.querySelectorAll('.card'));

        cards.sort((a, b) => {
            if (ordem === 'padrao') {
                // Ordena por Título (A-Z)
                const tituloA = a.querySelector('h3').textContent.trim();
                const tituloB = b.querySelector('h3').textContent.trim();
                return tituloA.localeCompare(tituloB);
            } else {
                // Pega o texto da nota (Ex: "Nota: 9.5/10")
                const textoNotaA = a.querySelector('.nota').textContent;
                const textoNotaB = b.querySelector('.nota').textContent;

                // Extrai apenas o número
                const getNota = (str) => {
                    const match = str.match(/(\d+(\.\d+)?)/);
                    return match ? parseFloat(match[0]) : 0;
                };

                const notaA = getNota(textoNotaA);
                const notaB = getNota(textoNotaB);

                // Maior -> Menor ou Menor -> Maior
                return ordem === 'maior' ? notaB - notaA : notaA - notaB;
            }
        });

        // Reinsere os cards na ordem certa
        cards.forEach(card => listaItens.appendChild(card));

        // Reaplica os filtros para garantir que itens ocultos continuem ocultos
        aplicarFiltros();
    }

    // --- FUNÇÃO MESTRA DE FILTRAGEM ---
    function aplicarFiltros() {
        const termo = searchBar.value.toLowerCase();
        const statusSelecionado = filterSelect.value;
        const cards = document.querySelectorAll('.card');
        let visiveis = 0;

        cards.forEach(card => {
            const titulo = card.querySelector('h3').textContent.toLowerCase();
            const statusTexto = card.querySelector('small').textContent.toLowerCase();

            const matchPesquisa = titulo.includes(termo);
            let matchFiltro = true;

            if (statusSelecionado !== 'todos') {
                if (statusSelecionado === 'concluido') {
                    matchFiltro = statusTexto.includes('zerado') || statusTexto.includes('assistido');
                } else if (statusSelecionado === 'andamento') {
                    matchFiltro = statusTexto.includes('jogando') || statusTexto.includes('assistindo');
                } else {
                    matchFiltro = statusTexto.includes(statusSelecionado);
                }
            }

            if (matchPesquisa && matchFiltro) {
                card.style.display = '';
                visiveis++;
            } else {
                card.style.display = 'none';
            }
        });

        if (msgNaoEncontrado) {
            msgNaoEncontrado.style.display = (visiveis === 0 && cards.length > 0) ? 'block' : 'none';
        }
    }

    // --- EVENTOS ---
    if (searchBar) searchBar.addEventListener('input', aplicarFiltros);
    if (filterSelect) filterSelect.addEventListener('change', aplicarFiltros);

    // Novo evento de ordenação
    if (orderSelect) {
        orderSelect.addEventListener('change', ordenarItens);
    }

    // --- BOTÃO VOLTAR AO TOPO ---
    const btnTopo = document.getElementById('btn-topo');
    if (btnTopo) {
        window.addEventListener('scroll', () => {
            btnTopo.classList.toggle('visivel', window.scrollY > 300);
        });
        btnTopo.addEventListener('click', () => {
            window.scrollTo({ top: 0, behavior: 'smooth' });
        });
    }
});