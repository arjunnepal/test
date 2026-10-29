// Live "$50 donation" preview. Mirrors AllocationCalculator / AllocationMessageBuilder
// in Java - keep in sync. Cosmetic only, the server revalidates on save.
// Loaded before allocation-form.js, which calls updatePreview().
function pvRound2(v) {
    return Math.round(v * 100) / 100;
}

function pvMoney(v) {
    return '$' + (v + 0).toFixed(2);
}

function pvEscape(s) {
    return (s || '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

function pvEffectivePurpose(b) {
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

var PV_COLOR_CLASSES = ['res', 'unres', 'c3', 'c4', 'c5', 'c6'];

function pvCollectBuckets() {
    var rows = document.querySelectorAll('#bucket-rows .bk-row');
    var buckets = [];
    rows.forEach(function (row, i) {
        var nameInput = row.querySelector('.bk-name');
        var pctInput = row.querySelector('.bk-pct-input');
        var purposeInput = row.querySelector('.bk-purpose');
        var descInput = row.querySelector('input[name^="bucketDesc_"]');
        buckets.push({
            name: nameInput ? nameInput.value : '',
            pct: parseInt(pctInput.value, 10) || 0,
            toRecipient: pctInput.getAttribute('data-recipient') === 'true',
            purpose: purposeInput ? purposeInput.value : '',
            description: descInput ? descInput.value : '',
            colorClass: PV_COLOR_CLASSES[i % PV_COLOR_CLASSES.length]
        });
    });
    return buckets;
}

function pvCompute(amount, feePct, feeFixed, feeModel, buckets) {
    var gift = Math.max(0, amount);
    var fee = gift > 0 ? pvRound2(gift * (feePct / 100) + feeFixed) : 0;

    var charge, processing, net;
    if (feeModel === 'added') {
        charge = pvRound2(gift + fee);
        processing = fee;
        net = gift;
    } else if (feeModel === 'absorbed') {
        charge = gift;
        processing = 0;
        net = gift;
    } else {
        charge = gift;
        processing = Math.min(fee, gift);
        net = pvRound2(gift - processing);
    }

    var total = buckets.reduce(function (s, b) { return s + b.pct; }, 0);
    var totalOrHundred = total === 0 ? 100 : total;

    var running = 0;
    var results = [];
    buckets.forEach(function (b, i) {
        var isLast = i === buckets.length - 1;
        var amt = isLast ? pvRound2(net - running) : pvRound2(net * (b.pct / totalOrHundred));
        running = pvRound2(running + amt);
        var pctOfCharge = charge > 0 ? (amt / charge * 100) : 0;
        results.push({name: b.name, colorClass: b.colorClass, amount: amt, pctOfCharge: pctOfCharge, pct: b.pct});
    });

    return {gift: gift, fee: fee, charge: charge, processing: processing, net: net, feeModel: feeModel, buckets: results};
}

function pvBuildMessage(recipientName, buckets, breakdown) {
    if (buckets.length === 0 || breakdown.buckets.length === 0) {
        return '';
    }

    var recipientIndex = 0;
    for (var i = 0; i < buckets.length; i++) {
        if (buckets[i].toRecipient) {
            recipientIndex = i;
            break;
        }
    }
    var recipientBucket = buckets[recipientIndex];
    var recipientAmount = breakdown.buckets[recipientIndex];
    var who = recipientName && recipientName.trim() !== '' ? recipientName : 'the recipient';

    var deducted = breakdown.feeModel === 'deducted';

    var msg = '';
    if (deducted && breakdown.processing > 0) {
        msg += 'After the ' + pvMoney(breakdown.processing) + ' processing fee, ';
    }
    var giftRef = deducted ? 'your gift' : 'your ' + pvMoney(breakdown.gift) + ' gift';
    msg += '<strong>' + recipientBucket.pct + '%</strong> of ' + giftRef + ' — <strong>' + pvMoney(recipientAmount.amount)
        + '</strong> — goes directly to support ' + pvEscape(who);

    var others = [];
    buckets.forEach(function (b, i) {
        if (i === recipientIndex || b.pct <= 0) {
            return;
        }
        var amt = breakdown.buckets[i];
        others.push('<strong>' + b.pct + '%</strong> — <strong>' + pvMoney(amt.amount)
            + '</strong> — supports ' + pvEscape(pvEffectivePurpose(b)));
    });

    if (others.length === 0) {
        return msg + '.';
    }
    return msg + ', and ' + others.join('; and ') + '.';
}

var _pvDebounceTimer = null;

// debounced - one rebuild after typing pauses, not one per keystroke
function updatePreview() {
    clearTimeout(_pvDebounceTimer);
    _pvDebounceTimer = setTimeout(updatePreviewNow, 80);
}

function updatePreviewNow() {
    var feePctEl = document.getElementById('feePct');
    var feeFixedEl = document.getElementById('feeFixed');
    var feeModelEl = document.getElementById('fee-model-input');
    if (!feePctEl || !feeFixedEl || !feeModelEl) {
        return;
    }

    var buckets = pvCollectBuckets();
    var feePct = parseFloat(feePctEl.value) || 0;
    var feeFixed = parseFloat(feeFixedEl.value) || 0;
    var feeModel = feeModelEl.value;
    var breakdown = pvCompute(50, feePct, feeFixed, feeModel, buckets);

    var feeSegPct = breakdown.charge > 0 ? (breakdown.processing / breakdown.charge * 100) : 0;
    var barHtml = '<div class="alloc-seg fee" data-pct="' + feeSegPct + '" data-min-px="' + (feeSegPct > 0 ? 3 : 0) + '">'
        + (feeSegPct >= 13 ? feeSegPct.toFixed(1) + '%' : '') + '</div>';
    breakdown.buckets.forEach(function (b) {
        barHtml += '<div class="alloc-seg ' + b.colorClass + '" data-pct="' + b.pctOfCharge + '" data-min-px="' + (b.pctOfCharge > 0 ? 3 : 0) + '">'
            + (b.pctOfCharge >= 13 ? b.pctOfCharge.toFixed(1) + '%' : '') + '</div>';
    });
    var previewBarEl = document.getElementById('preview-bar');
    previewBarEl.innerHTML = barHtml;
    applyBarStyles(previewBarEl);

    document.getElementById('preview-fee-label').textContent = feePct + '% + ' + pvMoney(feeFixed);
    document.getElementById('preview-fee-amt').textContent = pvMoney(breakdown.processing);

    var rowsHtml = '';
    breakdown.buckets.forEach(function (b) {
        rowsHtml +=
            '<div class="alloc-row">' +
                '<span class="alloc-dot ' + b.colorClass + '"></span>' +
                '<span class="alloc-lbl">' + pvEscape(b.name) + '<small>' + b.pct + '% of net gift</small></span>' +
                '<span class="alloc-right"><span class="alloc-amt ' + b.colorClass + '">' + pvMoney(b.amount) + '</span></span>' +
            '</div>';
    });
    document.getElementById('preview-bucket-rows').innerHTML = rowsHtml;

    document.getElementById('preview-charge').textContent = pvMoney(breakdown.charge);

    var recipientNameEl = document.getElementById('preview-recipient-name');
    var sampleName = recipientNameEl ? recipientNameEl.textContent : '';
    document.getElementById('preview-message').innerHTML = pvBuildMessage(sampleName, buckets, breakdown);
}
