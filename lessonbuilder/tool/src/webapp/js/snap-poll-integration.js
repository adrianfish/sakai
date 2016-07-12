(function ($) {

    if (sakai && sakai.showSnapPoll) {
        $('.itemlink').click(function (e) {

            e.preventDefault();
            var href = this.getAttribute('href');
            var currentPageId = document.getElementById('current-pageid').innerHTML;
            var callback = function () { document.location = href; };
            sakai.showSnapPoll('lessons', currentPageId, callback);
        });
    }
}) (jQuery);
