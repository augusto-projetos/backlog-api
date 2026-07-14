// Expande/recolhe um bloco de eventos agrupados na Linha do Tempo
// (vários registros que aconteceram na mesma ação do usuário).
function toggleGrupoTimeline(botao) {
    const card = botao.closest('.timeline-card');
    if (!card) return;

    const detalhes = card.querySelector('.timeline-grupo-detalhes');
    if (!detalhes) return;

    const aberto = detalhes.classList.toggle('aberto');
    botao.setAttribute('aria-expanded', aberto ? 'true' : 'false');
}
