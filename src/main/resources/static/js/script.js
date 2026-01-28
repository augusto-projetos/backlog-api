// --- Lógica de Inicialização ---

const listaItens = document.getElementById('lista-itens');
const formCadastro = document.getElementById('form-cadastro');

// Variável global para saber se estamos editando
let idEdicao = null;

// 1. Lógica da Home (Lista)
if (listaItens) {
    carregarItens();
}

// 2. Lógica do Cadastro/Edição (Formulário)
if (formCadastro) {
    // Verifica se tem ID na URL (Ex: cadastro.html?id=5)
    const parametros = new URLSearchParams(window.location.search);
    const idUrl = parametros.get('id');

    if (idUrl) {
        idEdicao = idUrl; // Guarda o ID para usar no salvar
        document.querySelector('h1').innerText = "✏️ Editar Item"; // Muda o título visualmente
        carregarDadosParaEdicao(idUrl); // Chama a função que busca os dados e preenche
    }

    // Ouve o clique no botão salvar
    formCadastro.addEventListener('submit', salvarItem);
}

// --- Funções ---

// Busca o item no Java e preenche os inputs
async function carregarDadosParaEdicao(id) {
    try {
        const resposta = await fetch(`/itens/${id}`);
        if (resposta.ok) {
            const item = await resposta.json();

            // Preenche os campos do formulário com o que veio do banco
            document.getElementById('titulo').value = item.titulo;
            document.getElementById('tipo').value = item.tipo;
            document.getElementById('status').value = item.status;
            document.getElementById('nota').value = item.nota;
            document.getElementById('resenha').value = item.resenha;

            // Tratamento especial para imagem (caso venha nulo)
            const campoImagem = document.getElementById('imagemUrl');
            if (campoImagem) {
                campoImagem.value = item.imagemUrl || "";
                atualizarPreview(); // Já mostra a foto carregada
            }
        }
    } catch (erro) {
        console.error("Erro ao carregar dados para edição:", erro);
        alert("Erro ao buscar dados do item.");
    }
}

// Serve tanto para Criar quanto para Editar
async function salvarItem(event) {
    event.preventDefault();

    // Pega os valores
    const titulo = document.getElementById('titulo').value;
    const tipo = document.getElementById('tipo').value;
    const status = document.getElementById('status').value;
    const nota = document.getElementById('nota').value;
    const resenha = document.getElementById('resenha').value;
    const campoImagem = document.getElementById('imagemUrl');
    const imagemUrl = campoImagem ? campoImagem.value : "";

    const dados = {
        titulo: titulo,
        tipo: tipo,
        status: status,
        nota: parseFloat(nota),
        resenha: resenha,
        imagemUrl: imagemUrl
    };

    try {
        let metodo;
        let url;

        if (idEdicao) {
            metodo = 'PUT';
            url = `/itens/${idEdicao}`;
        } else {
            metodo = 'POST';
            url = '/itens';
        }

        const resposta = await fetch(url, {
            method: metodo,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(dados)
        });

        // SE DEU CERTO (200 ou 201)
        if (resposta.ok) {
            Swal.fire({
                title: 'Sucesso!',
                text: 'Item salvo com sucesso!',
                icon: 'success',
                confirmButtonText: 'Ok'
            }).then(() => {
                window.location.href = '/';
            });
        }
        // SE DEU ERRO DE VALIDAÇÃO (400 - Bad Request)
        else if (resposta.status === 400) {
            const erros = await resposta.json(); // Pega o seu JSON de erros

            // Transforma o JSON {"titulo": "erro", "nota": "erro"} em texto HTML
            let mensagemErro = '<ul style="text-align: left;">';
            for (const campo in erros) {
                mensagemErro += `<li><b>${campo}:</b> ${erros[campo]}</li>`;
            }
            mensagemErro += '</ul>';

            Swal.fire({
                title: 'Erro de Validação!',
                html: mensagemErro, // Usamos HTML para mostrar a lista
                icon: 'error',
                confirmButtonText: 'Corrigir'
            });
        }
        // QUALQUER OUTRO ERRO
        else {
            Swal.fire('Erro!', 'Ocorreu um erro inesperado no servidor.', 'error');
        }

    } catch (erro) {
        console.error('Erro:', erro);
        Swal.fire('Erro!', 'Falha na comunicação com o sistema.', 'error');
    }
}

async function carregarItens() {
    try {
        const resposta = await fetch('/itens');
        const itens = await resposta.json();

        listaItens.innerHTML = '';

        itens.forEach(item => {
            const imagem = item.imagemUrl ? item.imagemUrl : 'https://placehold.co/150x200?text=Sem+Imagem';

            const card = `
                <div class="card">
                    <div class="card-img-wrapper">
                        <img src="${imagem}">
                    </div>

                    <div class="card-info">
                        <h3>${item.titulo} <span class="badge">${item.tipo}</span></h3>
                        <p>${item.resenha}</p>
                        <small>Status: ${item.status}</small>
                    </div>

                    <div class="card-actions">
                        <div class="nota">
                            Nota: ${item.nota}/10
                        </div>

                        <div class="btn-group">
                            <a href="/cadastro.html?id=${item.id}" class="btn-edit">
                                ✏️
                            </a>

                            <button onclick="deletarItem(${item.id})" class="btn-delete">
                                🗑️
                            </button>
                        </div>
                    </div>
                </div>
            `;
            listaItens.innerHTML += card;
        });
    } catch (erro) {
        console.error('Erro ao buscar itens:', erro);
    }
}

// Função Sanitizadora: Tenta criar uma URL limpa. Se falhar, devolve null.
function sanitizarUrl(string) {
    try {
        const urlObj = new URL(string);
        // Só aceita http e https
        if (urlObj.protocol === "http:" || urlObj.protocol === "https:") {
            return urlObj.href; // Retorna a URL reconstruída e segura
        }
    } catch (_) {
        // Se der erro, ignora
    }
    return null; // Retorna nulo se não for válida
}

function atualizarPreview() {
    const urlInput = document.getElementById('imagemUrl').value;
    const img = document.getElementById('preview-img');

    // Tenta limpar a URL
    const urlSegura = sanitizarUrl(urlInput);

    if (img) {
        if (urlSegura) {
            // Usamos a variável 'urlSegura', não o input original
            img.src = urlSegura;
            img.style.display = 'block';
        } else {
            img.style.display = 'none';
            img.src = '';
        }
    }
}

async function deletarItem(id) {
    // Pergunta estilizada
    const resultado = await Swal.fire({
        title: 'Tem certeza?',
        text: "Você não poderá reverter isso!",
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#e74c3c', // Vermelho botão delete
        cancelButtonColor: '#7f8c8d',
        confirmButtonText: 'Sim, deletar!',
        cancelButtonText: 'Cancelar'
    });

    // Só deleta se o usuário clicou em "Sim"
    if (resultado.isConfirmed) {
        try {
            const resposta = await fetch(`/itens/${id}`, { method: 'DELETE' });

            if (resposta.ok) {
                // Mostra sucesso rápido e recarrega
                Swal.fire(
                    'Deletado!',
                    'O item foi removido.',
                    'success'
                );
                carregarItens();
            } else {
                Swal.fire('Erro!', 'Não foi possível deletar.', 'error');
            }
        } catch (erro) {
            console.error("Erro:", erro);
        }
    }
}