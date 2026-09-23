/* Category chips, search and sort over the server-rendered cards in #donor-grid. data-pct is each card's sort key. */
(function () {
    'use strict';

    function initCardFilter() {
        var grid = document.getElementById('donor-grid');
        if (!grid) {
            return;
        }

        var cards = Array.prototype.slice.call(grid.querySelectorAll('.dcard'));
        var shownCount = document.getElementById('grid-shown');
        var searchInput = document.getElementById('search-input');
        var sortSelect = document.getElementById('sort-select');
        var chips = Array.prototype.slice.call(document.querySelectorAll('[data-filter]'));

        // original order, used by "Featured"
        cards.forEach(function (card, i) {
            card.dataset.idx = String(i);
        });

        var state = {category: 'all', search: '', sort: 'default'};

        function num(el, attr) {
            return parseInt(el.getAttribute(attr), 10) || 0;
        }

        function matches(card) {
            var catOk = state.category === 'all' ||
                card.getAttribute('data-cat').split(/\s+/).indexOf(state.category) !== -1;
            var searchOk = state.search === '' ||
                (card.getAttribute('data-search') || '').indexOf(state.search) !== -1;
            return catOk && searchOk;
        }

        function applySort() {
            var sorted = cards.slice();
            sorted.sort(function (a, b) {
                switch (state.sort) {
                    case 'pct-hi': return num(b, 'data-pct') - num(a, 'data-pct');
                    case 'pct-lo': return num(a, 'data-pct') - num(b, 'data-pct');
                    case 'raised': return num(b, 'data-raised') - num(a, 'data-raised');
                    default: return num(a, 'data-idx') - num(b, 'data-idx');
                }
            });
            sorted.forEach(function (card) {
                grid.appendChild(card);
            });
        }

        function applyFilter() {
            var visible = 0;
            cards.forEach(function (card) {
                var show = matches(card);
                card.hidden = !show;
                if (show) {
                    visible++;
                }
            });
            if (shownCount) {
                shownCount.textContent = String(visible);
            }
        }

        function refresh() {
            applySort();
            applyFilter();
        }

        // same as the prototype: a category click resets sort and search, searching resets
        // sort, and sorting clears the search
        function resetSort() {
            state.sort = 'default';
            if (sortSelect) {
                sortSelect.value = 'default';
            }
        }

        function resetSearch() {
            state.search = '';
            if (searchInput) {
                searchInput.value = '';
            }
        }

        chips.forEach(function (chip) {
            chip.addEventListener('click', function () {
                chips.forEach(function (c) {
                    c.classList.remove('active');
                });
                chip.classList.add('active');
                state.category = chip.getAttribute('data-cat-id');
                resetSort();
                resetSearch();
                refresh();
            });
        });

        if (searchInput) {
            searchInput.addEventListener('input', function () {
                state.search = searchInput.value.trim().toLowerCase();
                resetSort();
                refresh();
            });
        }

        if (sortSelect) {
            sortSelect.addEventListener('change', function () {
                state.sort = sortSelect.value;
                resetSearch();
                refresh();
            });
        }
    }

    C3S.ready(initCardFilter);
})();
