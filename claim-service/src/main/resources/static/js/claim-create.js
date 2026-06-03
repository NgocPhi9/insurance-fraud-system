// claim-create.js

// ── CSRF token ────────────────────────────────────────────
const csrfToken  = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

// ── Cấu hình từng loại code ──────────────────────────────
const CODE_CONFIG = {
    diagnosis: { max: 10, containerId: 'diagnosisRows', countId: 'diagnosisCount', btnId: 'addDiagnosisBtn', placeholder: 'VD: 41401' },
    procedure: { max: 6,  containerId: 'procedureRows', countId: 'procedureCount', btnId: 'addProcedureBtn', placeholder: 'VD: 8154' },
    hcpcs:     { max: 45, containerId: 'hcpcsRows',     countId: 'hcpcsCount',     btnId: 'addHcpcsBtn',     placeholder: 'VD: G0008' }
};

// ── Thêm dòng nhập ────────────────────────────────────────
function addRow(type, value = '', autoFocus = true) {
    const cfg       = CODE_CONFIG[type];
    const container = document.getElementById(cfg.containerId);
    const current   = container.querySelectorAll('.code-row').length;
    if (current >= cfg.max) return;

    const row = document.createElement('div');
    row.className = 'code-row';
    row.innerHTML = `
        <input type="text" class="form-control form-control-sm"
               placeholder="${cfg.placeholder}"
               value="${value}"
               data-type="${type}">
        <button type="button" class="btn btn-sm btn-outline-danger flex-shrink-0"
                onclick="removeRow(this, '${type}')" title="Xóa">
            <i class="bi bi-trash3"></i>
        </button>
    `;
    container.appendChild(row);
    updateCount(type);
    if (autoFocus) row.querySelector('input').focus();
}

// ── Xóa dòng ─────────────────────────────────────────────
function removeRow(btn, type) {
    btn.closest('.code-row').remove();
    updateCount(type);
}

// ── Cập nhật counter ──────────────────────────────────────
function updateCount(type) {
    const cfg       = CODE_CONFIG[type];
    const container = document.getElementById(cfg.containerId);
    const count     = container.querySelectorAll('.code-row').length;
    document.getElementById(cfg.countId).textContent = `(${count}/${cfg.max})`;
    document.getElementById(cfg.btnId).disabled = count >= cfg.max;
}

// ── Lấy danh sách code từ DOM ─────────────────────────────
function getCodeList(type) {
    const container = document.getElementById(CODE_CONFIG[type].containerId);
    return Array.from(container.querySelectorAll('input'))
        .map(el => el.value.trim().toUpperCase())
        .filter(v => v !== '');
}

// ── Helpers lấy giá trị form ──────────────────────────────
const str = id => document.getElementById(id)?.value?.trim() || null;
const num = id => { const v = str(id); return v ? parseFloat(v) : null; };
const int = id => { const v = str(id); return v ? parseInt(v, 10) : null; };

// ── Build payload ─────────────────────────────────────────
function buildPayload() {
    const statusEl = document.getElementById('claimStatus');
    return {
        clmId:                     str('clmId'),
        desynpufId:                str('desynpufId'),
        prvdrNum:                  str('prvdrNum'),
        segment:                   str('segment'),
        clmFromDt:                 str('clmFromDt'),
        clmThruDt:                 str('clmThruDt'),
        clmAdmsnDt:                str('clmAdmsnDt'),
        nchBeneDschrgDt:           str('nchBeneDschrgDt'),
        clmUtlztnDayCnt:           int('clmUtlztnDayCnt'),
        clmDrgCd:                  str('clmDrgCd'),
        admtngIcd9DgnsCd:          str('admtngIcd9DgnsCd'),
        atPhysnNpi:                str('atPhysnNpi'),
        opPhysnNpi:                str('opPhysnNpi'),
        otPhysnNpi:                str('otPhysnNpi'),
        clmPmtAmt:                 num('clmPmtAmt'),
        nchPrmryPyrClmPdAmt:       num('nchPrmryPyrClmPdAmt'),
        nchBeneIpDdctblAmt:        num('nchBeneIpDdctblAmt'),
        nchBenePtaCoinsrncLbltyAm: num('nchBenePtaCoinsrncLbltyAm'),
        nchBeneBloodDdctblLbltyAm: num('nchBeneBloodDdctblLbltyAm'),
        clmPassThruPerDiemAmt:     num('clmPassThruPerDiemAmt'),
        investigatorId:            int('investigatorId'),
        claimStatus:               statusEl ? statusEl.value : 'PENDING',
        diagnoses:                 getCodeList('diagnosis'),
        procedures:                getCodeList('procedure'),
        hcpcsCodes:                getCodeList('hcpcs')
    };
}

// ── Alert toàn form ───────────────────────────────────────
function showAlert(type, msg) {
    const el   = document.getElementById('formAlert');
    const icon = type === 'danger' ? 'exclamation-triangle-fill' : 'check-circle-fill';
    el.className = `alert alert-${type} d-flex align-items-center gap-2`;
    el.innerHTML = `<i class="bi bi-${icon}"></i><span>${msg}</span>`;
    el.classList.remove('d-none');
    // Không scroll alert — scroll đến field lỗi thay thế
}

// ── Hiển thị lỗi từng field từ BE ────────────────────────
const FIELD_MAP = {
    clmId: 'clmId', prvdrNum: 'prvdrNum', clmPmtAmt: 'clmPmtAmt',
    nchPrmryPyrClmPdAmt: 'nchPrmryPyrClmPdAmt', atPhysnNpi: 'atPhysnNpi',
    opPhysnNpi: 'opPhysnNpi', otPhysnNpi: 'otPhysnNpi',
    clmUtlztnDayCnt: 'clmUtlztnDayCnt', admtngIcd9DgnsCd: 'admtngIcd9DgnsCd',
    clmDrgCd: 'clmDrgCd', clmFromDt: 'clmFromDt', clmThruDt: 'clmThruDt',
    desynpufId: 'desynpufId', segment: 'segment',
    nchBeneIpDdctblAmt: 'nchBeneIpDdctblAmt',
    nchBenePtaCoinsrncLbltyAm: 'nchBenePtaCoinsrncLbltyAm',
    nchBeneBloodDdctblLbltyAm: 'nchBeneBloodDdctblLbltyAm',
    clmPassThruPerDiemAmt: 'clmPassThruPerDiemAmt',
};

const LIST_FIELD_MAP = {
    procedures: 'procedureRows',
    diagnoses:  'diagnosisRows',
    hcpcsCodes: 'hcpcsRows',
};

function showFieldErrors(errors) {
    clearFieldErrors();
    const firstKey = Object.keys(errors)[0];
    Object.entries(errors).forEach(([field, message]) => {
        if (FIELD_MAP[field]) {
            const input = document.getElementById(FIELD_MAP[field]);
            if (!input) return;
            input.classList.add('is-invalid');
            let fb = document.createElement('div');
            fb.className = 'invalid-feedback d-block server-feedback';
            fb.textContent = message;
            input.closest('.input-group')
                ? input.closest('.input-group').after(fb)
                : input.after(fb);
        } else if (LIST_FIELD_MAP[field]) {
            const container = document.getElementById(LIST_FIELD_MAP[field]);
            if (!container) return;
            let fb = document.getElementById(field + 'Error');
            if (!fb) {
                fb = document.createElement('div');
                fb.id = field + 'Error';
                fb.className = 'text-danger small mt-1 server-feedback';
                container.after(fb);
            }
            fb.innerHTML = `<i class="bi bi-exclamation-circle me-1"></i>${message}`;
        }
    });
    const firstEl = FIELD_MAP[firstKey]
        ? document.getElementById(FIELD_MAP[firstKey])
        : document.getElementById(LIST_FIELD_MAP[firstKey]);
    firstEl?.scrollIntoView({ behavior: 'smooth', block: 'center' });
}

function clearFieldErrors() {
    document.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
    document.querySelectorAll('.server-feedback').forEach(el => el.remove());
}

// ── Validate ngày ─────────────────────────────────────────
function validateDates() {
    const from = document.getElementById('clmFromDt');
    const thru = document.getElementById('clmThruDt');
    if (from.value && thru.value && thru.value < from.value) {
        thru.classList.add('is-invalid');
        return false;
    }
    thru.classList.remove('is-invalid');
    return true;
}
document.getElementById('clmFromDt')?.addEventListener('change', validateDates);
document.getElementById('clmThruDt')?.addEventListener('change', validateDates);

// ── Validate procedures (bắt buộc >= 1) ──────────────────
function validateProcedures() {
    const errEl = document.getElementById('proceduresError');
    if (getCodeList('procedure').length === 0) {
        if (!errEl) {
            const fb = document.createElement('div');
            fb.id = 'proceduresError';
            fb.className = 'text-danger small mt-1 server-feedback';
            fb.innerHTML = '<i class="bi bi-exclamation-circle me-1"></i>Phải có ít nhất 1 mã thủ thuật ICD-9.';
            document.getElementById('procedureRows').after(fb);
        }
        return false;
    }
    errEl?.remove();
    return true;
}

// ── Submit ────────────────────────────────────────────────
document.getElementById('createClaimForm').addEventListener('submit', async function (e) {
    e.preventDefault();
    clearFieldErrors();

    const formValid      = this.checkValidity();
    const datesValid     = validateDates();
    const procedureValid = validateProcedures();

    if (!formValid) {
        this.classList.add('was-validated');
    }
    if (!formValid || !datesValid || !procedureValid) {
        if (!formValid) {
            showAlert('danger', 'Vui lòng điền đầy đủ các trường bắt buộc.');
            // Scroll đến field invalid đầu tiên
            const firstInvalid = document.querySelector('.form-control:invalid, .form-select:invalid');
            firstInvalid?.scrollIntoView({ behavior: 'smooth', block: 'center' });
        } else if (!datesValid) {
            showAlert('danger', 'Ngày kết thúc phải sau hoặc bằng ngày bắt đầu.');
            document.getElementById('clmThruDt')
                    ?.scrollIntoView({ behavior: 'smooth', block: 'center' });
        } else {
            showAlert('danger', 'Phải có ít nhất 1 mã thủ thuật ICD-9.');
            document.getElementById('procedureRows')
                    ?.scrollIntoView({ behavior: 'smooth', block: 'center' });
        }
        return;
    }

    const btn = document.getElementById('submitBtn');
    btn.disabled = true;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span>Đang tạo...';

    try {
        const headers = { 'Content-Type': 'application/json' };
        if (csrfHeader && csrfToken) headers[csrfHeader] = csrfToken;

        const res  = await fetch('/api/claims', {
            method:  'POST',
            headers: headers,
            body:    JSON.stringify(buildPayload())
        });
        const json = await res.json();

        if (res.ok && json.success) {
            showAlert('success', 'Tạo claim thành công! Đang chuyển hướng...');
            setTimeout(() => {
                window.location.href = `/claims/${json.data.rawClaimId}`;
            }, 1000);
        } else if (res.status === 400 && json.data && typeof json.data === 'object') {
            showAlert('danger', 'Vui lòng kiểm tra lại các trường bên dưới.');
            showFieldErrors(json.data);
            btn.disabled = false;
            btn.innerHTML = '<i class="bi bi-check-circle me-1"></i>Tạo Claim';
        } else {
            showAlert('danger', json.message || 'Có lỗi xảy ra, vui lòng thử lại.');
            btn.disabled = false;
            btn.innerHTML = '<i class="bi bi-check-circle me-1"></i>Tạo Claim';
        }
    } catch {
        showAlert('danger', 'Không thể kết nối đến server.');
        btn.disabled = false;
        btn.innerHTML = '<i class="bi bi-check-circle me-1"></i>Tạo Claim';
    }
});

// ── Khởi tạo: 1 dòng procedure sẵn, không focus ─────────
document.addEventListener('DOMContentLoaded', () => {
    addRow('procedure', '', false);
});