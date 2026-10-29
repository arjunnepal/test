(function () {
  "use strict";

  // show/hide password toggle, the button stays hidden without JS
  var button = document.querySelector("[data-reveal]");
  if (!button) {
    return;
  }
  var input = document.getElementById(button.getAttribute("data-reveal"));
  if (!input) {
    return;
  }

  button.hidden = false;
  button.addEventListener("click", function () {
    var show = input.type === "password";
    input.type = show ? "text" : "password";
    button.textContent = show ? "Hide" : "Show";
    button.setAttribute("aria-pressed", String(show));
    button.setAttribute("aria-label", show ? "Hide password" : "Show password");
    input.focus();
  });
})();
