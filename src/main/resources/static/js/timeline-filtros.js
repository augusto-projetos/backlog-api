// Filtros da Linha do Tempo: busca por título, tipo de ação e data
// específica. Funciona tanto na timeline do usuário quanto na timeline
// do admin
(function () {
    const inputBusca = document.getElementById('filtro-busca');
    const selectTipo = document.getElementById('filtro-tipo');
    const inputData = document.getElementById('filtro-data');
    const btnLimpar = document.getElementById('filtro-limpar');
    const lista = document.getElementById('timeline-lista') || document.getElementById('lista-logs');
    const vazio = document.getElementById('timeline-filtro-vazio');

    // A timeline do usuário mostra o texto inteiro ("N registro(s)
    // encontrado(s)") num <span>; a do admin só troca o número dentro de
    // um <strong>, mantendo o texto ao redor fixo no HTML.
    const contadorTexto = document.getElementById('timeline-contador');
    const contadorNumero = document.getElementById('contador-logs');

    if (!lista || !inputBusca) return; // Nada pra filtrar (sem filtros nesta página, ou timeline vazia)

    const totalRegistrosTexto = contadorTexto ? contadorTexto.textContent : null;
    const totalRegistrosNumero = contadorNumero ? contadorNumero.textContent : null;
    let debounceTimer = null;

    function aplicarFiltros() {
        const busca = (inputBusca?.value || '').trim().toLowerCase();
        const tipo = selectTipo?.value || '';
        const data = inputData?.value || '';
        const filtrando = !!(busca || tipo || data);

        let algumVisivel = false;
        let totalLogsVisiveis = 0;

        lista.querySelectorAll('.timeline-dia').forEach(diaEl => {
            let algumNesteDia = false;
            const diaData = diaEl.getAttribute('data-dia') || '';
            const combinaData = !data || diaData === data;

            diaEl.querySelectorAll(':scope > .timeline-item').forEach(item => {
                const detalhes = item.querySelector('.timeline-grupo-detalhes');
                let visivel;

                if (detalhes) {
                    // Grupo com vários eventos: filtra CADA evento individualmente,
                    // e só esconde o card inteiro se nenhum evento bater.
                    let algumEventoBate = false;
                    const eventos = detalhes.querySelectorAll('.timeline-grupo-evento');
                    eventos.forEach(evento => {
                        const tipoEvento = evento.getAttribute('data-tipo') || '';
                        const textoEvento = evento.getAttribute('data-busca') || '';
                        const bate = combinaData
                            && (!tipo || tipoEvento === tipo)
                            && (!busca || textoEvento.includes(busca));
                        evento.style.display = bate ? '' : 'none';
                        if (bate) { algumEventoBate = true; totalLogsVisiveis++; }
                    });

                    visivel = algumEventoBate;

                    // Enquanto estiver filtrando, expande automaticamente os grupos
                    // que têm eventos batendo, pra não esconder o resultado dentro
                    // de um card recolhido. Sem filtro, volta ao estado recolhido.
                    const toggle = item.querySelector('.timeline-grupo-toggle');
                    if (filtrando && algumEventoBate) {
                        detalhes.classList.add('aberto');
                        toggle?.setAttribute('aria-expanded', 'true');
                    } else if (!filtrando) {
                        detalhes.classList.remove('aberto');
                        toggle?.setAttribute('aria-expanded', 'false');
                    }

                    atualizarBadgeGrupo(item, filtrando, eventos.length, algumEventoBate ?
                        Array.from(eventos).filter(e => e.style.display !== 'none').length : 0);
                } else {
                    const tiposDoItem = (item.getAttribute('data-tipos') || '').split(',');
                    const textoDoItem = item.getAttribute('data-busca') || '';
                    visivel = combinaData
                        && (!tipo || tiposDoItem.includes(tipo))
                        && (!busca || textoDoItem.includes(busca));
                    if (visivel) totalLogsVisiveis++;
                }

                item.style.display = visivel ? '' : 'none';
                if (visivel) algumNesteDia = true;
            });

            diaEl.style.display = algumNesteDia ? '' : 'none';
            if (algumNesteDia) algumVisivel = true;
        });

        if (vazio) vazio.style.display = algumVisivel ? 'none' : 'block';
        lista.style.display = algumVisivel ? '' : 'none';

        atualizarContador(filtrando, totalLogsVisiveis, algumVisivel);
        atualizarLinhasVerticais();
    }

    // Atualiza o texto do badge "N atualizações" do grupo pra refletir
    // quantos eventos batem com o filtro (ex: "2 de 5 atualizações").
    function atualizarBadgeGrupo(item, filtrando, total, visiveis) {
        const badge = item.querySelector('.timeline-card-header .timeline-badge');
        if (!badge) return;
        if (!badge.dataset.original) badge.dataset.original = badge.textContent;

        badge.textContent = filtrando
            ? `${visiveis} de ${total} atualizações`
            : badge.dataset.original;
    }

    // Ajusta a "linha vertical" da timeline pra não sobrar linha pendurada
    // depois do último item visível de cada dia.
    function atualizarLinhasVerticais() {
        lista.querySelectorAll('.timeline-dia').forEach(diaEl => {
            const itensVisiveis = Array.from(diaEl.querySelectorAll(':scope > .timeline-item'))
                .filter(el => el.style.display !== 'none');
            itensVisiveis.forEach((item, idx) => {
                const linha = item.querySelector('.timeline-line');
                if (!linha) return;
                linha.style.display = (idx === itensVisiveis.length - 1) ? 'none' : '';
            });
        });
    }

    function atualizarContador(filtrando, totalLogsVisiveis, algumVisivel) {
        if (contadorTexto) {
            contadorTexto.textContent = !filtrando
                ? totalRegistrosTexto
                : (algumVisivel ? totalLogsVisiveis + ' resultado(s) encontrado(s)' : '0 resultado(s) encontrado(s)');
        }
        if (contadorNumero) {
            contadorNumero.textContent = !filtrando ? totalRegistrosNumero : totalLogsVisiveis;
        }
    }

    function limparFiltros() {
        if (inputBusca) inputBusca.value = '';
        if (selectTipo) selectTipo.value = '';
        if (inputData) inputData.value = '';
        aplicarFiltros();
    }

    inputBusca?.addEventListener('input', () => {
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(aplicarFiltros, 200);
    });
    selectTipo?.addEventListener('change', aplicarFiltros);
    inputData?.addEventListener('change', aplicarFiltros);
    btnLimpar?.addEventListener('click', limparFiltros);
})();
