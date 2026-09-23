/* C3S.toast(message) - the short notice at the bottom of the dashboard. Needs #toast / #toast-msg. */
(function () {
    'use strict';

    var hideTimer = null;

    C3S.toast = function (message) {
        var toast = document.getElementById('toast');
        if (!toast) {
            return;
        }
        document.getElementById('toast-msg').textContent = message;
        toast.classList.add('show');
        clearTimeout(hideTimer);
        hideTimer = setTimeout(function () {
            toast.classList.remove('show');
        }, 3500);
    };

    // a message left by the previous request (save, archive, ...) comes via data-msg - it can
    // contain quotes - never inlined into a <script>
    C3S.ready(function showFlash() {
        var flash = document.querySelector('.flash-toast');
        if (flash && flash.dataset.msg) {
            C3S.toast(flash.dataset.msg);
        }
    });
})();
