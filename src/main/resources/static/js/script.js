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
        nota: parseInt(nota),
        resenha: resenha,
        imagemUrl: imagemUrl
    };

    try {
        let metodo;
        let url;

        // Decide se cria ou atualiza
        if (idEdicao) {
            // MODO EDIÇÃO (PUT)
            metodo = 'PUT';
            url = `/itens/${idEdicao}`;
        } else {
            // MODO CRIAÇÃO (POST)
            metodo = 'POST';
            url = '/itens';
        }

        const resposta = await fetch(url, {
            method: metodo,
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(dados)
        });

        if (resposta.ok) {
            alert('Salvo com sucesso!');
            window.location.href = '/'; // Volta para a Home
        } else {
            alert('Erro ao salvar. Verifique o console.');
        }

    } catch (erro) {
        console.error('Erro na requisição:', erro);
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

// Função auxiliar para verificar se é uma URL válida e segura
function ehUrlValida(string) {
    try {
        const url = new URL(string);
        // Só aceita http: ou https: (evita javascript:alert(...) e outros ataques)
        return url.protocol === "http:" || url.protocol === "https:";
    } catch (_) {
        return false; // Se der erro ao criar o objeto URL, não é válida
    }
}

function atualizarPreview() {
    const url = document.getElementById('imagemUrl').value;
    const img = document.getElementById('preview-img');

    if (img) {
        if (url && ehUrlValida(url)) {
            img.src = url;
            img.style.display = 'block';
        } else {
            img.style.display = 'none';
            img.src = ''; // Limpa o src para não ficar lixo
        }
    }
}

async function deletarItem(id) {
    const confirmar = confirm("Tem certeza que deseja excluir esse item? Não tem volta!");

    if (confirmar) {
        try {
            const resposta = await fetch(`/itens/${id}`, {
                method: 'DELETE'
            });

            if (resposta.ok) {
                carregarItens();
            } else {
                alert("Erro ao excluir. O servidor não deixou.");
            }
        } catch (erro) {
            console.error("Erro na exclusão:", erro);
        }
    }
}