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
        const blobUrl = window.URL.createObjectURL(blob);

        const linkTemporario = document.createElement('a');
        linkTemporario.href = blobUrl;
        linkTemporario.download = nomeArquivo;
        document.body.appendChild(linkTemporario);
        linkTemporario.click();
        linkTemporario.remove();

        // Libera a memória do Blob após o clique
        setTimeout(() => window.URL.revokeObjectURL(blobUrl), 1000);
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
