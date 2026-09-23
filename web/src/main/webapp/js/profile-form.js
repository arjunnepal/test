/*
 * The profile form: live preview card, deadline chips and photo preview; counts are char-counter.js.
 * Every element it touches is rendered by profile-form.jsp.
 */
(function () {
    'use strict';

    function initProfileForm() {
        var form = document.getElementById('profile-form');
        if (!form) {
            return;
        }

        function checkedCategoryBoxes() {
            return Array.prototype.slice.call(
                document.querySelectorAll('#cat-pills input[type=checkbox]:checked'));
        }

        function selectDeadline(btn) {
            var val = btn.getAttribute('data-val');
            document.querySelectorAll('.dl-opt').forEach(function (b) {
                b.classList.remove('active');
            });
            btn.classList.add('active');
            var input = document.getElementById('f-deadline');
            if (val === 'custom') {
                input.classList.remove('hidden');
            } else {
                input.classList.add('hidden');
                var d = new Date();
                d.setDate(d.getDate() + parseInt(val, 10));
                input.value = d.toISOString().slice(0, 10);
            }
        }

        function previewPhoto(input) {
            if (!input.files || !input.files[0]) {
                return;
            }
            var reader = new FileReader();
            reader.onload = function (e) {
                document.getElementById('preview-img').src = e.target.result;
                document.getElementById('photo-placeholder').classList.add('hidden');
                document.getElementById('photo-preview').classList.remove('hidden');
                var pcImg = document.getElementById('pc-img');
                pcImg.style.backgroundImage = 'url(' + e.target.result + ')';
                pcImg.style.backgroundSize = 'cover';
                document.getElementById('pc-emoji').classList.add('hidden');
            };
            reader.readAsDataURL(input.files[0]);
            // a new file beats an earlier "remove" click
            document.getElementById('remove-photo-input').disabled = true;
        }

        function removePhoto() {
            document.getElementById('photo-placeholder').classList.remove('hidden');
            document.getElementById('photo-preview').classList.add('hidden');
            document.getElementById('pc-img').style.backgroundImage = 'none';
            document.getElementById('pc-emoji').classList.remove('hidden');
            document.getElementById('photo-input').value = '';
            document.getElementById('remove-photo-input').disabled = false;
            updateCardPreview();
        }

        function updateCardPreview() {
            var name = document.getElementById('f-name').value || 'Recipient name';
            var blurb = document.getElementById('f-blurb').value || '';
            var goal = parseInt(document.getElementById('f-goal').value, 10) || 0;
            var raised = parseInt(document.getElementById('f-raised').value, 10) || 0;
            var emoji = document.getElementById('emoji-input').value;
            var pct = goal > 0 ? Math.min(100, Math.round(raised / goal * 100)) : 0;
            var checked = checkedCategoryBoxes();

            document.getElementById('pc-name').textContent = name;
            document.getElementById('pc-blurb').textContent = blurb;
            if (!emoji && checked.length > 0) {
                emoji = checked[0].getAttribute('data-emoji');
            }
            if (emoji) {
                document.getElementById('pc-emoji').textContent = emoji;
            }

            // the JSP rendered a tag per category - show the ticked ones, in the order it rendered them
            var tagList = document.getElementById('pc-tag-list');
            tagList.querySelectorAll('.pc-tag').forEach(function (tag) {
                tag.hidden = true;
            });
            checked.forEach(function (box) {
                var tag = tagList.querySelector('[data-cat-id="' + box.value + '"]');
                if (tag) {
                    tag.hidden = false;
                }
            });

            // fine to always set - previewPhoto() overrides it with a background-image when there's a photo
            document.getElementById('pc-img').className = 'pc-img'
                + (checked.length > 0 ? ' ' + checked[0].getAttribute('data-color') : '');

            document.getElementById('pc-goal-lbl').textContent =
                '$' + raised.toLocaleString() + ' of $' + goal.toLocaleString();
            document.getElementById('pc-pct').textContent = pct + '%';
            document.getElementById('pc-fill').style.width = pct + '%';
        }

        form.addEventListener('input', updateCardPreview);

        form.addEventListener('change', function (e) {
            if (e.target.id === 'photo-input') {
                previewPhoto(e.target);
            } else if (e.target.closest('#cat-pills')) {
                updateCardPreview();
            }
        });

        form.addEventListener('click', function (e) {
            if (e.target.closest('.photo-remove')) {
                e.stopPropagation();
                removePhoto();
                return;
            }
            if (e.target.closest('.photo-upload')) {
                document.getElementById('photo-input').click();
                return;
            }
            var chip = e.target.closest('.dl-opt');
            if (chip) {
                selectDeadline(chip);
            }
        });

        var initialDeadlineChip = document.querySelector('.dl-opt.active');
        if (initialDeadlineChip) {
            selectDeadline(initialDeadlineChip);
        }

        // photo URL comes from a data attr, not an inline style
        var pcImg = document.getElementById('pc-img');
        if (pcImg && pcImg.dataset.photoUrl) {
            pcImg.style.backgroundImage = 'url(' + pcImg.dataset.photoUrl + ')';
        }
    }

    C3S.ready(initProfileForm);
})();
