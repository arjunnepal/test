/* Show/hide for a password field: <button data-reveal="field-id" hidden>. The button stays hidden without JS. */
(function () {
    'use strict';

    function initPasswordReveal() {
        var button = document.querySelector('[data-reveal]');
        if (!button) {
            return;
        }
        var input = document.getElementById(button.getAttribute('data-reveal'));
        if (!input) {
            return;
        }

        button.hidden = false;
        button.addEventListener('click', function () {
            var show = input.type === 'password';
            input.type = show ? 'text' : 'password';
            button.textContent = show ? 'Hide' : 'Show';
            button.setAttribute('aria-pressed', String(show));
            button.setAttribute('aria-label', show ? 'Hide password' : 'Show password');
            input.focus();
        });
    }

    C3S.ready(initPasswordReveal);
})();
