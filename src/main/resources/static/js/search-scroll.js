document.addEventListener('DOMContentLoaded', () => {

    // --- LÓGICA DA BARRA DE PESQUISA ---
    const searchBar = document.getElementById('search-bar');
    const containerLista = document.getElementById('lista-itens');
    const msgNaoEncontrado = document.getElementById('msg-nao-encontrado');

    if (searchBar) {
        searchBar.addEventListener('input', (e) => {
            const termo = e.target.value.toLowerCase();
            // Pegamos os cards ATUAIS (pois eles foram carregados via fetch)
            const cards = document.querySelectorAll('.card');
            let visiveis = 0;

            cards.forEach(card => {
                const titulo = card.querySelector('h3').textContent.toLowerCase();

                if (titulo.includes(termo)) {
                    card.style.display = '';
                    visiveis++;
                } else {
                    card.style.display = 'none';
                }
            });

            // Controle da mensagem "Nenhum resultado"
            if (msgNaoEncontrado) {
                if (visiveis === 0 && cards.length > 0) {
                    msgNaoEncontrado.style.display = 'block';
                } else {
                    msgNaoEncontrado.style.display = 'none';
                }
            }
        });
    }

    // --- LÓGICA DO BOTÃO VOLTAR AO TOPO ---
    const btnTopo = document.getElementById('btn-topo');

    if (btnTopo) {
        // Mostrar botão quando rolar a página
        window.addEventListener('scroll', () => {
            if (window.scrollY > 300) {
                btnTopo.classList.add('visivel');
            } else {
                btnTopo.classList.remove('visivel');
            }
        });

        // Ação de clicar e subir suavemente
        btnTopo.addEventListener('click', () => {
            window.scrollTo({
                top: 0,
                behavior: 'smooth'
            });
        });
    }
});