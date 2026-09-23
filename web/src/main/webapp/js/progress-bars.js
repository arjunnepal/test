/*
 * Bars carry their width in data-bar-pct, with an optional data-bar-min-px. Sized on load;
 * C3S.applyBarWidths(root) resizes a preview that has just changed.
 */
(function () {
    'use strict';

    C3S.applyBarWidths = function (root) {
        (root || document).querySelectorAll('[data-bar-pct]').forEach(function (el) {
            el.style.width = el.dataset.barPct + '%';
        });
        (root || document).querySelectorAll('[data-bar-min-px]').forEach(function (el) {
            el.style.minWidth = el.dataset.barMinPx + 'px';
        });
    };

    C3S.ready(function initProgressBars() {
        C3S.applyBarWidths(document);
    });
})();
