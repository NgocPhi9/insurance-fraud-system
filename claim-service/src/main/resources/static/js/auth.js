document.addEventListener('DOMContentLoaded', function () {

    const toggleBtn  = document.getElementById('togglePassword');
    const passwordEl = document.getElementById('password');
    const toggleIcon = document.getElementById('toggleIcon');

    if (toggleBtn && passwordEl && toggleIcon) {
        toggleBtn.addEventListener('click', function () {
            const isPassword = passwordEl.type === 'password';
            passwordEl.type  = isPassword ? 'text' : 'password';
            toggleIcon.className = isPassword
                ? 'bi bi-eye-slash text-muted'
                : 'bi bi-eye text-muted';
        });
    }
});