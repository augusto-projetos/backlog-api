document.addEventListener('DOMContentLoaded', () => {
    const formSocial = document.getElementById('form-social');

    // Função auxiliar para capturar o token CSRF das meta tags do HTML
    function getCsrfHeaders() {
        const token = document.querySelector('meta[name="_csrf"]').getAttribute('content');
        const header = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');
        return {
            'Content-Type': 'application/json',
            [header]: token
        };
    }

    if (formSocial) {
        formSocial.addEventListener('submit', async (e) => {
            e.preventDefault(); // Impede o recarregamento da página

            // Captura os valores dos inputs
            const socialUsernameInput = document.getElementById('input-social-username').value.trim();
            const isPublicInput = document.getElementById('toggle-publico').checked;

            // 1. Validação local (Regex) para evitar chamadas desnecessárias à API
            const regex = /^[a-zA-Z0-9_.]+$/;
            
            if (!socialUsernameInput) {
                Swal.fire('Atenção', 'O nome de utilizador (@) não pode estar vazio.', 'warning');
                return;
            }
            
            if (!regex.test(socialUsernameInput)) {
                Swal.fire('Atenção', 'O @ só pode conter letras, números, pontos ou sublinhados.', 'warning');
                return;
            }

            // 2. Envio dos dados para o Backend
            try {
                // Desativa o botão temporariamente para evitar múltiplos cliques
                const btnSalvar = document.getElementById('btn-salvar-social');
                const textoOriginal = btnSalvar.innerText;
                btnSalvar.innerText = "A guardar...";
                btnSalvar.disabled = true;

                const response = await fetch('/api/users/profile', {
                    method: 'PUT',
                    headers: getCsrfHeaders(),
                    body: JSON.stringify({
                        socialUsername: socialUsernameInput,
                        isPublic: isPublicInput
                    })
                });

                // Restaura o botão
                btnSalvar.innerText = textoOriginal;
                btnSalvar.disabled = false;

                if (response.ok) {
                    Swal.fire('Sucesso!', 'O seu perfil social foi atualizado com sucesso.', 'success');
                } else {
                    // Tenta ler a mensagem de erro que enviámos do Java
                    const errorMsg = await response.text();
                    Swal.fire('Erro', errorMsg || 'Não foi possível atualizar o perfil.', 'error');
                }
            } catch (error) {
                console.error("Erro ao atualizar o perfil social:", error);
                Swal.fire('Erro', 'Ocorreu um problema de ligação ao servidor.', 'error');
            }
        });
    }
});