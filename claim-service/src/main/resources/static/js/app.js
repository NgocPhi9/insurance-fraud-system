document.addEventListener('DOMContentLoaded', function () {

    const sidebar      = document.getElementById('sidebar');
    const mainContent  = document.querySelector('.main-content');
    const toggleBtn    = document.getElementById('sidebarToggleBtn');
    const mobileToggle = document.getElementById('mobileSidebarToggle');
    const overlay      = document.getElementById('sidebarOverlay');
    const openIcon     = document.querySelector('.sidebar-open-icon');
    const closedIcon   = document.querySelector('.sidebar-closed-icon');

    const COLLAPSED_KEY = 'sidebarCollapsed';
    const isMobile = () => window.innerWidth < 768;

    // ── Set collapsed state ───────────────────────────
    // ── Set collapsed state ───────────────────────────
    function setCollapsed(collapsed) {
        if (!sidebar) return;
        if (collapsed) {
            document.querySelectorAll('.sidebar .sidebar-submenu.show').forEach(menu => {
                menu.classList.remove('show', 'collapsing');
                menu.style.height = '';

                const trigger = document.querySelector(`[href="#${menu.id}"]`);
                if (trigger) trigger.classList.add('collapsed');
            });
        }

        sidebar.classList.toggle('collapsed', collapsed);
        if (mainContent) mainContent.classList.toggle('collapsed', collapsed);
        localStorage.setItem(COLLAPSED_KEY, collapsed);
        if (openIcon) openIcon.classList.toggle('d-none', collapsed);
        if (closedIcon) closedIcon.classList.toggle('d-none', !collapsed);
    }

    // ── Restore trạng thái ────────────────────────────
    if (sidebar && !isMobile()) {
        const wasCollapsed = localStorage.getItem(COLLAPSED_KEY) === 'true';
        if (wasCollapsed) setCollapsed(true);
    }

    // ── Desktop toggle btn ────────────────────────────
    if (toggleBtn && sidebar) {
        toggleBtn.addEventListener('click', function () {
            if (isMobile()) return;
            const isCollapsed = sidebar.classList.contains('collapsed');
            setCollapsed(!isCollapsed);
        });
    }

    // ── Click icon có submenu khi collapsed ───────────
    if (sidebar) {
        document.querySelectorAll('.sidebar [data-bs-toggle="collapse"]').forEach(trigger => {
            trigger.addEventListener('click', function (e) {
                if (!sidebar.classList.contains('collapsed')) return;

                e.preventDefault();
                e.stopPropagation();

                const targetId = this.getAttribute('href');
                const targetEl = document.querySelector(targetId);

                setCollapsed(false);

                requestAnimationFrame(() => {
                    if (targetEl) {
                        const bsCollapse = bootstrap.Collapse.getOrCreateInstance(targetEl, {
                            toggle: false
                        });
                        bsCollapse.show();
                        this.classList.remove('collapsed');
                    }
                });
            });
        });
    }

    // ── Mobile toggle ─────────────────────────────────
    if (mobileToggle && sidebar && overlay) {
        mobileToggle.addEventListener('click', function () {
            sidebar.classList.toggle('mobile-open');
            overlay.classList.toggle('d-none');
        });
    }

    if (overlay && sidebar) {
        overlay.addEventListener('click', function () {
            sidebar.classList.remove('mobile-open');
            overlay.classList.add('d-none');
        });
    }

    // ── Lắng nghe sự kiện toggle để lưu trạng thái submenu ───
    const SUBMENU_STATE_KEY = 'sidebarSubmenuStates';
    let submenuStates = {};
    try {
        submenuStates = JSON.parse(localStorage.getItem(SUBMENU_STATE_KEY)) || {};
    } catch (e) {
        submenuStates = {};
    }

    document.querySelectorAll('.sidebar .sidebar-submenu').forEach(menu => {
        menu.addEventListener('shown.bs.collapse', function () {
            submenuStates[menu.id] = true;
            localStorage.setItem(SUBMENU_STATE_KEY, JSON.stringify(submenuStates));
        });
        menu.addEventListener('hidden.bs.collapse', function () {
            submenuStates[menu.id] = false;
            localStorage.setItem(SUBMENU_STATE_KEY, JSON.stringify(submenuStates));
        });
    });

    // ── Active link highlight ─────────────────────────
    const currentPath = window.location.pathname;
    document.querySelectorAll('.sidebar .nav-link').forEach(link => {
        if (link.getAttribute('href') === currentPath) {
            link.classList.add('active');
            const parentCollapse = link.closest('.collapse');
            if (parentCollapse) {
                parentCollapse.classList.add('show');
                const trigger = document.querySelector(`[href="#${parentCollapse.id}"]`);
                if (trigger) trigger.classList.remove('collapsed');
            }
        }
    });

    // ── Initialize Bootstrap Tooltips ─────────────────
    const tooltipTriggerList = document.querySelectorAll('[data-bs-toggle="tooltip"]');
    const tooltipList = [...tooltipTriggerList].map(tooltipTriggerEl => new bootstrap.Tooltip(tooltipTriggerEl, {
        trigger: 'hover'
    }));
});