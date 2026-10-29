/* [data-goto="page-donors"] - navigation to pages that aren't built yet just says so. */
(function () {
    'use strict';

    C3S.ready(function initNavStubs() {
        document.addEventListener('click', function (e) {
            var goto = e.target.closest('[data-goto]');
            if (goto) {
                C3S.toast('Navigation to "' + goto.getAttribute('data-goto') + '" is handled by the host application');
            }
        });
    });
})();
