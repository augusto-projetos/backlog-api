// --- Lógica de Inicialização ---

const listaItens = document.getElementById('lista-itens');
const formCadastro = document.getElementById('form-cadastro');

// Variável global para saber se estamos editando
let idEdicao = null;

// 1. Lógica da Home (Lista)
if (listaItens && !window.location.pathname.includes('/share')) {
    carregarItens();
}

// 2. Lógica do Cadastro/Edição (Formulário)
if (formCadastro) {
    // Verifica se tem ID na URL (Ex: cadastro?id=5)
    const parametros = new URLSearchParams(window.location.search);
    const idUrl = parametros.get('id');

    if (idUrl) {
        idEdicao = idUrl; // Guarda o ID para usar no salvar
        document.querySelector('h1').innerText = "✏️ Editar Item"; // Muda o título visualmente
        carregarDadosParaEdicao(idUrl); // Chama a função que busca os dados e preenche
    }

    atualizarStatusDinamico();

    // Ouve o clique no botão salvar
    formCadastro.addEventListener('submit', salvarItem);
}

// Troca o texto do Status baseado no Tipo
function atualizarStatusDinamico() {
    const tipo = document.getElementById('tipo').value;
    const optConcluido = document.getElementById('opt-concluido');
    const optAndamento = document.getElementById('opt-andamento');

    if (!optConcluido || !optAndamento) return;

    if (tipo === 'Jogo') {
        // Modo Gamer 🎮
        optConcluido.innerText = "Zerado";
        optConcluido.value = "Zerado";

        optAndamento.innerText = "Jogando";
        optAndamento.value = "Jogando";
    } else {
        // Modo Cinéfilo 🍿 (Filme ou Série)
        optConcluido.innerText = "Assistido";
        optConcluido.value = "Assistido";

        optAndamento.innerText = "Assistindo";
        optAndamento.value = "Assistindo";
    }
}

// --- Funções ---

// Função auxiliar para pegar o token CSRF das metatags
function getCsrfHeaders() {
    const tokenMeta = document.querySelector('meta[name="_csrf"]');
    const headerMeta = document.querySelector('meta[name="_csrf_header"]');

    // Segurança: Se não achar as metatags (ex: página estática), retorna só o JSON
    if (!tokenMeta || !headerMeta) {
        console.warn("CSRF Tokens não encontrados. Verifique o <head> do HTML.");
        return { 'Content-Type': 'application/json' };
    }

    const token = tokenMeta.getAttribute('content');
    const header = headerMeta.getAttribute('content');

    // Retorna o cabeçalho pronto: { "X-CSRF-TOKEN": "valor-do-token", ... }
    return {
        'Content-Type': 'application/json',
        [header]: token
    };
}

// Busca o item no Java e preenche os inputs
async function carregarDadosParaEdicao(id) {
    try {
        const resposta = await fetch(`/itens/${id}`);
        if (resposta.ok) {
            const item = await resposta.json();

            // Preenche os campos do formulário com o que veio do banco
            document.getElementById('titulo').value = item.titulo;
            document.getElementById('tipo').value = item.tipo;
            atualizarStatusDinamico(); // Chamamos a função para trocar os nomes (Zerado <-> Assistido)
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
            headers: getCsrfHeaders(),
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
                window.location.href = '/home';
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
                html: mensagemErro,
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

        // --- LISTA VAZIA ---
        if (itens.length === 0) {
            // Forçamos o container a virar FLEXBOX centralizado.
            // Isso sobrescreve o "display: grid" do CSS temporariamente.
            listaItens.style.display = 'flex';
            listaItens.style.flexDirection = 'column';
            listaItens.style.alignItems = 'center';
            listaItens.style.justifyContent = 'center';
            listaItens.style.width = '100%'; // Garante largura total

            listaItens.innerHTML = `
                <div style="text-align: center; color: #7f8c8d; padding: 20px;">
                    <p style="font-size: 4rem; margin: 0;">📭</p>
                    <h3>Sua coleção está vazia!</h3>
                    <p>Que tal adicionar aquele jogo ou filme favorito agora?</p>
                    <a href="/cadastro" class="btn-novo" style="display: inline-block; margin-top: 15px; background-color: #e94560; color: white; padding: 10px 20px; border-radius: 50px; text-decoration: none;">
                        Começar Agora
                    </a>
                </div>
            `;
            return; // Para a função aqui
        }

        // --- TEM ITENS (Volta ao Normal) ---
        // Importante: Limpamos os estilos inline para o CSS (style.css) assumir o controle de novo
        listaItens.style.display = '';
        listaItens.style.flexDirection = '';
        listaItens.style.alignItems = '';
        listaItens.style.justifyContent = '';
        listaItens.style.width = '';

        // Ordena alfabeticamente
        itens.sort((a, b) => a.titulo.localeCompare(b.titulo));

        itens.forEach(item => {
            const imagem = item.imagemUrl ? item.imagemUrl : 'https://placehold.co/150x200?text=Sem+Imagem';

            const card = `
                <div class="card">
                    <div class="card-img-wrapper">
                        <img src="${imagem}">
                    </div>

                    <div class="card-info">
                        <h3>${item.titulo} <span class="badge">${item.tipo}</span></h3>

                        <div class="resenha-wrapper">
                            <p class="resenha-texto" id="resenha-${item.id}">${item.resenha || "Sem resenha."}</p>

                            <button class="btn-ver-mais" id="btn-ver-${item.id}" onclick="alternarResenha(${item.id})">
                                ... Ver mais
                            </button>
                        </div>

                        <small>Status: ${item.status}</small>
                    </div>

                    <div class="card-actions">
                        <div class="nota">
                            Nota: ${item.nota}/10
                        </div>

                        <div class="btn-group">
                            <a href="/cadastro?id=${item.id}" class="btn-edit">
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

        verificarTamanhoResenhas();

    } catch (erro) {
        console.error('Erro ao buscar itens:', erro);
        listaItens.innerHTML = '<p style="text-align:center; color:red">Erro ao carregar itens.</p>';
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
            const resposta = await fetch(`/itens/${id}`, {
                method: 'DELETE',
                headers: getCsrfHeaders()
            });

            if (resposta.ok) {
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

// --- FUNÇÕES PARA A RESENHA ---

function alternarResenha(id) {
    const texto = document.getElementById(`resenha-${id}`);
    const botao = document.getElementById(`btn-ver-${id}`);

    // Adiciona/Remove a classe 'expandido' que tira o limite de linhas
    texto.classList.toggle('expandido');

    // Muda o texto do botão
    if (texto.classList.contains('expandido')) {
        botao.innerText = "Ver menos";
    } else {
        botao.innerText = "... Ver mais";
    }
}

function verificarTamanhoResenhas() {
    // Pega todas as resenhas
    const resenhas = document.querySelectorAll('.resenha-texto');

    resenhas.forEach(resenha => {
        if (resenha.scrollHeight > resenha.clientHeight) {
            // Acha o botão "..." que está logo depois do texto e mostra ele
            const botao = resenha.nextElementSibling;
            if (botao) {
                botao.style.display = 'inline-block';
            }
        }
    });
}

// --- Lógica do Contador de Resenha ---
const resenhaInput = document.getElementById('resenha');
const charCount = document.getElementById('char-count');
const contadorDiv = document.getElementById('contador-caracteres');

if (resenhaInput && charCount) {
    resenhaInput.addEventListener('input', () => {
        const comprimento = resenhaInput.value.length;
        charCount.innerText = comprimento;

        // Feedback visual de cores
        if (comprimento >= 250) {
            contadorDiv.className = 'limite-atingido';
        } else if (comprimento >= 200) {
            contadorDiv.className = 'limite-proximo';
        } else {
            contadorDiv.className = '';
        }
    });
}

// --- PERFIL USUÁRIO ---

// --- LÓGICA DE SENHA FORTE (Universal) ---
document.addEventListener('DOMContentLoaded', () => {

    // Tenta achar o campo pela classe (funciona no Register e no Perfil)
    const inputSenha = document.querySelector('.senha-verifica');
    const boxRequisitos = document.getElementById('box-requisitos');

    if (inputSenha && boxRequisitos) {

        const reqTamanho = document.getElementById('req-tamanho');
        const reqMaiuscula = document.getElementById('req-maiuscula');
        const reqMinuscula = document.getElementById('req-minuscula');
        const reqNumero = document.getElementById('req-numero');
        const reqEspecial = document.getElementById('req-especial');

        // Mostrar/Esconder
        inputSenha.addEventListener('focus', () => boxRequisitos.classList.add('mostrar-requisitos'));
        inputSenha.addEventListener('blur', () => {
            setTimeout(() => boxRequisitos.classList.remove('mostrar-requisitos'), 200);
        });

        // Validação em Tempo Real
        inputSenha.addEventListener('input', () => {
            const valor = inputSenha.value;

            validarItem(reqTamanho, valor.length >= 8);
            validarItem(reqMaiuscula, /[A-Z]/.test(valor));
            validarItem(reqMinuscula, /[a-z]/.test(valor));
            validarItem(reqNumero, /[0-9]/.test(valor));
            validarItem(reqEspecial, /[!@#$%^&*(),.?":{}|<>]/.test(valor));
        });

        function validarItem(elemento, valido) {
            if (!elemento) return; // Segurança extra
            const icone = elemento.querySelector('i');
            if (valido) {
                elemento.classList.add('valido');
                elemento.classList.remove('invalido');
                icone.className = 'fa-solid fa-circle-check';
            } else {
                elemento.classList.remove('valido');
                elemento.classList.add('invalido');
                icone.className = 'fa-solid fa-circle-xmark';
            }
        }
    }
});

// --- PERFIL: ENVIO DO FORMULÁRIO ---
const formSenhaPerfil = document.getElementById('form-senha');
if (formSenhaPerfil) {
    formSenhaPerfil.addEventListener('submit', async (e) => {
        e.preventDefault();
        const senhaAntiga = document.getElementById('senhaAntiga').value;
        const novaSenha = document.querySelector('.senha-verifica').value; // Pega pela classe

        try {
            const response = await fetch('/perfil/senha', {
                method: 'PUT',
                headers: getCsrfHeaders(),
                body: JSON.stringify({ senhaAntiga, novaSenha })
            });

            if (response.ok) {
                Swal.fire('Sucesso!', 'Senha atualizada!', 'success');
                formSenhaPerfil.reset();
                // Reseta ícones visualmente
                document.querySelectorAll('.requisito-item').forEach(el => {
                    el.classList.remove('valido');
                    el.querySelector('i').className = 'fa-solid fa-circle-xmark';
                });
            } else {
                const erro = await response.json();
                Swal.fire('Erro!', erro.message, 'error');
            }
        } catch (err) {
            Swal.fire('Erro!', 'Falha de conexão.', 'error');
        }
    });
}

// 2. Excluir Conta
async function confirmarExclusao() {
    const result = await Swal.fire({
        title: 'TEM CERTEZA?',
        text: "Sua conta e todos os seus itens serão apagados para sempre!",
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#d33',
        cancelButtonColor: '#3085d6',
        confirmButtonText: 'Sim, excluir tudo!',
        cancelButtonText: 'Cancelar'
    });

    if (result.isConfirmed) {
        try {
            const response = await fetch('/perfil', {
                method: 'DELETE',
                headers: getCsrfHeaders()
            });

            if (response.ok) {
                await Swal.fire('Conta Excluída!', 'Sentiremos sua falta. 😢', 'success');
                window.location.href = '/register?deleted'; // Desloga e manda pro register
            } else {
                Swal.fire('Erro!', 'Não foi possível excluir a conta.', 'error');
            }
        } catch (err) {
            Swal.fire('Erro!', 'Falha ao processar.', 'error');
        }
    }
}

// 3. Atualizar Apelido
const formApelido = document.getElementById('form-apelido'); // Tenta pegar o elemento
if (formApelido) { // Só entra se ele existir na página
    formApelido.addEventListener('submit', async (e) => {
        e.preventDefault();
        const novoApelido = document.getElementById('nome-usuario').value;

        try {
            const response = await fetch('/perfil/apelido', {
                method: 'PUT',
                headers: getCsrfHeaders(),
                body: JSON.stringify({ novoApelido })
            });

            if (response.ok) {
                Swal.fire({
                    title: 'Sucesso!',
                    text: 'Apelido atualizado! A página será recarregada.',
                    icon: 'success'
                }).then(() => location.reload());
            } else {
                Swal.fire('Erro!', 'Não foi possível atualizar o apelido.', 'error');
            }
        } catch (err) {
            Swal.fire('Erro!', 'Falha na comunicação.', 'error');
        }
    });
}

// --- LÓGICA DA BUSCA DE CAPAS (V3.0) ---

document.addEventListener('DOMContentLoaded', () => {

    // 1. Pegamos os elementos com segurança
    const btnBusca = document.getElementById('btn-busca-capa');
    const modal = document.getElementById('modalCapas');
    const btnFechar = document.querySelector('.close-modal');
    const inputBusca = document.getElementById('buscaCapaInput');

    // Se não tiver modal na página (ex: login ou home), para aqui e não dá erro
    // É POR ISSO QUE NÃO APARECIA ERRO NO CONSOLE: Ele parava aqui silenciosamente.
    if (!modal || !btnBusca) {
        console.log("Busca de capas não ativa nesta página.");
        return;
    }

    console.log("Sistema de Busca de Capas ATIVO! 🚀"); // Debug para confirmar que carregou

    // 2. Evento de Abrir (substitui o onclick="abrirModalCapa()")
    btnBusca.addEventListener('click', () => {
        modal.style.display = 'flex';

        // Pega o título digitado pra facilitar
        const tituloInput = document.getElementById('titulo');
        if (tituloInput && tituloInput.value && inputBusca) {
            inputBusca.value = tituloInput.value;
            // Opcional: já buscar automático se quiser
            // buscarCapaApi();
        }
        if(inputBusca) inputBusca.focus();
    });

    // 3. Evento de Fechar (Botão X)
    if (btnFechar) {
        btnFechar.addEventListener('click', () => {
            modal.style.display = 'none';
        });
    }

    // 4. Fechar clicando fora da janela (Overlay)
    window.addEventListener('click', (e) => {
        if (e.target === modal) {
            modal.style.display = 'none';
        }
    });

    // 5. Botão "Buscar" de dentro do modal
    // Precisamos achar o botão que chama a função buscarCapaApi
    // Vamos adicionar um ID nele no HTML pra facilitar, ou pegar pelo onclick
    const btnBuscarInterno = document.querySelector('#modalCapas button[onclick="buscarCapaApi()"]');
    if (btnBuscarInterno) {
        btnBuscarInterno.onclick = null; // Remove o onclick antigo do HTML para não duplicar
        btnBuscarInterno.addEventListener('click', buscarCapaApi);
    }

    // 6. Enter no input busca
    if (inputBusca) {
        inputBusca.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') buscarCapaApi();
        });
    }
});

// Essa função precisa ser global ou estar acessível
async function buscarCapaApi() {
    const inputBusca = document.getElementById('buscaCapaInput');
    const divResultados = document.getElementById('resultadosCapas');
    const tipoSelect = document.getElementById('tipo');
    const tipo = tipoSelect ? tipoSelect.value : '';

    if (!inputBusca || !divResultados) return;

    // --- BLOQUEIO PARA JOGOS ---
        if (tipo === 'Jogo') {
            divResultados.innerHTML = `
                <div style="text-align:center; padding: 20px; grid-column: 1 / -1;">
                    <p style="font-size: 3rem; margin: 0;">🎮</p>
                    <p style="color: #ff6b6b; font-weight: bold; margin-top: 10px;">A busca automática é exclusiva para Filmes e Séries.</p>
                    <p style="color: #ccc; font-size: 0.9rem;">Para jogos, copie o link da imagem no Google e cole no campo anterior.</p>
                </div>
            `;
            return; // Para a função aqui. Não chama o Java.
        }
        // ------------------------------------

    const query = inputBusca.value;
    if (!query) return;

    divResultados.innerHTML = '<p style="color:white; text-align:center; grid-column: 1 / -1;">⏳ Buscando na TMDB...</p>';

    try {
        const response = await fetch(`/api/buscar-capa?query=${encodeURIComponent(query)}&tipo=${tipo}`);

        if (!response.ok) throw new Error('Erro na API');

        const lista = await response.json();
        divResultados.innerHTML = '';

        if (lista.length === 0) {
            divResultados.innerHTML = '<p style="color:#ccc; text-align:center; grid-column: 1 / -1;">Nenhum resultado encontrado.</p>';
            return;
        }

        lista.forEach(filme => {
            const div = document.createElement('div');
            div.className = 'capa-item';
            div.onclick = () => selecionarCapa(filme.imagem);

            div.innerHTML = `
                <img src="${filme.imagem}" alt="${filme.titulo}" style="width:100%; border-radius:4px;">
                <p style="color:#ccc; font-size:0.8rem; margin-top:5px; text-align:center;">
                    ${filme.ano ? filme.ano.split('-')[0] : '?'} <br>
                    <b>${filme.titulo}</b>
                </p>
            `;
            divResultados.appendChild(div);
        });

    } catch (erro) {
        console.error(erro);
        divResultados.innerHTML = '<p style="color:#ff6b6b; text-align:center; grid-column: 1 / -1;">Erro ao buscar capas.</p>';
    }
}

function selecionarCapa(url) {
    const inputImg = document.getElementById('imagemUrl');
    const modal = document.getElementById('modalCapas');

    if (inputImg) {
        inputImg.value = url;
        // Se tiver a função de preview, chama ela
        if (typeof atualizarPreview === 'function') {
            atualizarPreview();
        }
    }
    if (modal) modal.style.display = 'none';
}

// --- MOSTRAR/OCULTAR SENHA ---
function togglePassword(botao) {
    // Acha o input que está logo antes do botão
    const input = botao.previousElementSibling;
    const icone = botao.querySelector('i');

    if (input.type === "password") {
        input.type = "text";
        icone.classList.remove('fa-eye');
        icone.classList.add('fa-eye-slash'); // Olho cortado
    } else {
        input.type = "password";
        icone.classList.remove('fa-eye-slash');
        icone.classList.add('fa-eye'); // Olho normal
    }
}

// --- LÓGICA DE COMPARTILHAMENTO (LINKS) ---

const listaLinks = document.getElementById('lista-links');

async function carregarMeusLinks() {
    if (!listaLinks) return; // Só roda na página de links

    try {
        const res = await fetch('/api/share/meus-links');
        const links = await res.json();

        listaLinks.innerHTML = '';

        if (links.length === 0) {
            listaLinks.innerHTML = '<p style="text-align:center; color:#7f8c8d;">Você não tem links ativos.</p>';
            return;
        }

        // Ordena para mostrar os mais novos primeiro (inverte a lista)
        links.reverse().forEach(link => {

            // Calcula data legível
            const dataExp = new Date(link.expiresAt + (link.expiresAt.endsWith('Z') ? '' : 'Z')).toLocaleString('pt-BR');
            // Monta a URL completa (Pega o domínio atual + /share/ + token)
            const urlCompleta = `${window.location.origin}/share/${link.token}`;

            const html = `
                <div class="link-card">
                    <div class="link-info">
                        <h4>Ativo até: ${dataExp}</h4>
                        <div class="link-url">${urlCompleta}</div>
                        <p style="margin-top:5px;">👁️ ${link.visualizacoes} visualizações</p>
                    </div>
                    <div class="link-actions">
                        <button onclick="copiarTexto('${urlCompleta}')" class="btn-copy" title="Copiar">
                            <i class="fa-solid fa-copy"></i> Copiar
                        </button>
                        <button onclick="revogarLink(${link.id})" class="btn-revoke" title="Apagar">
                            <i class="fa-solid fa-trash"></i>
                        </button>
                    </div>
                </div>
            `;
            listaLinks.innerHTML += html;
        });

    } catch (erro) {
        console.error(erro);
        listaLinks.innerHTML = '<p style="color:red">Erro ao carregar links.</p>';
    }
}

async function gerarLink() {
    const horas = document.getElementById('validade').value;

    try {
        const res = await fetch('/api/share/gerar', {
            method: 'POST',
            headers: getCsrfHeaders(),
            body: JSON.stringify({ horas: parseInt(horas) })
        });

        if (res.ok) {
            Swal.fire({
                title: 'Link Gerado!',
                text: 'Seu link está pronto para ser enviado.',
                icon: 'success',
                timer: 1500
            });
            carregarMeusLinks(); // Recarrega a lista
        } else {
            Swal.fire('Erro', 'Não foi possível gerar o link.', 'error');
        }
    } catch (erro) {
        console.error(erro);
    }
}

async function revogarLink(id) {
    const result = await Swal.fire({
        title: 'Apagar Link?',
        text: "Quem tiver esse link não poderá mais acessar sua lista.",
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#d33',
        confirmButtonText: 'Sim, apagar'
    });

    if (result.isConfirmed) {
        try {
            await fetch(`/api/share/${id}`, {
                method: 'DELETE',
                headers: getCsrfHeaders()
            });
            carregarMeusLinks();
            Swal.fire('Apagado!', 'O link foi invalidado.', 'success');
        } catch (erro) {
            Swal.fire('Erro', 'Falha ao apagar.', 'error');
        }
    }
}

function copiarTexto(texto) {
    navigator.clipboard.writeText(texto).then(() => {
        const Toast = Swal.mixin({
            toast: true,
            position: 'top-end',
            showConfirmButton: false,
            timer: 3000,
            timerProgressBar: true
        });
        Toast.fire({
            icon: 'success',
            title: 'Link copiado!'
        });
    });
}