/*
 * The allocation policy form: bucket rows, running total, fee model and the live "$50 donation"
 * preview. The maths mirrors AllocationCalculator / AllocationMessageBuilder in Java - keep them
 * in sync; the server recomputes and revalidates on save. The JSPs render every row, segment and
 * sentence slot the page can show, so this only reveals one and sets its text.
 */
(function () {
    'use strict';

    function initAllocationForm() {
        var form = document.getElementById('alloc-form');
        var bucketRows = document.getElementById('bucket-rows');
        if (!form || !bucketRows) {
            return;
        }

        var MIN_LABELLED_SEGMENT_PCT = 13;   // AllocationFormPageData.MIN_LABELLED_SEGMENT_PCT
        var previewRowCount = document.querySelectorAll('#preview-bucket-rows .alloc-row').length;
        var messageSlotCount = document.querySelectorAll('#preview-message .pm-other').length;

        function rows() {
            return Array.prototype.slice.call(bucketRows.querySelectorAll('.bk-row:not([hidden])'));
        }

        function byId(id) {
            return document.getElementById(id);
        }

        // --- preview maths: mirrors AllocationCalculator / AllocationMessageBuilder in Java -
        // keep in sync. Cosmetic only, the server revalidates on save.

        function round2(v) {
            return Math.round(v * 100) / 100;
        }

        function money(v) {
            return '$' + (v + 0).toFixed(2);
        }

        function effectivePurpose(b) {
            if (b.purpose && b.purpose.trim() !== '') {
                return b.purpose.trim();
            }
            if (b.toRecipient) {
                return null;
            }
            if (b.description && b.description.trim() !== '') {
                return b.description.trim();
            }
            return b.name;
        }

        function collectBuckets() {
            var buckets = [];
            rows().forEach(function (row) {
                var nameInput = row.querySelector('.bk-name');
                var pctInput = row.querySelector('.bk-pct-input');
                var purposeInput = row.querySelector('.bk-purpose');
                var descInput = row.querySelector('input[name^="bucketDesc_"]');
                buckets.push({
                    name: nameInput ? nameInput.value : '',
                    pct: parseInt(pctInput.value, 10) || 0,
                    toRecipient: pctInput.getAttribute('data-recipient') === 'true',
                    purpose: purposeInput ? purposeInput.value : '',
                    description: descInput ? descInput.value : ''
                });
            });
            return buckets;
        }

        function compute(amount, feePct, feeFixed, feeModel, buckets) {
            var gift = Math.max(0, amount);
            var fee = gift > 0 ? round2(gift * (feePct / 100) + feeFixed) : 0;

            var charge, processing, net;
            if (feeModel === 'added') {
                charge = round2(gift + fee);
                processing = fee;
                net = gift;
            } else if (feeModel === 'absorbed') {
                charge = gift;
                processing = 0;
                net = gift;
            } else {
                charge = gift;
                processing = Math.min(fee, gift);
                net = round2(gift - processing);
            }

            var total = buckets.reduce(function (s, b) {
                return s + b.pct;
            }, 0);
            var totalOrHundred = total === 0 ? 100 : total;

            var running = 0;
            var results = [];
            buckets.forEach(function (b, i) {
                var isLast = i === buckets.length - 1;
                var amt = isLast ? round2(net - running) : round2(net * (b.pct / totalOrHundred));
                running = round2(running + amt);
                var pctOfCharge = charge > 0 ? (amt / charge * 100) : 0;
                results.push({name: b.name, amount: amt, pctOfCharge: pctOfCharge, pct: b.pct});
            });

            return {gift: gift, fee: fee, charge: charge, processing: processing, net: net, feeModel: feeModel, buckets: results};
        }

        // --- filling the slots the JSP rendered --------------------------------

        function fillSegment(seg, pct, show) {
            if (!seg) {
                return;
            }
            seg.hidden = !show;
            seg.setAttribute('data-bar-pct', pct);
            seg.setAttribute('data-bar-min-px', pct > 0 ? 3 : 0);
            seg.textContent = show && pct >= MIN_LABELLED_SEGMENT_PCT ? pct.toFixed(1) + '%' : '';
        }

        function fillBar(breakdown) {
            var bar = byId('preview-bar');
            if (!bar) {
                return;
            }
            var any = breakdown.buckets.length > 0;
            var feeSegPct = breakdown.charge > 0 ? (breakdown.processing / breakdown.charge * 100) : 0;
            fillSegment(byId('preview-seg-fee'), feeSegPct, any);
            for (var i = 0; i < previewRowCount; i++) {
                var b = breakdown.buckets[i];
                fillSegment(byId('preview-seg-' + i), b ? b.pctOfCharge : 0, !!b);
            }
            C3S.applyBarWidths(bar);
        }

        function fillRows(breakdown) {
            for (var i = 0; i < previewRowCount; i++) {
                var row = byId('preview-row-' + i);
                var b = breakdown.buckets[i];
                if (!row) {
                    continue;
                }
                row.hidden = !b;
                if (b) {
                    byId('preview-row-name-' + i).textContent = b.name;
                    byId('preview-row-share-' + i).textContent = b.pct + '% of net gift';
                    byId('preview-row-amt-' + i).textContent = money(b.amount);
                }
            }
        }

        // The same sentence AllocationMessageBuilder produces, written into the slots
        // allocation-preview.jsp rendered. Each slot carries its own ", and " / "; and ".
        function fillMessage(recipientName, buckets, breakdown) {
            var box = byId('preview-message');
            if (!box) {
                return;
            }
            if (buckets.length === 0 || breakdown.buckets.length === 0) {
                box.hidden = true;
                return;
            }
            box.hidden = false;

            var recipientIndex = 0;
            for (var i = 0; i < buckets.length; i++) {
                if (buckets[i].toRecipient) {
                    recipientIndex = i;
                    break;
                }
            }
            var recipientBucket = buckets[recipientIndex];
            var recipientAmount = breakdown.buckets[recipientIndex];
            var deducted = breakdown.feeModel === 'deducted';

            var fee = byId('pm-fee');
            fee.hidden = !(deducted && breakdown.processing > 0);
            byId('pm-fee-amt').textContent = money(breakdown.processing);

            byId('pm-recipient-pct').textContent = recipientBucket.pct + '%';
            byId('pm-gift-ref').textContent = deducted ? 'your gift' : 'your ' + money(breakdown.gift) + ' gift';
            byId('pm-recipient-amt').textContent = money(recipientAmount.amount);
            byId('pm-who').textContent = recipientName && recipientName.trim() !== '' ? recipientName : 'the recipient';

            var slot = 0;
            buckets.forEach(function (b, i) {
                if (i === recipientIndex || b.pct <= 0 || slot >= messageSlotCount) {
                    return;
                }
                byId('pm-other-pct-' + slot).textContent = b.pct + '%';
                byId('pm-other-amt-' + slot).textContent = money(breakdown.buckets[i].amount);
                byId('pm-other-purpose-' + slot).textContent = effectivePurpose(b);
                byId('pm-other-' + slot).hidden = false;
                slot++;
            });
            for (; slot < messageSlotCount; slot++) {
                byId('pm-other-' + slot).hidden = true;
            }
        }

        var previewTimer = null;

        // debounced - one refresh after typing pauses, not one per keystroke
        function updatePreview() {
            clearTimeout(previewTimer);
            previewTimer = setTimeout(updatePreviewNow, 80);
        }

        function updatePreviewNow() {
            var feePctEl = byId('feePct');
            var feeFixedEl = byId('feeFixed');
            var feeModelEl = byId('fee-model-input');
            if (!feePctEl || !feeFixedEl || !feeModelEl) {
                return;
            }

            var buckets = collectBuckets();
            var feePct = parseFloat(feePctEl.value) || 0;
            var feeFixed = parseFloat(feeFixedEl.value) || 0;
            var breakdown = compute(50, feePct, feeFixed, feeModelEl.value, buckets);

            fillBar(breakdown);
            fillRows(breakdown);

            byId('preview-fee-label').textContent = feePct + '% + ' + money(feeFixed);
            byId('preview-fee-amt').textContent = money(breakdown.processing);
            byId('preview-charge').textContent = money(breakdown.charge);

            var recipientNameEl = byId('preview-recipient-name');
            fillMessage(recipientNameEl ? recipientNameEl.textContent : '', buckets, breakdown);
        }

        // --- the form itself ---------------------------------------------------

        function currentIndexes() {
            var raw = byId('bucket-indexes').value;
            return raw ? raw.split(',').filter(function (s) {
                return s.length > 0;
            }) : [];
        }

        function setIndexes(indexes) {
            byId('bucket-indexes').value = indexes.join(',');
        }

        function enableRow(row, on) {
            row.querySelectorAll('input').forEach(function (input) {
                input.disabled = !on;
            });
        }

        // Takes the next row the JSP left hidden. A row removed earlier is hidden too, so adding
        // after a removal brings that bucket back rather than starting an empty one.
        function addBucketRow() {
            var row = bucketRows.querySelector('.bk-row[hidden]');
            if (!row) {
                C3S.toast('Six allocation buckets is the practical maximum');
                return;
            }
            row.hidden = false;
            enableRow(row, true);

            var indexes = currentIndexes();
            indexes.push(row.getAttribute('data-bucket-idx'));
            setIndexes(indexes);
            onPctChange();
            C3S.toast('Allocation bucket added - name it and set its share');
        }

        function removeBucketRow(idx) {
            var row = byId('bucket-row-' + idx);
            if (row) {
                row.hidden = true;
                enableRow(row, false);
            }
            setIndexes(currentIndexes().filter(function (i) {
                return i !== String(idx);
            }));
            onPctChange();
        }

        function bucketTotal() {
            var total = 0;
            rows().forEach(function (row) {
                total += parseInt(row.querySelector('.bk-pct-input').value, 10) || 0;
            });
            return total;
        }

        function onPctChange() {
            var total = bucketTotal();
            var balanced = total === 100;
            byId('bk-total').className = 'bk-total ' + (balanced ? 'ok' : 'bad');
            byId('bk-total-pct').textContent = total + '%';
            byId('bk-total-note').textContent = balanced
                ? ' - balanced'
                : ' - ' + (total > 100 ? (total - 100) + '% over' : (100 - total) + '% unallocated');
            document.querySelector('.bk-balance').hidden = balanced;
            updatePreview();
        }

        function balanceBuckets() {
            var recipients = rows().filter(function (row) {
                return row.querySelector('.bk-pct-input').getAttribute('data-recipient') === 'true';
            });
            if (recipients.length === 0) {
                return;
            }
            var recipientInput = recipients[recipients.length - 1].querySelector('.bk-pct-input');
            var diff = 100 - bucketTotal();
            recipientInput.value = Math.max(0, (parseInt(recipientInput.value, 10) || 0) + diff);
            onPctChange();
            C3S.toast('Balanced to 100%');
        }

        function setFeeModel(model) {
            byId('fee-model-input').value = model;
            document.querySelectorAll('#fee-seg button').forEach(function (b) {
                b.classList.toggle('active', b.getAttribute('data-model') === model);
            });
            updatePreview();
        }

        form.addEventListener('input', function (e) {
            if (e.target.classList.contains('bk-pct-input')) {
                onPctChange();
            } else if (e.target.matches('.bk-name, .bk-purpose, #feePct, #feeFixed')) {
                updatePreview();
            }
        });

        form.addEventListener('click', function (e) {
            var del = e.target.closest('.bk-del');
            if (del && !del.disabled) {
                removeBucketRow(parseInt(del.getAttribute('data-bucket-idx'), 10));
                return;
            }
            if (e.target.closest('.bk-add')) {
                addBucketRow();
                return;
            }
            var feeBtn = e.target.closest('#fee-seg button');
            if (feeBtn) {
                setFeeModel(feeBtn.getAttribute('data-model'));
                return;
            }
            if (e.target.closest('.bk-balance')) {
                balanceBuckets();
            }
        });

        // server checks this too - this is just to skip the round trip
        form.addEventListener('submit', function (e) {
            var total = bucketTotal();
            if (total !== 100) {
                e.preventDefault();
                C3S.toast('Allocations total ' + total + '% - they must total 100% before saving.');
            }
        });

        onPctChange();
        setFeeModel(byId('fee-model-input').value);
    }

    C3S.ready(initAllocationForm);
})();
