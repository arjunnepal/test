/* A field with data-count-target="some-id" keeps #some-id showing its current length. */
(function () {
    'use strict';

    C3S.ready(function initCharCounters() {
        document.addEventListener('input', function (e) {
            var countId = e.target.getAttribute && e.target.getAttribute('data-count-target');
            var out = countId && document.getElementById(countId);
            if (out) {
                out.textContent = e.target.value.length;
            }
        });
    });
})();
