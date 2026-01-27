// Essa função roda assim que a página abre
async function carregarItens() {
    try {
        // 1. Chama o seu Back-end Java
        const resposta = await fetch('/itens');

        // 2. Converte o JSON que veio do Java
        const itens = await resposta.json();

        // 3. Limpa a tela e desenha cada item
        const listaDiv = document.getElementById('lista-itens');
        listaDiv.innerHTML = ''; // Limpa o "Carregando..."

        itens.forEach(item => {
            // Cria o HTML de cada cartão
            const card = `
                <div class="card">
                    <div class="info">
                        <h3>${item.titulo} <span class="badge">${item.tipo}</span></h3>
                        <p>${item.resenha}</p>
                        <small>Status: ${item.status}</small>
                    </div>
                    <div class="nota">
                        Nota: ${item.nota}/10
                    </div>
                </div>
            `;
            listaDiv.innerHTML += card;
        });
    } catch (erro) {
        console.error('Erro ao buscar itens:', erro);
    }
}

// Chama a função
carregarItens();