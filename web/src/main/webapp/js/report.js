/*
 * The report's what-if controls and Export PDF. Moving a control re-splits each recipient row
 * under that policy, as the reference's allocForBatch does, and saves nothing. The formats are
 * ReportFormat's - keep the two in sync.
 */
(function () {
    'use strict';

    var r2 = C3S.round2;

    // each part of the split and the allocation bar segment that shows it
    var SEGMENTS = {processing: 'rp-seg-fee', restricted: 'rp-seg-res', unrestricted: 'rp-seg-unres'};

    function money0(n) {
        return '$' + Math.round(n).toLocaleString('en-US');
    }

    function pctOf(part, whole) {
        return whole > 0 ? (part / whole * 100) : 0;
    }

    function perDollar(part, whole) {
        return pctOf(part, whole).toFixed(1) + '¢';
    }

    function deduction(n) {
        return n > 0 ? '−' + money0(n) : '$0';
    }

    // one recipient's gifts under a policy: the reference's allocForBatch
    function allocForBatch(gross, tx, policy) {
        var fee = r2(gross * (policy.feePct / 100) + policy.feeFixed * tx);
        var split = C3S.feeSplit(gross, fee, policy.feeModel);
        var unrestricted = r2(split.net * (policy.unrestrictedPct / 100));
        var restricted = r2(split.net - unrestricted);
        return {fee: fee, charge: split.charge, processing: split.processing, restricted: restricted,
            unrestricted: unrestricted};
    }

    function initReport() {
        var page = document.getElementById('page-report');
        if (!page) {
            return;
        }
        var slider = document.getElementById('rp-slider');
        var sliderVal = document.getElementById('rp-slider-val');
        var modelButtons = Array.prototype.slice.call(page.querySelectorAll('.fee-seg button[data-model]'));
        var rows = Array.prototype.slice.call(page.querySelectorAll('#alloc-table-body tr[data-gross]'));
        var minLabelPct = parseFloat(page.getAttribute('data-min-label-pct')) || 0;

        var activeButton = page.querySelector('.fee-seg button.active');
        var policy = {
            feeModel: activeButton ? activeButton.getAttribute('data-model') : 'deducted',
            feePct: parseFloat(page.getAttribute('data-fee-pct')) || 0,
            feeFixed: parseFloat(page.getAttribute('data-fee-fixed')) || 0,
            unrestrictedPct: slider ? parseInt(slider.value, 10) || 0 : 0
        };

        function setText(name, text) {
            page.querySelectorAll('[data-field="' + name + '"]').forEach(function (el) {
                el.textContent = text;
            });
        }

        // the same data-bar-* attributes the JSP renders, then progress-bars.js sizes them
        function fillSegment(seg, pct) {
            seg.setAttribute('data-bar-pct', pct.toFixed(2));
            seg.setAttribute('data-bar-min-px', pct > 0 ? 3 : 0);
            seg.textContent = pct >= minLabelPct ? pct.toFixed(1) + '%' : '';
        }

        function recalculate() {
            var t = {fee: 0, charge: 0, processing: 0, restricted: 0, unrestricted: 0};
            rows.forEach(function (row) {
                var gross = parseFloat(row.getAttribute('data-gross')) || 0;
                var tx = parseInt(row.getAttribute('data-tx'), 10) || 0;
                var a = allocForBatch(gross, tx, policy);
                t.fee += a.fee;
                t.charge += a.charge;
                t.processing += a.processing;
                t.restricted += a.restricted;
                t.unrestricted += a.unrestricted;
                row.querySelector('[data-cell="charge"]').textContent = money0(a.charge);
                row.querySelector('[data-cell="processing"]').textContent = deduction(a.processing);
                row.querySelector('[data-cell="restricted"]').textContent = money0(a.restricted);
                row.querySelector('[data-cell="unrestricted"]').textContent = money0(a.unrestricted);
            });

            setText('charge', money0(t.charge));
            setText('fee', money0(t.fee));
            setText('processing-deduction', deduction(t.processing));
            ['processing', 'restricted', 'unrestricted'].forEach(function (part) {
                var cents = perDollar(t[part], t.charge);
                var share = pctOf(t[part], t.charge);
                setText(part, money0(t[part]));
                setText(part + '-badge', cents + ' of every $1');
                setText(part + '-per', cents + ' per $1');
                setText(part + '-cents', cents);
                setText(part + '-share', share.toFixed(1) + '%');
                var seg = document.getElementById(SEGMENTS[part]);
                if (seg) {
                    fillSegment(seg, share);
                }
            });
            C3S.applyBarWidths(page);

            setText('restricted-pct', (100 - policy.unrestrictedPct) + '%');
            setText('unrestricted-pct', policy.unrestrictedPct + '%');
            if (sliderVal) {
                sliderVal.textContent = policy.unrestrictedPct + '%';
            }
            modelButtons.forEach(function (b) {
                var active = b.getAttribute('data-model') === policy.feeModel;
                b.classList.toggle('active', active);
                if (active) {
                    setText('model-label', b.getAttribute('data-label'));
                }
            });
            var absorbed = policy.feeModel === 'absorbed';
            page.querySelectorAll('[data-when]').forEach(function (el) {
                el.hidden = el.getAttribute('data-when') === 'absorbed' ? !absorbed : absorbed;
            });
        }

        if (slider) {
            slider.addEventListener('input', function () {
                var max = parseInt(slider.max, 10) || 50;
                policy.unrestrictedPct = Math.max(0, Math.min(max, parseInt(slider.value, 10) || 0));
                recalculate();
            });
        }
        modelButtons.forEach(function (b) {
            b.addEventListener('click', function () {
                policy.feeModel = b.getAttribute('data-model');
                recalculate();
            });
        });

        var print = page.querySelector('[data-print]');
        if (print) {
            print.addEventListener('click', function () {
                window.print();
            });
        }
    }

    C3S.ready(initReport);
})();
