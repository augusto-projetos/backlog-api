// Verifica se estamos na página inicial (tem a lista?)
const listaItens = document.getElementById('lista-itens');
if (listaItens) {
    carregarItens();
}

// Verifica se estamos na página de cadastro (tem o formulário?)
const formCadastro = document.getElementById('form-cadastro');
if (formCadastro) {
    formCadastro.addEventListener('submit', cadastrarItem);
}

// --- Funções ---

async function carregarItens() {
    try {
        const resposta = await fetch('/itens');
        const itens = await resposta.json();

        listaItens.innerHTML = '';

        itens.forEach(item => {
            // Define uma imagem padrão caso o item não tenha link
            const imagem = item.imagemUrl ? item.imagemUrl : 'https://placehold.co/150x200?text=Sem+Imagem';

            const card = `
                <div class="card">
                    <div style="margin-right: 15px;">
                        <img src="${imagem}" style="width: 100px; height: 140px; object-fit: cover; border-radius: 4px;">
                    </div>

                    <div class="info" style="flex: 1;">
                        <h3>${item.titulo} <span class="badge">${item.tipo}</span></h3>
                        <p>${item.resenha}</p>
                        <small>Status: ${item.status}</small>
                    </div>

                    <div style="display: flex; flex-direction: column; align-items: flex-end;">
                        <div class="nota" style="margin-bottom: 10px;">
                            Nota: ${item.nota}/10
                        </div>
                        <button onclick="deletarItem(${item.id})" class="btn-delete">
                            🗑️
                        </button>
                    </div>
                </div>
            `;
            listaItens.innerHTML += card;
        });
    } catch (erro) {
        console.error('Erro ao buscar itens:', erro);
    }
}

async function cadastrarItem(event) {
    // 1. Evita que a página recarregue sozinha (comportamento padrão do form)
    event.preventDefault();

    // 2. Pega os valores dos inputs
    const titulo = document.getElementById('titulo').value;
    const tipo = document.getElementById('tipo').value;
    const status = document.getElementById('status').value;
    const nota = document.getElementById('nota').value;
    const resenha = document.getElementById('resenha').value;
    const campoImagem = document.getElementById('imagemUrl');
    const imagemUrl = campoImagem ? campoImagem.value : "";

    // 3. Monta o JSON
    const dados = {
        titulo: titulo,
        tipo: tipo,
        status: status,
        nota: parseInt(nota), // Converte texto para número
        resenha: resenha,
        imagemUrl: imagemUrl
    };

    try {
        // 4. Envia pro Java via POST
        const resposta = await fetch('/itens', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(dados)
        });

        if (resposta.ok) {
            alert('Item cadastrado com sucesso!');
            window.location.href = '/'; // Volta para a página inicial
        } else {
            alert('Erro ao cadastrar. Verifique o console.');
        }

    } catch (erro) {
        console.error('Erro na requisição:', erro);
    }
}

function atualizarPreview() {
    const url = document.getElementById('imagemUrl').value;
    const img = document.getElementById('preview-img');

    if (url) {
        img.src = url;
        img.style.display = 'block'; // Mostra a imagem
    } else {
        img.style.display = 'none';  // Esconde se estiver vazio
        img.src = '';
    }
}

async function deletarItem(id) {
    // 1. Pergunta de segurança (UX básica)
    const confirmar = confirm("Tem certeza que deseja excluir esse item? Não tem volta!");

    if (confirmar) {
        try {
            // 2. Chama o DELETE do Java
            const resposta = await fetch(`/itens/${id}`, {
                method: 'DELETE'
            });

            if (resposta.ok) {
                // 3. Se deu certo, recarrega a lista para o item sumir
                carregarItens();
            } else {
                alert("Erro ao excluir. O servidor não deixou.");
            }
        } catch (erro) {
            console.error("Erro na exclusão:", erro);
        }
    }
}