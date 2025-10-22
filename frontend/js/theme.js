document.addEventListener('DOMContentLoaded', () => {
    const themeToggle = document.getElementById('theme-toggle');
    const currentTheme = localStorage.getItem('theme');

    // Function to apply the theme
    const applyTheme = (theme) => {
        if (theme === 'dark') {
            document.documentElement.setAttribute('data-theme', 'dark');
            if(themeToggle) themeToggle.checked = true;
        } else {
            document.documentElement.removeAttribute('data-theme');
            if(themeToggle) themeToggle.checked = false;
        }
    };

    // Apply saved theme on load
    if (currentTheme) {
        applyTheme(currentTheme);
    } else {
        // If no theme is saved, check OS preference
        const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
        applyTheme(prefersDark ? 'dark' : 'light');
    }

    // Add event listener for the toggle switch
    if (themeToggle) {
        themeToggle.addEventListener('change', () => {
            const newTheme = themeToggle.checked ? 'dark' : 'light';
            localStorage.setItem('theme', newTheme);
            applyTheme(newTheme);
        });
    }
});