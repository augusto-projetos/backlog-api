document.addEventListener('DOMContentLoaded', () => {
    const inputBusca = document.getElementById('input-busca-social');
    const dropdown = document.getElementById('dropdown-busca-social');
    const btnMobile = document.getElementById('btn-mobile-busca');
    const containerBusca = document.getElementById('container-busca-social');
    const btnFechar = document.getElementById('btn-fechar-busca');
    let timeoutBusca = null;

    if (!inputBusca || !dropdown) return;

    // Lógica de abrir/fechar no celular
    if (btnMobile && containerBusca && btnFechar) {

        // Abre a barra flutuante
        btnMobile.addEventListener('click', (e) => {
            e.stopPropagation(); // Evita que feche na mesma hora
            containerBusca.classList.add('ativa');
            inputBusca.focus(); // Já coloca o teclado na tela
        });

        // Fecha pelo botão X
        btnFechar.addEventListener('click', () => {
            containerBusca.classList.remove('ativa');
            dropdown.style.display = 'none';
            inputBusca.value = ''; // Limpa a busca ao fechar
        });

        // Fecha se o usuário tocar em qualquer lugar fora da barra
        document.addEventListener('click', (e) => {
            if (containerBusca.classList.contains('ativa') &&
                !containerBusca.contains(e.target) &&
                !btnMobile.contains(e.target)) {

                containerBusca.classList.remove('ativa');
                dropdown.style.display = 'none';
            }
        });
    }

    // Fecha o dropdown se clicar fora
    document.addEventListener('click', (e) => {
        if (!inputBusca.contains(e.target) && !dropdown.contains(e.target)) {
            dropdown.style.display = 'none';
        }
    });

    inputBusca.addEventListener('input', (e) => {
        const query = e.target.value.trim();

        // Limpa o timer anterior
        clearTimeout(timeoutBusca);

        if (query.length < 2) {
            dropdown.style.display = 'none';
            dropdown.innerHTML = '';
            return;
        }

        // Aguarda 300ms após o usuário parar de digitar para fazer a busca (Debounce)
        timeoutBusca = setTimeout(async () => {
            dropdown.style.display = 'flex';
            dropdown.innerHTML = '<div class="social-msg">Buscando... 🔍</div>';

            try {
                const response = await fetch(`/api/users/search?q=${encodeURIComponent(query)}`);
                if (!response.ok) throw new Error('Erro na busca');

                const usuarios = await response.json();
                dropdown.innerHTML = '';

                if (usuarios.length === 0) {
                    dropdown.innerHTML = '<div class="social-msg">Nenhum perfil encontrado. 😕</div>';
                    return;
                }

                usuarios.forEach(user => {
                    // Pega a primeira letra do @ para fazer uma foto de perfil improvisada
                    const inicial = user.socialUsername.charAt(0).toUpperCase();

                    const link = document.createElement('a');
                    link.className = 'social-user-item';
                    // Ao clicar, redireciona para a página do perfil público
                    link.href = `/u/${user.socialUsername}`;

                    link.innerHTML = `
                        <div class="social-avatar">${inicial}</div>
                        <div class="social-name">@${user.socialUsername}</div>
                    `;

                    dropdown.appendChild(link);
                });

            } catch (error) {
                console.error("Erro ao buscar usuários:", error);
                dropdown.innerHTML = '<div class="social-msg" style="color: #e74c3c;">Erro ao buscar. Tente novamente.</div>';
            }
        }, 300);
    });

    // Se clicar no input e já tiver algo digitado, mostra a gaveta de novo
    inputBusca.addEventListener('focus', () => {
        if (inputBusca.value.trim().length >= 2 && dropdown.innerHTML !== '') {
            dropdown.style.display = 'flex';
        }
    });
});
