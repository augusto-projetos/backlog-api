document.addEventListener('DOMContentLoaded', () => {
    const ratingToggleBtn = document.getElementById('rating-toggle');
    const ratingIcon = document.getElementById('rating-icon');

    // Verifica o localStorage (o padrão será 'nota')
    let currentRatingMode = localStorage.getItem('ratingMode') || 'nota';

    // Atualiza a interface assim que a página carrega
    updateRatingUI(currentRatingMode);

    ratingToggleBtn.addEventListener('click', () => {
        // Alterna o modo
        currentRatingMode = currentRatingMode === 'nota' ? 'estrela' : 'nota';

        // Salva a nova preferência no navegador
        localStorage.setItem('ratingMode', currentRatingMode);

        // Atualiza o botão
        updateRatingUI(currentRatingMode);

        // Dispara um evento para que o script dos cards saiba que deve atualizar a tela
        window.dispatchEvent(new Event('ratingModeChanged'));
    });

    function updateRatingUI(mode) {
        if (mode === 'estrela') {
            ratingIcon.textContent = '🔟';
            ratingToggleBtn.title = "Mudar para visualização por Notas";
        } else {
            ratingIcon.textContent = '⭐';
            ratingToggleBtn.title = "Mudar para visualização por Estrelas";
        }
    }
});
