// claim-create.js
// Frontend validation và submit form tạo Claim.

// ── CSRF token ────────────────────────────────────────────
const csrfToken  = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

// ── Cấu hình từng loại code ──────────────────────────────
const CODE_CONFIG = {
    diagnosis: { max: 10, containerId: 'diagnosisRows', countId: 'diagnosisCount', btnId: 'addDiagnosisBtn', placeholder: 'VD: 41401' },
    procedure: { max: 6,  containerId: 'procedureRows', countId: 'procedureCount', btnId: 'addProcedureBtn', placeholder: 'VD: 8154'  },
    hcpcs:     { max: 45, containerId: 'hcpcsRows',     countId: 'hcpcsCount',     btnId: 'addHcpcsBtn',     placeholder: 'VD: G0008' }
};

// ── Thêm dòng nhập code ───────────────────────────────────
function addRow(type, value = '', autoFocus = true) {
    const cfg       = CODE_CONFIG[type];
    const container = document.getElementById(cfg.containerId);
    if (container.querySelectorAll('.code-row').length >= cfg.max) return;

    const row = document.createElement('div');
    row.className = 'code-row d-flex gap-2 mb-2';
    row.innerHTML = `
        <input type="text" class="form-control form-control-sm"
               placeholder="${cfg.placeholder}" value="${value}" data-type="${type}">
        <button type="button" class="btn btn-sm btn-outline-danger flex-shrink-0"
                onclick="removeRow(this, '${type}')" title="Xóa">
            <i class="bi bi-trash3"></i>
        </button>
    `;
    container.appendChild(row);
    updateCount(type);
    if (type === 'procedure') syncOpNpiHint();
    if (autoFocus) row.querySelector('input').focus();

    // Lắng nghe thay đổi để revalidate OP NPI khi xóa hết procedure
    row.querySelector('input').addEventListener('input', () => {
        if (type === 'procedure') syncOpNpiHint();
    });
}

// ── Xóa dòng ─────────────────────────────────────────────
function removeRow(btn, type) {
    btn.closest('.code-row').remove();
    updateCount(type);
    if (type === 'procedure') {
        syncOpNpiHint();
        // Khi xóa hết procedure, bỏ lỗi OP NPI nếu đang hiển thị
        if (getCodeList('procedure').length === 0) {
            clearFieldError('opPhysnNpi');
        }
    }
}

// ── Cập nhật counter ──────────────────────────────────────
function updateCount(type) {
    const cfg   = CODE_CONFIG[type];
    const count = document.getElementById(cfg.containerId).querySelectorAll('.code-row').length;
    document.getElementById(cfg.countId).textContent = `(${count}/${cfg.max})`;
    document.getElementById(cfg.btnId).disabled      = count >= cfg.max;
}

// ── Lấy danh sách code từ DOM ─────────────────────────────
function getCodeList(type) {
    return Array.from(document.getElementById(CODE_CONFIG[type].containerId).querySelectorAll('input'))
        .map(el => el.value.trim().toUpperCase())
        .filter(v => v !== '');
}

// ── Đồng bộ gợi ý OP NPI khi thêm/xóa procedure ─────────
function syncOpNpiHint() {
    // Chỉ đồng bộ validation để khi thêm thủ thuật thì mất báo đỏ nếu thỏa mãn
    validateOpNpi();
}

// Lắng nghe sự kiện input trên các ô nhập procedure được sinh ra động
document.getElementById('procedureRows')?.addEventListener('input', function() {
    validateOpNpi();
});

// ── Helpers lấy giá trị form ──────────────────────────────
const str = id => document.getElementById(id)?.value?.trim() || null;
const num = id => { const v = str(id); return v !== null ? parseFloat(v) : null; };
const int = id => { const v = str(id); return v !== null ? parseInt(v, 10) : null; };

// ── Build payload (không có clmId và segment — server tự sinh) ────────────
function buildPayload() {
    return {
        desynpufId:                str('desynpufId'),
        prvdrNum:                  str('prvdrNum'),
        clmAdmsnDt:                str('clmAdmsnDt'),
        nchBeneDschrgDt:           str('nchBeneDschrgDt'),
        clmFromDt:                 str('clmFromDt'),
        clmThruDt:                 str('clmThruDt'),
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
        diagnoses:                 getCodeList('diagnosis'),
        procedures:                getCodeList('procedure'),
        hcpcsCodes:                getCodeList('hcpcs')
    };
}

// ── Hiển thị alert toàn form ──────────────────────────────
function showAlert(type, msg) {
    const el = document.getElementById('formAlert');
    const icon = type === 'danger' ? 'exclamation-triangle-fill' : 'check-circle-fill';
    el.className = `alert alert-${type} d-flex align-items-center gap-2`;
    el.innerHTML = `<i class="bi bi-${icon} flex-shrink-0"></i><span>${msg}</span>`;
    el.classList.remove('d-none');
    el.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

// ── Hiển thị/xóa lỗi một field ───────────────────────────
function setFieldError(fieldId, feedbackId, message) {
    const input = document.getElementById(fieldId);
    if (!input) return;
    input.classList.add('is-invalid');
    const fb = document.getElementById(feedbackId);
    if (fb && message) fb.textContent = message;
}

function clearFieldError(fieldId) {
    document.getElementById(fieldId)?.classList.remove('is-invalid');
}

function clearAllFieldErrors() {
    document.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
    document.querySelectorAll('.server-feedback').forEach(el => el.remove());
}

// ── Hiển thị lỗi field từ backend ────────────────────────
const FIELD_MAP = {
    prvdrNum: 'prvdrNum', clmPmtAmt: 'clmPmtAmt',
    nchPrmryPyrClmPdAmt: 'nchPrmryPyrClmPdAmt', atPhysnNpi: 'atPhysnNpi',
    opPhysnNpi: 'opPhysnNpi', otPhysnNpi: 'otPhysnNpi',
    clmUtlztnDayCnt: 'clmUtlztnDayCnt', admtngIcd9DgnsCd: 'admtngIcd9DgnsCd',
    clmDrgCd: 'clmDrgCd', clmFromDt: 'clmFromDt', clmThruDt: 'clmThruDt',
    clmAdmsnDt: 'clmAdmsnDt', nchBeneDschrgDt: 'nchBeneDschrgDt',
};

function showFieldErrors(errors) {
    clearAllFieldErrors();
    Object.entries(errors).forEach(([field, message]) => {
        const inputId = FIELD_MAP[field];
        if (!inputId) return;
        const input = document.getElementById(inputId);
        if (!input) return;
        input.classList.add('is-invalid');
        // Tạo feedback tạm nếu không có sẵn
        const existingFb = input.parentElement.querySelector('.invalid-feedback')
            || input.closest('.input-group')?.nextElementSibling;
        if (existingFb?.classList.contains('invalid-feedback')) {
            existingFb.textContent = message;
        } else {
            const fb = document.createElement('div');
            fb.className = 'invalid-feedback d-block server-feedback';
            fb.textContent = message;
            (input.closest('.input-group') || input).after(fb);
        }
    });
    // Cuộn tới field lỗi đầu tiên
    const firstErrorField = Object.keys(errors).find(f => FIELD_MAP[f]);
    if (firstErrorField) {
        document.getElementById(FIELD_MAP[firstErrorField])
            ?.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }
}

// ── Validation ngày (Frontend, bổ sung trước submit) ─────
function validateDates() {
    let valid = true;

    const admsnVal   = str('clmAdmsnDt');
    const dschrgVal  = str('nchBeneDschrgDt');
    const fromVal    = str('clmFromDt');
    const thruVal    = str('clmThruDt');

    // Quy tắc 1: Ngày xuất viện >= ngày nhập viện
    if (admsnVal && dschrgVal && dschrgVal < admsnVal) {
        setFieldError('nchBeneDschrgDt', 'nchBeneDschrgDtFeedback',
            'Ngày xuất viện không được trước ngày nhập viện.');
        valid = false;
    } else {
        clearFieldError('nchBeneDschrgDt');
    }

    // Quy tắc 2: Ngày bắt đầu claim >= ngày nhập viện (nếu nhập)
    if (fromVal && admsnVal && fromVal < admsnVal) {
        setFieldError('clmFromDt', 'clmFromDtFeedback',
            'Ngày bắt đầu claim không được trước ngày nhập viện.');
        valid = false;
    } else {
        clearFieldError('clmFromDt');
    }

    // Quy tắc 3: Ngày kết thúc claim >= ngày xuất viện (nếu nhập)
    if (thruVal && dschrgVal && thruVal < dschrgVal) {
        setFieldError('clmThruDt', 'clmThruDtFeedback',
            'Ngày kết thúc claim không được trước ngày xuất viện.');
        valid = false;
    } else {
        clearFieldError('clmThruDt');
    }

    // Quy tắc 4: Số ngày điều trị <= số ngày nằm viện thực tế (nếu nhập cả 3)
    const dayCntEl = document.getElementById('clmUtlztnDayCnt');
    const dayCnt   = dayCntEl && dayCntEl.value.trim() !== '' ? parseInt(dayCntEl.value, 10) : null;
    if (dayCnt !== null && admsnVal && dschrgVal) {
        const maxDays = Math.round(
            (new Date(dschrgVal) - new Date(admsnVal)) / (1000 * 60 * 60 * 24)
        );
        if (dayCnt > maxDays) {
            setFieldError('clmUtlztnDayCnt', 'clmUtlztnDayCntFeedback',
                `Số ngày điều trị (${dayCnt}) không được lớn hơn số ngày nằm viện thực tế (${maxDays} ngày).`);
            valid = false;
        } else {
            clearFieldError('clmUtlztnDayCnt');
        }
    } else {
        clearFieldError('clmUtlztnDayCnt');
    }

    return valid;
}

// ── Validation OP NPI khi có procedures ──────────────────
function validateOpNpi() {
    const hasProcedures = getCodeList('procedure').length > 0;
    const opNpiVal      = str('opPhysnNpi');
    let valid = true;

    // Chiều 1: Có thủ thuật -> Bắt buộc nhập Bác sĩ
    if (hasProcedures && !opNpiVal) {
        setFieldError('opPhysnNpi', 'opPhysnNpiFeedback',
            'Bắt buộc nhập mã NPI bác sĩ phẫu thuật khi có thủ thuật ICD-9.');
        valid = false;
    } else {
        clearFieldError('opPhysnNpi');
    }

    // Chiều 2: Có Bác sĩ -> Bắt buộc có thủ thuật
    const procedureFeedback = document.getElementById('procedureFeedback');
    if (procedureFeedback) {
        if (opNpiVal && !hasProcedures) {
            procedureFeedback.classList.add('d-block');
            valid = false;
        } else {
            procedureFeedback.classList.remove('d-block');
        }
    }

    return valid;
}

// ── Auto-fill dates khi nhập viện/xuất viện thay đổi ─────
function autoFillDates() {
    const admsnVal  = str('clmAdmsnDt');
    const dschrgVal = str('nchBeneDschrgDt');

    const fromEl   = document.getElementById('clmFromDt');
    const thruEl   = document.getElementById('clmThruDt');
    const dayCntEl = document.getElementById('clmUtlztnDayCnt');

    if (fromEl && admsnVal && !fromEl.value) {
        fromEl.value = admsnVal;
    }
    if (thruEl && dschrgVal && !thruEl.value) {
        thruEl.value = dschrgVal;
    }
    if (dayCntEl && admsnVal && dschrgVal && !dayCntEl.value) {
        const days = Math.round(
            (new Date(dschrgVal) - new Date(admsnVal)) / (1000 * 60 * 60 * 24)
        );
        if (days >= 0) dayCntEl.value = Math.max(days, 1);
    }
}

// ── Event listeners ngày ─────────────────────────────────
document.getElementById('clmAdmsnDt')?.addEventListener('change', function () {
    autoFillDates();
    validateDates();
});
document.getElementById('nchBeneDschrgDt')?.addEventListener('change', function () {
    autoFillDates();
    validateDates();
});
document.getElementById('clmFromDt')?.addEventListener('change', validateDates);
document.getElementById('clmThruDt')?.addEventListener('change', validateDates);
document.getElementById('clmUtlztnDayCnt')?.addEventListener('input', validateDates);
document.getElementById('opPhysnNpi')?.addEventListener('input', function () {
    validateOpNpi();
});

// ── Submit ────────────────────────────────────────────────
document.getElementById('createClaimForm')?.addEventListener('submit', async function (e) {
    e.preventDefault();
    clearAllFieldErrors();

    const htmlValid = this.checkValidity();
    const datesOk   = validateDates();
    const opNpiOk   = validateOpNpi();

    if (!htmlValid) {
        this.classList.add('was-validated');
        showAlert('danger', 'Vui lòng điền đầy đủ các trường bắt buộc được đánh dấu bên dưới.');
        this.querySelector(':invalid')?.scrollIntoView({ behavior: 'smooth', block: 'center' });
        return;
    }
    if (!datesOk) {
        showAlert('danger', 'Kiểm tra lại ràng buộc ngày tháng. Xem chi tiết bên dưới từng trường.');
        return;
    }
    if (!opNpiOk) {
        showAlert('danger', 'Kiểm tra lại liên kết giữa Bác sĩ phẫu thuật và Mã thủ thuật ICD-9.');
        
        // Cuộn tới vị trí lỗi
        if (str('opPhysnNpi') && getCodeList('procedure').length === 0) {
            document.getElementById('procedureFeedback')?.scrollIntoView({ behavior: 'smooth', block: 'center' });
        } else {
            document.getElementById('opPhysnNpi')?.scrollIntoView({ behavior: 'smooth', block: 'center' });
        }
        return;
    }

    // Tất cả validation pass — gửi request
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
            return;
        }

        // Lỗi validation từ backend
        if (res.status === 400 && json.data && typeof json.data === 'object') {
            showAlert('danger', 'Có lỗi xác thực. Kiểm tra lại từng trường bên dưới.');
            showFieldErrors(json.data);
        } else {
            showAlert('danger', json.message || 'Đã xảy ra lỗi. Vui lòng thử lại.');
        }
    } catch {
        showAlert('danger', 'Không thể kết nối đến máy chủ. Vui lòng kiểm tra kết nối mạng.');
    } finally {
        btn.disabled = false;
        btn.innerHTML = '<i class="bi bi-check-circle me-1"></i>Tạo Claim';
    }
});