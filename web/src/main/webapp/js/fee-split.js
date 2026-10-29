/*
 * C3S.feeSplit(gift, fee, feeModel) - the charge, the processor's cut and what is left to split,
 * for the allocation form's preview and the report's what-if controls. Same rules as schema.sql.
 */
(function () {
    'use strict';

    C3S.round2 = function (v) {
        return Math.round(v * 100) / 100;
    };

    C3S.feeSplit = function (gift, fee, feeModel) {
        // added: the donor pays the fee on top; absorbed: UWNEFL pays it; deducted: it comes out of the gift
        if (feeModel === 'added') {
            return {charge: C3S.round2(gift + fee), processing: fee, net: gift};
        }
        if (feeModel === 'absorbed') {
            return {charge: gift, processing: 0, net: gift};
        }
        var processing = Math.min(fee, gift);
        return {charge: gift, processing: processing, net: C3S.round2(gift - processing)};
    };
})();
