// Allocation form: add/remove bucket rows, running total, fee model, block a
// save that isn't 100%. Delegated listeners so new rows work too. No inline on*.
(function () {
    'use strict';

    var form = document.getElementById('alloc-form');
    var bucketRows = document.getElementById('bucket-rows');
    if (!form || !bucketRows) {
        return;
    }

    var nextBucketIndex = parseInt(bucketRows.getAttribute('data-next-index'), 10) || 0;
    var BK_COLOR_CLASSES = ['res', 'unres', 'c3', 'c4', 'c5', 'c6'];

    function currentIndexes() {
        var raw = document.getElementById('bucket-indexes').value;
        return raw ? raw.split(',').filter(function (s) { return s.length > 0; }) : [];
    }

    function setIndexes(indexes) {
        document.getElementById('bucket-indexes').value = indexes.join(',');
    }

    function addBucketRow() {
        if (bucketRows.querySelectorAll('.bk-row').length >= 6) {
            showToast('Six allocation buckets is the practical maximum');
            return;
        }
        var idx = nextBucketIndex++;
        var swatchClass = BK_COLOR_CLASSES[bucketRows.querySelectorAll('.bk-row').length % BK_COLOR_CLASSES.length];
        var row = document.createElement('div');
        row.className = 'bk-row';
        row.id = 'bucket-row-' + idx;
        var key = 'custom-' + Date.now() + '-' + idx;
        row.innerHTML =
            '<span class="bk-swatch ' + swatchClass + '"></span>' +
            '<div class="bk-fields">' +
              '<input class="bk-name" type="text" name="bucketName_' + idx + '" placeholder="Bucket name" value="New allocation"/>' +
              '<input class="bk-purpose" type="text" name="bucketPurpose_' + idx + '" placeholder="What this funds - blank uses a default description"/>' +
              '<input type="hidden" name="bucketDesc_' + idx + '" value=""/>' +
              '<input type="hidden" name="bucketKey_' + idx + '" value="' + key + '"/>' +
            '</div>' +
            '<div class="bk-pct"><input type="number" min="0" max="100" step="1" name="bucketPct_' + idx + '" value="0" class="bk-pct-input" data-recipient="false"/><span>%</span></div>' +
            '<button type="button" class="bk-del" title="Remove bucket" data-idx="' + idx + '">&times;</button>';
        bucketRows.appendChild(row);

        var indexes = currentIndexes();
        indexes.push(String(idx));
        setIndexes(indexes);
        onPctChange();
        showToast('Allocation bucket added - name it and set its share');
    }

    function removeBucketRow(idx) {
        var row = document.getElementById('bucket-row-' + idx);
        if (row) {
            row.remove();
        }
        setIndexes(currentIndexes().filter(function (i) { return i !== String(idx); }));
        onPctChange();
    }

    function onPctChange() {
        var inputs = document.querySelectorAll('.bk-pct-input');
        var total = 0;
        inputs.forEach(function (i) { total += parseInt(i.value, 10) || 0; });
        var el = document.getElementById('bk-total');
        var balanced = total === 100;
        el.className = 'bk-total ' + (balanced ? 'ok' : 'bad');
        el.innerHTML = balanced
            ? 'Total <strong>100%</strong> - balanced'
            : 'Total <strong>' + total + '%</strong> - ' + (total > 100 ? (total - 100) + '% over' : (100 - total) + '% unallocated') +
              ' <button type="button" class="bk-balance">Balance</button>';
        updatePreview();
    }

    function balanceBuckets() {
        var inputs = document.querySelectorAll('.bk-pct-input');
        var total = 0;
        var recipientInput = null;
        inputs.forEach(function (i) {
            total += parseInt(i.value, 10) || 0;
            if (i.getAttribute('data-recipient') === 'true') { recipientInput = i; }
        });
        if (!recipientInput) {
            return;
        }
        var diff = 100 - total;
        recipientInput.value = Math.max(0, (parseInt(recipientInput.value, 10) || 0) + diff);
        onPctChange();
        showToast('Balanced to 100%');
    }

    function setFeeModel(model) {
        document.getElementById('fee-model-input').value = model;
        document.querySelectorAll('#fee-seg button').forEach(function (b) {
            b.classList.toggle('active', b.getAttribute('data-model') === model);
        });
        updatePreview();
    }

    // --- wiring -----------------------------------------------------------

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
            removeBucketRow(parseInt(del.getAttribute('data-idx'), 10));
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
        var inputs = document.querySelectorAll('.bk-pct-input');
        var total = 0;
        inputs.forEach(function (i) { total += parseInt(i.value, 10) || 0; });
        if (total !== 100) {
            e.preventDefault();
            showToast('Allocations total ' + total + '% - they must total 100% before saving.');
        }
    });

    onPctChange();
    setFeeModel(document.getElementById('fee-model-input').value);
})();
