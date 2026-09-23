/*
 * C3S.confirm({...}) - the "are you sure?" dialog. Needs the #confirm-modal markup.
 *
 *   C3S.confirm({title: 'Archive Education?', body: 'This removes it from the active list.',
 *                confirmLabel: 'Archive', confirmClass: 'tb-btn-danger', onConfirm: fn});
 *
 * A [data-archive] button inside a form asks first, using the form's data-confirm-title and
 * data-confirm-body, and submits on Confirm.
 */
(function () {
    'use strict';

    var callback = null;

    function overlay() {
        return document.getElementById('confirm-modal');
    }

    function close() {
        var o = overlay();
        if (o) {
            o.classList.remove('show');
        }
        callback = null;
    }

    C3S.confirm = function (opts) {
        var o = overlay();
        if (!o) {
            return;
        }
        document.getElementById('confirm-modal-title').textContent = opts.title || 'Are you sure?';
        document.getElementById('confirm-modal-body').textContent = opts.body || '';
        var confirmBtn = document.getElementById('confirm-modal-confirm');
        confirmBtn.textContent = opts.confirmLabel || 'Confirm';
        confirmBtn.className = 'tb-btn ' + (opts.confirmClass || 'tb-btn-primary');
        callback = typeof opts.onConfirm === 'function' ? opts.onConfirm : null;
        o.classList.add('show');
    };

    C3S.ready(function initConfirmDialog() {
        var o = overlay();
        if (!o) {
            return;
        }
        document.getElementById('confirm-modal-confirm').addEventListener('click', function () {
            var run = callback;
            close();
            if (run) {
                run();
            }
        });
        document.getElementById('confirm-modal-cancel').addEventListener('click', close);
        o.addEventListener('click', function (e) {
            if (e.target === o) {
                close();
            }
        });
        document.addEventListener('keydown', function (e) {
            if (e.key === 'Escape') {
                close();
            }
        });

        document.addEventListener('click', function (e) {
            var archive = e.target.closest('[data-archive]');
            var form = archive && archive.closest('form');
            if (!form) {
                return;
            }
            C3S.confirm({
                title: form.dataset.confirmTitle,
                body: form.dataset.confirmBody,
                confirmLabel: 'Archive',
                confirmClass: 'tb-btn-danger',
                onConfirm: function () {
                    form.submit();
                }
            });
        });
    });
})();
