document.addEventListener('DOMContentLoaded', () => {
    initFilter();
    loadRiskScores();
    initAssign();
});

// ── FILTER ─────────────────────────────
function initFilter() {
    const searchInput  = document.getElementById('searchInput');
    const statusFilter = document.getElementById('statusFilter');

    function filterTable() {
        const search = searchInput.value.toLowerCase();
        const status = statusFilter.value;

        document.querySelectorAll('.claim-row').forEach(row => {
            const matchSearch = row.dataset.search.toLowerCase().includes(search);
            const matchStatus = !status || row.dataset.status === status;
            row.style.display = matchSearch && matchStatus ? '' : 'none';
        });
    }

    window.resetFilter = function () {
        searchInput.value  = '';
        statusFilter.value = '';
        filterTable();
    };

    searchInput.addEventListener('input', filterTable);
    statusFilter.addEventListener('change', filterTable);
}

// ── RISK SCORE ─────────────────────────
function loadRiskScores() {
    const riskCells = document.querySelectorAll('[id^="risk-"]');

    if (riskCells.length === 0) return;

    riskCells.forEach(cell => loadSingleRisk(cell));
}

async function loadSingleRisk(cell) {
    const claimId = cell.id.replace('risk-', '');

    try {
        const res  = await fetch(`/api/predictions/${claimId}/latest`);
        const json = await res.json();

        if (json.success && json.data) {
            renderRisk(cell, json.data);
        } else {
            cell.innerHTML = `<span class="text-muted small fst-italic">Đang xử lý...</span>`;
        }
    } catch (e) {
        cell.innerHTML = `<span class="text-muted small">—</span>`;
    }
}

function renderRisk(cell, data) {
    const risk      = data.riskPercentage ?? 0;
    const isAnomaly = data.predictedLabel === 'ANOMALY';

    const badgeClass = risk >= 70 ? 'bg-danger'
                     : risk >= 50 ? 'bg-warning text-dark'
                     : 'bg-success';

    cell.innerHTML = `
        <span class="badge ${badgeClass}">
            ${risk.toFixed(1)}%
        </span>
        <div class="small text-muted" style="font-size:0.7rem;">
            ${isAnomaly ? '⚠ ANOMALY' : '✓ NORMAL'}
        </div>
    `;
}

// ── ASSIGN CLAIM ───────────────────────
function initAssign() {
    document.querySelectorAll('.assign-btn').forEach(btn => {
        btn.addEventListener('click', async function () {
            const claimId = this.dataset.claimId;
            const oldHtml = this.innerHTML;

            this.disabled = true;
            this.innerHTML = '<span class="spinner-border spinner-border-sm"></span>';

            try {
                const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
                const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;

                const headers = {};
                if (csrfHeader && csrfToken) {
                    headers[csrfHeader] = csrfToken;
                }

                const res = await fetch(`/api/claims/${claimId}/assign`, {
                    method: 'POST',
                    headers: headers
                });

                const text = await res.text();
                let json = null;

                try {
                    json = text ? JSON.parse(text) : null;
                } catch (e) {
                    throw new Error(`HTTP ${res.status}: ${text}`);
                }

                if (!res.ok) {
                    throw new Error(json?.message || json?.error || `Assign thất bại. HTTP ${res.status}`);
                }

                if (!json.success) {
                    throw new Error(json.message || 'Assign thất bại');
                }

                this.closest('td').innerHTML = `
                    <span class="small text-success fw-semibold">
                        <i class="bi bi-person-check me-1"></i>
                        ${json.data.investigatorName}
                    </span>
                `;
            } catch (e) {
                alert(e.message);
                this.disabled = false;
                this.innerHTML = oldHtml;
            }
        });
    });
}

function getCsrfToken() {
    return document.cookie
        .split('; ')
        .find(r => r.startsWith('XSRF-TOKEN='))
        ?.split('=')[1] ?? '';
}

