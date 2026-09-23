/*
 * The one global the scripts share - load it first on any page with scripts. Every other file
 * under /js is a feature that registers with it and does nothing on a page without its markup.
 *
 * No inline on* handlers (the CSP's script-src is 'self'), and no file here builds markup: the
 * JSPs render every element a page can show and hide the rest, and these files reveal one and set
 * its text. build/verify-no-markup-in-js.sh checks.
 */
window.C3S = window.C3S || (function () {
    'use strict';

    return {
        /** Run fn once the DOM is parsed. Scripts are deferred, so that's usually already true. */
        ready: function (fn) {
            if (document.readyState === 'loading') {
                document.addEventListener('DOMContentLoaded', fn);
            } else {
                fn();
            }
        },

        // replaced by toast.js and progress-bars.js on pages that load them
        toast: function () {
        },
        applyBarWidths: function () {
        }
    };
})();
