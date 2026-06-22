const POLL_MS = 3000;
const MAX_POLL = 20;

let pollTimer = null;
let pollCount = 0;

function getCsrfHeaders() {
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;

    const headers = {};
    if (csrfHeader && csrfToken) {
        headers[csrfHeader] = csrfToken;
    }
    return headers;
}

async function fetchPrediction() {
    showState('pending');
    try {
        const res = await fetch(`/api/predictions/${window.CLAIM_ID}/latest`);
        const json = await res.json();

        if (json.success && json.data) {
            renderPrediction(json.data);
            stopPolling();
        } else if (json.message === 'PENDING') {
            startPolling();
        } else {
            showError();
        }
    } catch (e) {
        showError();
    }
}

async function requestPrediction(button) {
    const oldHtml = button?.innerHTML;

    if (button) {
        button.disabled = true;
        button.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span>Đang phân tích...';
    }

    stopPolling();
    showState('pending');

    try {
        const res = await fetch(`/api/predictions/predict/${window.CLAIM_ID}`, {
            method: 'POST',
            headers: getCsrfHeaders()
        });

        const text = await res.text();
        let json = null;
        try {
            json = text ? JSON.parse(text) : null;
        } catch (e) {
            throw new Error(`HTTP ${res.status}: ${text}`);
        }

        if (!res.ok || !json?.success) {
            throw new Error(json?.message || `Predict failed. HTTP ${res.status}`);
        }

        if (json.data) {
            renderPrediction(json.data);
            stopPolling();
        } else {
            pollCount = 0;
            startPolling();
        }
    } catch (e) {
        showError(e.message);
    } finally {
        if (button) {
            button.disabled = false;
            button.innerHTML = oldHtml;
        }
    }
}

function renderPrediction(data) {
    const isAnomaly = data.predictedLabel === 'ANOMALY';
    const risk = data.riskPercentage ?? 0;

    const labelEl = document.getElementById('mlLabel');
    if (labelEl) {
        labelEl.textContent = isAnomaly ? 'ANOMALY' : 'NORMAL';
        labelEl.className = `badge fs-6 px-3 py-2 ${isAnomaly ? 'bg-danger' : 'bg-success'}`;
    }

    const riskEl = document.getElementById('mlRisk');
    if (riskEl) {
        riskEl.textContent = risk.toFixed(1) + '%';
    }

    const bar = document.getElementById('mlProgressBar');
    if (bar) {
        bar.style.width = risk + '%';
        bar.className = `progress-bar ${
            risk >= 70 ? 'bg-danger' :
            risk >= 50 ? 'bg-warning' : 'bg-success'
        }`;
    }

    const alertEl = document.getElementById('mlAlert');
    if (alertEl) {
        alertEl.classList.toggle('d-none', !data.shouldAlert);
    }

    const anomalyScoreEl = document.getElementById('mlAnomalyScore');
    if (anomalyScoreEl) {
        anomalyScoreEl.textContent = data.anomalyScore?.toFixed(4) ?? '-';
    }

    const methodEl = document.getElementById('mlMethod');
    if (methodEl) {
        methodEl.textContent = data.modelSelected ?? data.shapMethod ?? 'SHAP';
    }

    const versionEl = document.getElementById('mlVersion');
    if (versionEl) {
        versionEl.textContent = data.modelVersion ?? '—';
    }

    const shapFactorsEl = document.getElementById('shapFactors');
    if (shapFactorsEl) {
        shapFactorsEl.innerHTML = (data.topFactors ?? []).map((f, i) => {
            const isIncrease = f.direction?.includes('increase');
            return `
            <div class="d-flex align-items-center gap-2 mb-2">
                <span class="badge bg-secondary">${i + 1}</span>
                <div class="flex-grow-1">
                    <div class="small fw-semibold">${f.feature}</div>
                    <div class="text-muted" style="font-size:0.75rem;">
                        Giá trị: ${f.value} &nbsp;|&nbsp;
                        Ảnh hưởng: ${f.impact?.toFixed(3)}
                    </div>
                </div>
                <span class="badge ${isIncrease ? 'bg-danger' : 'bg-success'}">
                    ${isIncrease ? '↑' : '↓'}
                </span>
            </div>
        `}).join('');
    }

    if (data.predictedAt) {
        const predictedAtEl = document.getElementById('mlPredictedAt');
        if (predictedAtEl) {
            predictedAtEl.textContent = new Date(data.predictedAt).toLocaleString('vi-VN');
        }
    }

    showState('result');
}

function startPolling() {
    if (pollTimer) return;

    pollTimer = setInterval(() => {
        pollCount++;
        if (pollCount >= MAX_POLL) {
            stopPolling();
            showError();
            return;
        }
        fetchPrediction();
    }, POLL_MS);
}

function stopPolling() {
    if (pollTimer) {
        clearInterval(pollTimer);
        pollTimer = null;
    }
}

function showState(state) {
    const states = {
        mlPending: state !== 'pending',
        mlResult: state !== 'result',
        mlError: state !== 'error'
    };

    Object.entries(states).forEach(([id, shouldHide]) => {
        document.getElementById(id)?.classList.toggle('d-none', shouldHide);
    });
}

function showError(message) {
    const errorText = document.querySelector('#mlError .text-muted.small');
    if (errorText) {
        errorText.textContent = message || 'Khong the lay ket qua phan tich';
    }
    showState('error');
}

function retryPrediction(button) {
    requestPrediction(button);
}

function initRePredict() {
    const btn = document.getElementById('rePredictBtn');
    if (!btn) return;

    btn.addEventListener('click', async function () {
        await requestPrediction(this);
    });
}

function initAssign() {
    document.querySelectorAll('.assign-btn, .assign-btn-detail, #assignBtn').forEach(btn => {
        btn.addEventListener('click', async function () {
            const claimId = this.dataset.claimId;
            const oldHtml = this.innerHTML;

            this.disabled = true;
            this.innerHTML = '<span class="spinner-border spinner-border-sm"></span>';

            try {
                const res = await fetch(`/api/claims/${claimId}/assign`, {
                    method: 'POST',
                    headers: getCsrfHeaders()
                });

                const text = await res.text();
                let json = null;
                try {
                    json = text ? JSON.parse(text) : null;
                } catch (e) {
                    throw new Error(`HTTP ${res.status}: ${text}`);
                }

                if (!res.ok || !json?.success) {
                    throw new Error(json?.message || `Assign failed. HTTP ${res.status}`);
                }

                window.location.reload();
            } catch (e) {
                alert(e.message);
                this.disabled = false;
                this.innerHTML = oldHtml;
            }
        });
    });
}

// ── INVESTIGATION ACTIONS ──────────────
function initInvestigationActions() {
    const approveBtn = document.getElementById('approveBtn');
    const rejectBtn  = document.getElementById('rejectBtn');
    const addNoteBtn = document.getElementById('addNoteBtn');

    if (approveBtn) {
        approveBtn.addEventListener('click', () =>
            performAction('APPROVE', ''));
    }

    if (rejectBtn) {
        rejectBtn.addEventListener('click', () => {
            const note = document.getElementById('investigationNote')?.value?.trim();
            if (!note) {
                alert('Vui lòng nhập lý do từ chối');
                document.getElementById('investigationNote').focus();
                return;
            }
            performAction('REJECT', note);
        });
    }

    if (addNoteBtn) {
        addNoteBtn.addEventListener('click', () => {
            const note = document.getElementById('investigationNote')?.value?.trim();
            if (!note) {
                alert('Vui lòng nhập ghi chú');
                return;
            }
            performAction('NOTE', note);
        });
    }
}

async function performAction(action, note) {
    const btn = document.getElementById(
        action === 'APPROVE' ? 'approveBtn' :
        action === 'REJECT'  ? 'rejectBtn'  : 'addNoteBtn'
    );
    if (btn) {
        btn.dataset.originalHtml = btn.innerHTML;
        btn.disabled = true;
        btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span>Đang xử lý...';
    }

    try {
        const res  = await fetch(`/api/investigation/claims/${window.CLAIM_ID}/action`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                ...getCsrfHeaders()
            },
            body: JSON.stringify({ action, note })
        });
        const json = await res.json();

        if (json.success) {
            if (action !== 'NOTE') {
                // APPROVE/REJECT → reload để cập nhật status
                location.reload();
            } else {
                // NOTE → chỉ reload lịch sử
                document.getElementById('investigationNote').value = '';
                loadHistory();
            }
        } else {
            alert(json.message);
            if (btn) {
                btn.disabled = false;
                btn.innerHTML = action === 'APPROVE'
                    ? '<i class="bi bi-check-circle me-1"></i>Duyệt hợp lệ'
                    : action === 'REJECT'
                    ? '<i class="bi bi-x-circle me-1"></i>Từ chối (Gian lận)'
                    : '<i class="bi bi-sticky me-1"></i>Lưu ghi chú';
            }
        }
    } catch (e) {
        alert('Có lỗi xảy ra, vui lòng thử lại');
    }
}

// ── INVESTIGATION HISTORY ──────────────
async function loadHistory() {
    const container = document.getElementById('investigationHistory');
    if (!container) return;

    try {
        const res  = await fetch(`/api/investigation/claims/${window.CLAIM_ID}/history`);
        const json = await res.json();

        if (json.success && json.data.length > 0) {
            container.innerHTML = json.data.map(h => {
                const actionConfig = {
                    'APPROVE': { label: 'Duyệt',    cls: 'text-success', icon: 'bi-check-circle-fill' },
                    'REJECT':  { label: 'Từ chối',  cls: 'text-danger',  icon: 'bi-x-circle-fill'     },
                    'NOTE':    { label: 'Ghi chú',  cls: 'text-secondary',icon: 'bi-sticky-fill'      },
                };
                const cfg = actionConfig[h.action] ?? { label: h.action, cls: 'text-muted', icon: 'bi-circle' };

                return `
                    <div class="d-flex gap-3 px-3 py-2 border-bottom align-items-start">
                        <div style="width: 80px; flex-shrink: 0;" class="text-start pt-1">
                            <span class="small fw-semibold ${cfg.cls}">
                                <i class="bi ${cfg.icon} me-1"></i>${cfg.label}
                            </span>
                        </div>
                        <div class="flex-grow-1">
                            <div class="small fw-semibold">${h.investigatorName}</div>
                            ${h.note
                                ? `<div class="small text-muted mt-1">${h.note}</div>`
                                : ''}
                            <div class="text-muted mt-1" style="font-size:0.72rem;">
                                ${new Date(h.createdAt).toLocaleString('vi-VN')}
                            </div>
                        </div>
                    </div>
                `;
            }).join('');
        } else {
            container.innerHTML =
                '<div class="text-center py-3 text-muted small">Chưa có lịch sử</div>';
        }
    } catch (e) {
        container.innerHTML =
            '<div class="text-center py-3 text-muted small">Không thể tải lịch sử</div>';
    }
}

document.addEventListener('DOMContentLoaded', () => {
    if (document.getElementById('mlCard')) {
        fetchPrediction();
    }
    initRePredict();
    initAssign();
    initInvestigationActions();
    loadHistory();
});
