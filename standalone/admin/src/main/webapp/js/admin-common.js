function showToast(msg) {
    var toast = document.getElementById('toast');
    if (!toast) {
        return;
    }
    document.getElementById('toast-msg').textContent = msg;
    toast.classList.add('show');
    clearTimeout(showToast._hideTimer);
    showToast._hideTimer = setTimeout(function () {
        toast.classList.remove('show');
    }, 3500);
}

function goTo(page) {
    showToast('Navigation to "' + page + '" is handled by the host application');
}

// Delegated click handlers for the shell:
//   [data-goto]    -> goTo()             (report / donors / browse stubs)
//   [data-archive] -> openArchiveModal   (list-page Archive buttons)
document.addEventListener('click', function (e) {
    var goto = e.target.closest('[data-goto]');
    if (goto) {
        goTo(goto.getAttribute('data-goto'));
        return;
    }
    var archive = e.target.closest('[data-archive]');
    if (archive) {
        var form = archive.closest('form');
        if (form) {
            openArchiveModal(form);
        }
    }
});

/**
 *   openConfirmModal({
 *       title: 'Archive Education?',
 *       body: 'This removes it from the active list.',
 *       confirmLabel: 'Archive',        // default 'Confirm'
 *       confirmClass: 'tb-btn-danger',  // default 'tb-btn-primary'
 *       onConfirm: function () { ... }  // only called if Confirm is clicked
 *   });
 */
var _confirmModalCallback = null;

function openConfirmModal(opts) {
    var overlay = document.getElementById('confirm-modal');
    if (!overlay) {
        return;
    }
    document.getElementById('confirm-modal-title').textContent = opts.title || 'Are you sure?';
    document.getElementById('confirm-modal-body').textContent = opts.body || '';
    var confirmBtn = document.getElementById('confirm-modal-confirm');
    confirmBtn.textContent = opts.confirmLabel || 'Confirm';
    confirmBtn.className = 'tb-btn ' + (opts.confirmClass || 'tb-btn-primary');
    _confirmModalCallback = typeof opts.onConfirm === 'function' ? opts.onConfirm : null;
    overlay.classList.add('show');
}

function closeConfirmModal() {
    var overlay = document.getElementById('confirm-modal');
    if (overlay) {
        overlay.classList.remove('show');
    }
    _confirmModalCallback = null;
}

document.addEventListener('DOMContentLoaded', function () {
    var overlay = document.getElementById('confirm-modal');
    var confirmBtn = document.getElementById('confirm-modal-confirm');
    var cancelBtn = document.getElementById('confirm-modal-cancel');
    if (confirmBtn) {
        confirmBtn.addEventListener('click', function () {
            var callback = _confirmModalCallback;
            closeConfirmModal();
            if (callback) {
                callback();
            }
        });
    }
    if (cancelBtn) {
        cancelBtn.addEventListener('click', closeConfirmModal);
    }
    if (overlay) {
        overlay.addEventListener('click', function (e) {
            if (e.target === overlay) {
                closeConfirmModal();
            }
        });
    }
    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape') {
            closeConfirmModal();
        }
    });
});

// title/body come from data-attrs on the form (escaped server-side), not built here
function openArchiveModal(form) {
    openConfirmModal({
        title: form.dataset.confirmTitle,
        body: form.dataset.confirmBody,
        confirmLabel: 'Archive',
        confirmClass: 'tb-btn-danger',
        onConfirm: function () {
            form.submit();
        }
    });
}

// flash text comes via data-msg (it can contain quotes), not inlined into a <script>
document.addEventListener('DOMContentLoaded', function () {
    var flash = document.querySelector('.flash-toast');
    if (flash && flash.dataset.msg) {
        showToast(flash.dataset.msg);
    }
});

// Bars carry their width in data-pct / data-min-px instead of an inline style, to keep
// the markup CSP-clean. Run on load, and again by anything that rebuilds a bar.
function applyBarStyles(root) {
    (root || document).querySelectorAll('[data-pct]').forEach(function (el) {
        el.style.width = el.dataset.pct + '%';
    });
    (root || document).querySelectorAll('[data-min-px]').forEach(function (el) {
        el.style.minWidth = el.dataset.minPx + 'px';
    });
}

document.addEventListener('DOMContentLoaded', function () {
    applyBarStyles();
});
