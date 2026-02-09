document.addEventListener('DOMContentLoaded', () => {
    const htmlElement = document.documentElement;
    const themeToggle = document.getElementById('theme-toggle');
    const themeIcon = document.getElementById('theme-icon');

    // Sempre carrega o tema salvo (em qualquer página)
    const savedTheme = localStorage.getItem('theme') || 'light';
    if (savedTheme === 'dark') {
        htmlElement.setAttribute('data-theme', 'dark');
        if (themeIcon) themeIcon.textContent = '☀️';
    }

    // Só tenta configurar o clique se o botão existir
    if (themeToggle) {
        themeToggle.addEventListener('click', () => {
            const isDark = htmlElement.getAttribute('data-theme') === 'dark';
            if (isDark) {
                htmlElement.removeAttribute('data-theme');
                localStorage.setItem('theme', 'light');
                if (themeIcon) themeIcon.textContent = '🌙';
            } else {
                htmlElement.setAttribute('data-theme', 'dark');
                localStorage.setItem('theme', 'dark');
                if (themeIcon) themeIcon.textContent = '☀️';
            }
        });
    }
});