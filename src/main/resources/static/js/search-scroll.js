document.addEventListener('DOMContentLoaded', () => {

    // --- ELEMENTOS ---
    const btnToggle = document.getElementById('btn-filtros-toggle');
    const filterMenu = document.getElementById('filter-menu');

    const searchBar = document.getElementById('search-bar');
    const filterStatus = document.getElementById('filter-status');
    const filterType = document.getElementById('filter-type');
    const orderSelect = document.getElementById('filter-order');
    const btnLimpar = document.getElementById('btn-limpar-filtros');

    const msgNaoEncontrado = document.getElementById('msg-nao-encontrado');
    const listaItens = document.getElementById('lista-itens');

    // --- LÓGICA DE ABRIR/FECHAR A GAVETA ---
    if (btnToggle && filterMenu) {
        btnToggle.addEventListener('click', () => {
            filterMenu.classList.toggle('aberto');

            // Troca o ícone da seta
            const icon = btnToggle.querySelector('i');
            if (icon) {
                icon.classList.toggle('fa-chevron-down');
                icon.classList.toggle('fa-chevron-up');
            }
        });
    }

    // Fechar a gaveta se clicar fora dela
    document.addEventListener('click', (event) => {
        if (!filterMenu || !btnToggle) return;

        const isClickInside = filterMenu.contains(event.target) || btnToggle.contains(event.target);

        if (!isClickInside && filterMenu.classList.contains('aberto')) {
            filterMenu.classList.remove('aberto');
            const icon = btnToggle.querySelector('i');
            if (icon) {
                icon.classList.replace('fa-chevron-up', 'fa-chevron-down');
            }
        }
    });

    // --- FUNÇÃO DE ORDENAÇÃO ---
    function ordenarItens() {
        const ordem = orderSelect ? orderSelect.value : 'padrao';
        const cards = Array.from(document.querySelectorAll('.card'));

        cards.sort((a, b) => {
            if (ordem === 'padrao') {
                const tituloA = a.querySelector('h3').textContent.trim();
                const tituloB = b.querySelector('h3').textContent.trim();
                return tituloA.localeCompare(tituloB);
            } else {
                const textoNotaA = a.querySelector('.nota').textContent;
                const textoNotaB = b.querySelector('.nota').textContent;

                const getNota = (str) => {
                    const match = str.match(/(\d+(\.\d+)?)/);
                    return match ? parseFloat(match[0]) : 0;
                };

                const notaA = getNota(textoNotaA);
                const notaB = getNota(textoNotaB);

                return ordem === 'maior' ? notaB - notaA : notaA - notaB;
            }
        });

        // Reinsere os cards ordenados
        cards.forEach(card => listaItens.appendChild(card));
        // Reaplica os filtros para manter oculto o que deve estar oculto
        aplicarFiltros();
    }

    // --- FUNÇÃO MESTRA DE FILTRAGEM ---
    function aplicarFiltros() {
        const termo = searchBar ? searchBar.value.toLowerCase() : "";
        const statusSelecionado = filterStatus ? filterStatus.value : "todos";
        const tipoSelecionado = filterType ? filterType.value : "todos";

        const cards = document.querySelectorAll('.card');
        let visiveis = 0;

        cards.forEach(card => {
            const titulo = card.querySelector('h3').textContent.toLowerCase();
            const statusTexto = card.querySelector('small').textContent.toLowerCase();

            // Pega o tipo da Badge (Jogo, Filme, Série) e limpa espaços
            const badgeTipo = card.querySelector('.badge');
            const tipoTexto = badgeTipo ? badgeTipo.textContent.trim() : "";

            // 1. Filtro de Nome
            const matchPesquisa = titulo.includes(termo);

            // 2. Filtro de Tipo
            const matchTipo = (tipoSelecionado === 'todos' || tipoTexto === tipoSelecionado);

            // 3. Filtro de Status
            let matchStatus = true;
            if (statusSelecionado !== 'todos') {
                if (statusSelecionado === 'concluido') {
                    matchStatus = statusTexto.includes('zerado') || statusTexto.includes('assistido');
                } else if (statusSelecionado === 'andamento') {
                    matchStatus = statusTexto.includes('jogando') || statusTexto.includes('assistindo');
                } else {
                    matchStatus = statusTexto.includes(statusSelecionado);
                }
            }

            // --- RESULTADO FINAL ---
            if (matchPesquisa && matchStatus && matchTipo) {
                card.style.display = '';
                visiveis++;
            } else {
                card.style.display = 'none';
            }
        });

        // Mensagem de "Nenhum item encontrado"
        if (msgNaoEncontrado) {
            msgNaoEncontrado.style.display = (visiveis === 0 && cards.length > 0) ? 'block' : 'none';
        }
    }

    // --- BOTÃO LIMPAR FILTROS ---
    if (btnLimpar) {
        btnLimpar.addEventListener('click', () => {
            // 1. Reseta os valores dos inputs
            if (searchBar) searchBar.value = '';
            if (filterStatus) filterStatus.value = 'todos';
            if (filterType) filterType.value = 'todos';
            if (orderSelect) orderSelect.value = 'padrao';

            // 2. Reaplica a filtragem (para mostrar tudo de novo)
            aplicarFiltros();

            // 3. Reaplica a ordenação padrão (A-Z)
            ordenarItens();
        });
    }

    // --- EVENTOS ---
    if (searchBar) searchBar.addEventListener('input', aplicarFiltros);
    if (filterStatus) filterStatus.addEventListener('change', aplicarFiltros);
    if (filterType) filterType.addEventListener('change', aplicarFiltros);
    if (orderSelect) orderSelect.addEventListener('change', ordenarItens);

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