// --- Lógica de Inicialização ---

const listaItens = document.getElementById('lista-itens');
const formCadastro = document.getElementById('form-cadastro');

// Variável global para saber se estamos editando
let idEdicao = null;

// 1. Lógica da Home (Lista)
if (listaItens && !window.location.pathname.includes('/share') && !window.location.pathname.includes('/u/')) {
    carregarItens();
}

// Lógica para a Página Compartilhada (/share) e Perfil Público (/u/)
if (window.location.pathname.includes('/share') || window.location.pathname.includes('/u/')) {
    configurarRatingShared();
}

// OUVINTE PARA A TROCA DE SISTEMA DE AVALIAÇÃO
window.addEventListener('ratingModeChanged', () => {
    // Busca todas as divs de nota na tela
    const notasNaTela = document.querySelectorAll('.nota[data-nota]');
    const ratingMode = localStorage.getItem('ratingMode') || 'nota';

    notasNaTela.forEach(el => {
        // Pega o valor bruto que guardamos no atributo oculto
        const notaOriginal = el.getAttribute('data-nota');
        if (notaOriginal === null || notaOriginal === '') return;

        const notaNum = parseFloat(notaOriginal);

        if (ratingMode === 'estrela') {
            el.innerHTML = gerarEstrelasHTML(notaNum);
        } else {
            el.innerHTML = `Nota: ${notaNum}/10`;
        }
    });
});

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

    setTimeout(travarCamposPeloStatus, 100);

    // Quando mudar o STATUS (ex: de Backlog para Zerado), roda a função
    document.getElementById('status').addEventListener('change', travarCamposPeloStatus);

    // Quando mudar o TIPO, também roda, pois o tipo muda as opções de status
    document.getElementById('tipo').addEventListener('change', () => {
        atualizarStatusDinamico();
        travarCamposPeloStatus();
    });

    // Ouve o clique no botão salvar
    formCadastro.addEventListener('submit', salvarItem);

    // --- LÓGICA DAS ESTRELAS INTERATIVAS NO CADASTRO ---
    const inputNota = document.getElementById('nota');
    const starsContainer = document.getElementById('interactive-stars');
    const starText = document.getElementById('star-rating-text');
    const ratingMode = localStorage.getItem('ratingMode') || 'nota';

    if (inputNota && starsContainer && ratingMode === 'estrela') {
        // Esconde o input numérico e mostra as estrelas
        inputNota.style.display = 'none';
        starsContainer.style.display = 'inline-flex';
        if (starText) starText.style.display = 'inline';

        // Cria as 5 estrelas interagíveis
        for (let i = 0; i < 5; i++) {
            const star = document.createElement('span');
            star.className = 'interactive-star';
            star.innerHTML = '★<span class="half">★</span>';

            // Função: descobre se o clique/hover foi na metade esquerda ou direita da estrela
            const calculateRating = (e) => {
                const rect = star.getBoundingClientRect();
                const isHalf = (e.clientX - rect.left) < (rect.width / 2);
                return (i * 2) + (isHalf ? 1 : 2); // Transforma em nota de 0 a 10
            };

            // Evento: Passar o mouse (Hover) pinta as estrelas temporariamente
            star.addEventListener('mousemove', (e) => {
                const hoverRating = calculateRating(e);
                renderInteractiveStars(hoverRating);
            });

            // Evento: Clicar salva a nota de verdade no input escondido
            star.addEventListener('click', (e) => {
                const selectedRating = calculateRating(e);
                inputNota.value = selectedRating; // Envia o valor pro formulário
                renderInteractiveStars(selectedRating);
            });

            starsContainer.appendChild(star);
        }

        // Se tirar o mouse do container inteiro, volta a pintar o que está salvo no input
        starsContainer.addEventListener('mouseleave', () => {
            renderInteractiveStars(inputNota.value || 0);
        });

        // Função que pinta as estrelas baseada no número recebido
        function renderInteractiveStars(rating) {
            const ratingNum = parseFloat(rating) || 0;
            const notaArredondada = Math.ceil(ratingNum);
            const numEstrelas = notaArredondada / 2;

            const allStars = starsContainer.querySelectorAll('.interactive-star');
            allStars.forEach((s, index) => {
                s.classList.remove('filled', 'half-filled');
                if (index + 1 <= Math.floor(numEstrelas)) {
                    s.classList.add('filled');
                } else if (index < numEstrelas) {
                    s.classList.add('half-filled');
                }
            });

            if (starText) {
                const valorEmEstrelas = ratingNum / 2; // Converte a nota de 10 para a escala de 5
                starText.textContent = ratingNum > 0 ? `${valorEmEstrelas}/5` : '0/5';
            }
        }

        // Esperamos um tempinho rápido para caso seja uma Edição de Item.
        // Dá tempo do fetch trazer a nota antiga e preencher o input antes de pintarmos as estrelas.
        setTimeout(() => {
            renderInteractiveStars(inputNota.value || 0);
        }, 300);
    }
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

            travarCamposPeloStatus();

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
            const data = await resposta.json();

            // Dispara toasts de conquistas ANTES de redirecionar
            const novas = data.conquistasDesbloqueadas || [];

            Swal.fire({
                title: 'Sucesso!',
                text: 'Item salvo com sucesso!',
                icon: 'success',
                confirmButtonText: 'Ok'
            }).then(() => {
                // Armazena conquistas para exibir na home após redirect
                if (novas.length > 0) {
                    sessionStorage.setItem('conquistas_pendentes', JSON.stringify(novas));
                }
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

            // Verifica qual modo o usuário quer ver
            const ratingMode = localStorage.getItem('ratingMode') || 'nota';
            let htmlAvaliacao = '';

            if (ratingMode === 'estrela') {
                htmlAvaliacao = gerarEstrelasHTML(item.nota);
            } else {
                htmlAvaliacao = `Nota: ${item.nota}/10`;
            }

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
                        <div class="nota" data-nota="${item.nota}">
                            ${htmlAvaliacao}
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

// Função para travar Nota e Resenha dependendo do Status
function travarCamposPeloStatus() {
    const status = document.getElementById('status').value;
    const tipo = document.getElementById('tipo').value;
    const notaInput = document.getElementById('nota');
    const resenhaInput = document.getElementById('resenha');
    const starsContainer = document.getElementById('interactive-stars');

    // Lista de status que devem travar os campos
    const statusBloqueados = ['Backlog', 'Jogando', 'Assistindo'];

    if (statusBloqueados.includes(status)) {
        // --- MODO TRAVADO ---

        // 1. Texto Dinâmico (Jogo/Filme/Série)
        let artigo = "o item";
        if (tipo === 'Jogo') artigo = "o jogo";
        else if (tipo === 'Filme') artigo = "o filme";
        else if (tipo === 'Série') artigo = "a série";

        // 2. Trava a Resenha
        resenhaInput.disabled = true;
        resenhaInput.style.opacity = "0.5";
        resenhaInput.style.cursor = "not-allowed";
        resenhaInput.placeholder = `Termine ${artigo} para escrever uma resenha.`;

        // 3. Trava a Nota e define como 0
        notaInput.value = 0;
        notaInput.readOnly = true; // readOnly para o usuário ver o 0 mas não mudar
        notaInput.style.opacity = "0.5";
        notaInput.style.cursor = "not-allowed";

        // 4. Trava e limpa as Estrelas
        if (starsContainer) {
            starsContainer.style.pointerEvents = "none";
            starsContainer.style.opacity = "0.5";

            // Remove o preenchimento de todas as estrelas
            const allStars = starsContainer.querySelectorAll('.interactive-star');
            allStars.forEach(s => s.classList.remove('filled', 'half-filled'));

            // Reseta o texto ao lado das estrelas
            const starText = document.getElementById('star-rating-text');
            if (starText) starText.textContent = '0/5';
        }

    } else {
        // --- MODO LIBERADO (Zerado, Assistido, Dropado) ---

        // 1. Libera Resenha
        resenhaInput.disabled = false;
        resenhaInput.style.opacity = "1";
        resenhaInput.style.cursor = "text";
        resenhaInput.placeholder = "Escreva sua opinião..."; // Restaura placeholder

        // 2. Libera Nota
        notaInput.readOnly = false;
        notaInput.style.opacity = "1";
        notaInput.style.cursor = "text";

        // 3. Remove o '0' automático apenas se o usuário ainda não tiver dado nota
        if (notaInput.value == 0) {
            notaInput.value = '';
        }

        // 4. Libera as Estrelas
        if (starsContainer) {
            starsContainer.style.pointerEvents = "auto";
            starsContainer.style.opacity = "1";
            starsContainer.dispatchEvent(new Event('mouseleave'));
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

// --- LÓGICA DA BUSCA DE CAPAS ---

document.addEventListener('DOMContentLoaded', () => {

    // 1. Pegamos os elementos com segurança
    const btnBusca = document.getElementById('btn-busca-capa');
    const modal = document.getElementById('modalCapas');
    const btnFechar = document.querySelector('.close-modal');
    const inputBusca = document.getElementById('buscaCapaInput');

    // Se não tiver modal na página (ex: login ou home), para aqui e não dá erro
    if (!modal || !btnBusca) {
        console.log("Busca de capas não ativa nesta página.");
        return;
    }

    // 2. Evento de Abrir
    btnBusca.addEventListener('click', () => {
        modal.style.display = 'flex';

        // Pega o título digitado pra facilitar
        const tituloInput = document.getElementById('titulo');
        if (tituloInput && tituloInput.value && inputBusca) {
            inputBusca.value = tituloInput.value;
            buscarCapaApi();
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

// --- FUNÇÃO PARA RENDERIZAR ESTRELAS ---
function gerarEstrelasHTML(notaOriginal) {
    if (notaOriginal === 0 || notaOriginal === '' || notaOriginal === null) {
        return '<span style="color: #7f8c8d; font-size: 0.9rem;">Sem nota</span>';
    }

    // A lógica matemática para meias estrelas
    const notaArredondada = Math.ceil(notaOriginal);
    const numEstrelas = notaArredondada / 2;
    let estrelasHTML = '';

    for (let i = 1; i <= 5; i++) {
        if (i <= numEstrelas) {
            // Estrela preenchida (inteira)
            estrelasHTML += '<span class="star filled">★</span>';
        } else if (i - 0.5 === numEstrelas) {
            // Meia estrela (truque de sobreposição com CSS)
            estrelasHTML += `
            <span class="star-half-container">
                <span class="star empty">★</span>
                <span class="star filled half-filled">★</span>
            </span>`;
        } else {
            // Estrela vazia
            estrelasHTML += '<span class="star empty">★</span>';
        }
    }

    return `<div class="stars-container" title="Nota original: ${notaOriginal}/10">${estrelasHTML}</div>`;
}

// --- FUNÇÃO PARA RENDERIZAR E FORMATAR AS NOTAS NA PÁGINA COMPARTILHADA ---
function configurarRatingShared() {
    // Busca todas as divs de nota que possuem o atributo data-nota
    const notasShared = document.querySelectorAll('.nota[data-nota]');
    const ratingMode = localStorage.getItem('ratingMode') || 'nota';

    notasShared.forEach(el => {
        const notaOriginal = el.getAttribute('data-nota');
        if (notaOriginal === null || notaOriginal === '') return;

        const notaNum = parseFloat(notaOriginal);

        if (ratingMode === 'estrela') {
            // Aplica o sistema de estrelas
            el.innerHTML = gerarEstrelasHTML(notaNum);
        } else {
            // Restaura o modo nota
            el.innerHTML = `Nota: ${notaNum}/10`;
        }
    });
}

// Lógica de Controle da Gaveta Mobile
document.addEventListener('DOMContentLoaded', () => {
    const btnGaveta = document.getElementById('btn-mobile-gaveta');
    const wrapperGaveta = document.getElementById('gaveta-opcoes-mobile');
    const overlayGaveta = document.getElementById('gaveta-overlay');
    const conteudoGaveta = wrapperGaveta ? wrapperGaveta.querySelector('.gaveta-conteudo') : null;
    const linksGaveta = document.querySelectorAll('.gaveta-conteudo a, .gaveta-conteudo button');

    let startY = 0;
    let currentY = 0;
    let IsDragging = false;

    if (btnGaveta && wrapperGaveta && overlayGaveta && conteudoGaveta) {
        
        // Abre a gaveta subindo o transform para 0
        btnGaveta.addEventListener('click', (e) => {
            e.stopPropagation();
            wrapperGaveta.classList.add('ativa');
            conteudoGaveta.style.transition = 'transform 0.4s cubic-bezier(0.25, 0.8, 0.25, 1)';
            conteudoGaveta.style.transform = 'translateY(0)';
            document.body.style.overflow = 'hidden';
        });

        // Função unificada de fechar
        function fecharGaveta() {
            if (window.innerWidth > 600) {
                wrapperGaveta.classList.remove('ativa');
                document.body.style.overflow = '';
                return;
            }

            conteudoGaveta.style.transition = 'transform 0.35s cubic-bezier(0.32, 0.94, 0.6, 1)';
            conteudoGaveta.style.transform = 'translateY(100%)'; // Desce suavemente a partir de onde estiver
            
            overlayGaveta.style.opacity = '0';
            overlayGaveta.style.pointerEvents = 'none';

            setTimeout(() => {
                wrapperGaveta.classList.remove('remove'); // Remove estados ativos
                wrapperGaveta.classList.remove('ativa');
                overlayGaveta.style.opacity = '';
                overlayGaveta.style.pointerEvents = '';
                document.body.style.overflow = '';
            }, 350);
        }

        overlayGaveta.addEventListener('click', fecharGaveta);
        linksGaveta.forEach(elemento => {
            elemento.addEventListener('click', () => {
                if (window.innerWidth <= 600) {
                    fecharGaveta();
                }
            });
        });

        // --- EVENTOS DE TOQUE (TOUCH) ---
        conteudoGaveta.addEventListener('touchstart', (e) => {
            if (window.innerWidth > 600) return;
            startY = e.touches[0].clientY;
            IsDragging = true;
            conteudoGaveta.style.transition = 'none'; // Remove a transição para seguir o dedo em tempo real
        }, { passive: false });

        conteudoGaveta.addEventListener('touchmove', (e) => {
            if (!IsDragging || window.innerWidth > 600) return;

            currentY = e.touches[0].clientY;
            const deltaY = currentY - startY;

            if (e.cancelable) e.preventDefault();

            if (deltaY > 0) {
                conteudoGaveta.style.transform = `translateY(${deltaY}px)`;
            }
        }, { passive: false });

        conteudoGaveta.addEventListener('touchend', (e) => {
            if (!IsDragging || window.innerWidth > 600) return;
            IsDragging = false;

            const deltaY = currentY - startY;

            if (deltaY > 120) {
                fecharGaveta(); // Continua a descida nativa se arrastou bastante
            } else {
                // Volta para o topo suavemente caso tenha arrastado pouco
                conteudoGaveta.style.transition = 'transform 0.3s cubic-bezier(0.25, 0.8, 0.25, 1)';
                conteudoGaveta.style.transform = 'translateY(0)';
            }
            
            startY = 0;
            currentY = 0;
        });

        overlayGaveta.addEventListener('touchmove', (e) => {
            if (e.cancelable && window.innerWidth <= 600) e.preventDefault();
        }, { passive: false });
    }
});

// --- SISTEMA DE RECOMENDAÇÃO INTELIGENTE (IA) ---
const btnIaPc = document.getElementById('btn-ia-toggle-pc');
const btnIaMobile = document.getElementById('btn-ia-toggle-mobile');
const dropdownIaOpcoes = document.getElementById('dropdown-ia-opcoes');
const wrapperGavetaMobile = document.getElementById('gaveta-opcoes-mobile');

// Abrir/Fechar balão flutuante no PC
if (btnIaPc) {
    btnIaPc.addEventListener('click', (e) => {
        e.stopPropagation();
        dropdownIaOpcoes.classList.toggle('ativo');
    });
}

// No Celular: clicar no botão pílula da gaveta abre a janela flutuante de opções
if (btnIaMobile) {
    btnIaMobile.addEventListener('click', (e) => {
        e.stopPropagation();
        // Fecha a gaveta mobile primeiro para dar espaço
        if (wrapperGavetaMobile) wrapperGavetaMobile.classList.remove('ativa');
        document.body.style.overflow = '';
        
        // Abre o painel de opções de IA
        dropdownIaOpcoes.classList.add('ativo');
    });
}

// Fecha as opções flutuantes clicando em qualquer outro lugar fora
document.addEventListener('click', (e) => {
    if (dropdownIaOpcoes && !dropdownIaOpcoes.contains(e.target) && e.target !== btnIaPc && e.target !== btnIaMobile) {
        dropdownIaOpcoes.classList.remove('ativo');
    }
});

// --- LÓGICA DE REQUISIÇÃO AJAX DO BACKEND E EFEITO DIGITAÇÃO ---
function abrirModalIA(tipo) {
    const modal = document.getElementById('modal-ia');
    const titulo = document.getElementById('modal-ia-titulo');
    const loading = document.getElementById('ia-loading');
    const textoResposta = document.getElementById('ia-texto-resposta');

    dropdownIaOpcoes.classList.remove('ativo'); // Fecha as opções
    titulo.innerText = `🤖 Recomendações de ${tipo}s`;
    textoResposta.innerText = ''; 
    
    modal.classList.add('ativo');
    loading.style.display = 'block';
    document.body.style.overflow = 'hidden';

    fetch(`/api/recomendacoes?tipo=${tipo}`)
        .then(response => {
            if (!response.ok) throw new Error('Erro na requisição');
            return response.json();
        })
        .then(data => {
            loading.style.display = 'none';
            efeitoDigitacao(textoResposta, data.recomendacao);
        })
        .catch(error => {
            loading.style.display = 'none';
            textoResposta.innerHTML = `<span style="color: #e74c3c;">🤖 Ih, deu erro! O Geminino falhou na conexão com o servidor.</span>`;
        });
}

function fecharModalIA() {
    document.getElementById('modal-ia').classList.remove('ativo');
    document.body.style.overflow = '';
}

function efeitoDigitacao(elemento, texto) {
    let i = 0;
    let textoAcumulado = '';
    elemento.innerHTML = '';
    
    function digitar() {
        if (i < texto.length) {
            textoAcumulado += texto.charAt(i);
            
            // Renderiza o texto processado com HTML real (negritos, itálicos)
            elemento.innerHTML = converterMarkdownParaHtml(textoAcumulado);
            i++;
            
            const modalBody = document.querySelector('.modal-ia-body');
            if (modalBody) modalBody.scrollTop = modalBody.scrollHeight;
            
            setTimeout(digitar, 12);
        }
    }
    digitar();
}

function converterMarkdownParaHtml(texto) {
    // 1. Escapa o HTML nativo para evitar ataques XSS
    let html = texto
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;");

    // 2. Converte negritos (ex: **texto** para <strong>texto</strong>)
    html = html.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');

    // 3. Converte itálicos simples (ex: *texto* para <em>texto</em>)
    html = html.replace(/\*(.*?)\*/g, '<em>$1</em>');

    // 4. Converte marcadores de lista/tópicos (ex: * Item ou - Item)
    html = html.replace(/^\s*[\*\-]\s+(.*)$/gm, '• $1');

    return html;
}