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

    // --- PERSISTÊNCIA DOS FILTROS ---
    const FILTRO_STORAGE_KEY = 'backlog_filtros_' + window.location.pathname;

    function salvarFiltros() {
        try {
            sessionStorage.setItem(FILTRO_STORAGE_KEY, JSON.stringify({
                termo: searchBar ? searchBar.value : '',
                status: filterStatus ? filterStatus.value : 'todos',
                tipo: filterType ? filterType.value : 'todos',
                ordem: orderSelect ? orderSelect.value : 'padrao'
            }));
        } catch (_) {
            // sessionStorage indisponível (modo privado etc.) - ignora silenciosamente
        }
    }

    function restaurarFiltros() {
        let salvo = null;
        try {
            salvo = JSON.parse(sessionStorage.getItem(FILTRO_STORAGE_KEY));
        } catch (_) {
            salvo = null;
        }
        if (!salvo) return false;

        if (searchBar) searchBar.value = salvo.termo || '';
        if (filterStatus) filterStatus.value = salvo.status || 'todos';
        if (filterType) filterType.value = salvo.tipo || 'todos';
        if (orderSelect) orderSelect.value = salvo.ordem || 'padrao';

        // Se algum filtro estiver ativo, abre a gaveta de filtros para deixar claro
        // pro usuário que a lista está filtrada (evita achar que "sumiram" itens).
        const temFiltroAtivo = (salvo.termo && salvo.termo.length > 0)
            || salvo.status !== 'todos' || salvo.tipo !== 'todos' || salvo.ordem !== 'padrao';
        if (temFiltroAtivo && filterMenu && btnToggle) {
            filterMenu.classList.add('aberto');
            const icon = btnToggle.querySelector('i');
            if (icon) {
                icon.classList.remove('fa-chevron-down');
                icon.classList.add('fa-chevron-up');
            }
        }

        return true;
    }

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
            } else if (ordem === 'tempo-maior' || ordem === 'tempo-menor') {
                // Só Filmes (duração) e Jogos (horas jogadas) têm tempo cadastrado.
                // Itens sem tempo (ex.: Séries, ou Filmes/Jogos sem duração informada) ficam sempre no final, independente da direção escolhida.
                const tempoAttrA = a.getAttribute('data-tempo');
                const tempoAttrB = b.getAttribute('data-tempo');
                const tempoA = (tempoAttrA === '' || tempoAttrA === null) ? null : parseInt(tempoAttrA, 10);
                const tempoB = (tempoAttrB === '' || tempoAttrB === null) ? null : parseInt(tempoAttrB, 10);

                if (tempoA === null && tempoB === null) return 0;
                if (tempoA === null) return 1;
                if (tempoB === null) return -1;

                return ordem === 'tempo-maior' ? tempoB - tempoA : tempoA - tempoB;
            } else {
                // Buscamos o valor numérico puro direto do atributo oculto 'data-nota'
                const notaA = parseFloat(a.querySelector('.nota').getAttribute('data-nota')) || 0;
                const notaB = parseFloat(b.querySelector('.nota').getAttribute('data-nota')) || 0;

                // Se o select for 'maior', faz Maior -> Menor (B - A). Se não, Menor -> Maior (A - B)
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

            // 4. Limpa o filtro salvo
            try { sessionStorage.removeItem(FILTRO_STORAGE_KEY); } catch (_) {}
        });
    }

    // --- EVENTOS ---
    if (searchBar) searchBar.addEventListener('input', () => { salvarFiltros(); aplicarFiltros(); });
    if (filterStatus) filterStatus.addEventListener('change', () => { salvarFiltros(); aplicarFiltros(); });
    if (filterType) filterType.addEventListener('change', () => { salvarFiltros(); aplicarFiltros(); });
    if (orderSelect) orderSelect.addEventListener('change', () => { salvarFiltros(); ordenarItens(); });

    // --- RESTAURA E APLICA OS FILTROS SALVOS ---
    restaurarFiltros();

    // Sempre que a lista for (re)carregada - seja no load inicial da home
    // ou depois de deletar um item - reaplicamos o que está selecionado nos 
    // filtros agora, e não só o que foi restaurado do sessionStorage.
    document.addEventListener('itensCarregados', () => {
        ordenarItens();
    });

    // Em páginas renderizadas direto pelo servidor (ex.: /u/{username}), os
    // cards já existem no DOM neste ponto, então aplicamos de imediato.
    if (document.querySelectorAll('.card').length > 0) {
        ordenarItens();
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
