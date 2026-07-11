document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('[data-export-url]').forEach((botao) => {
        botao.addEventListener('click', async (e) => {
            e.preventDefault();
            await exportarArquivo(botao);
        });
    });
});

async function exportarArquivo(botao) {
    const url = botao.getAttribute('data-export-url');
    const nomeArquivo = botao.getAttribute('data-export-filename') || 'download';
    const textoOriginal = botao.innerHTML;

    botao.disabled = true;
    botao.classList.add('is-loading');
    botao.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> A gerar...';

    try {
        const resposta = await fetch(url, {
            method: 'GET',
            credentials: 'same-origin'
        });

        if (resposta.status === 401 || resposta.status === 403) {
            throw new Error('Sessão expirada. Faça login novamente.');
        }

        if (!resposta.ok) {
            throw new Error('Não foi possível gerar o arquivo (' + resposta.status + ').');
        }

        const blob = await resposta.blob();

        if (estaRodandoNoApp()) {
            await salvarESeCompartilharNoDispositivo(blob, nomeArquivo);
        } else {
            baixarNoNavegador(blob, nomeArquivo);
        }
    } catch (erro) {
        console.error('Erro ao exportar arquivo:', erro);
        if (window.Swal) {
            Swal.fire('Ops!', erro.message || 'Não foi possível baixar o arquivo. Tente novamente.', 'error');
        } else {
            alert(erro.message || 'Não foi possível baixar o arquivo. Tente novamente.');
        }
    } finally {
        botao.disabled = false;
        botao.classList.remove('is-loading');
        botao.innerHTML = textoOriginal;
    }
}

// Download "clássico" via navegador.
function baixarNoNavegador(blob, nomeArquivo) {
    const blobUrl = window.URL.createObjectURL(blob);

    const linkTemporario = document.createElement('a');
    linkTemporario.href = blobUrl;
    linkTemporario.download = nomeArquivo;
    document.body.appendChild(linkTemporario);
    linkTemporario.click();
    linkTemporario.remove();

    // Libera a memória do Blob após o clique
    setTimeout(() => window.URL.revokeObjectURL(blobUrl), 1000);
}

// Detecta se estamos rodando dentro do wrapper nativo (Capacitor/Android).
function estaRodandoNoApp() {
    return !!(window.Capacitor && typeof window.Capacitor.isNativePlatform === 'function' && window.Capacitor.isNativePlatform());
}

// Salva o blob de verdade no armazenamento do app e abre o menu nativo de
// compartilhar/salvar, usando os plugins @capacitor/filesystem e @capacitor/share.
async function salvarESeCompartilharNoDispositivo(blob, nomeArquivo) {
    const plugins = window.Capacitor && window.Capacitor.Plugins;
    const Filesystem = plugins && plugins.Filesystem;
    const Share = plugins && plugins.Share;

    if (!Filesystem || !Share) {
        throw new Error('Exportação pelo app ainda não está configurada. Tente pelo site por enquanto.');
    }

    const base64Data = await blobParaBase64(blob);

    const arquivoSalvo = await Filesystem.writeFile({
        path: nomeArquivo,
        data: base64Data,
        directory: 'CACHE'
    });
    
    await Share.share({
        title: nomeArquivo,
        dialogTitle: 'Salvar ou compartilhar arquivo',
        files: [arquivoSalvo.uri]
    });
}

// Converte um Blob em base64 puro
function blobParaBase64(blob) {
    return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onloadend = () => {
            const resultado = reader.result;
            const base64 = typeof resultado === 'string' ? resultado.split(',')[1] : '';
            resolve(base64);
        };
        reader.onerror = reject;
        reader.readAsDataURL(blob);
    });
}
