$PBJQ(document).ready(function () {
    console.log('LOADED dsfsadfasdfasdfasdf');

    $PBJQ('.profile-popup').each(function () {

        var userId = this.dataset.userId;
        console.log('USR ID: ' + userId);

        var pp = $PBJQ(this);

        pp.qtip({
            position: { viewport: $PBJQ(window), adjust: { method: 'flipinvert none'} },
            show: { event: 'click', delay: 0 },
            style: { classes: 'commons-qtip qtip-rounded' },
            hide: { event: 'click unfocus' },
            content: {
                text: function (event, api) {

                    return $PBJQ.ajax( { url: "/direct/profile/" + userId + "/formatted" })
                        .then(function (html) {
                                return html;
                            }, function (xhr, status, error) {
                                api.set('content.text', status + ': ' + error);
                            });
                }
            }
        });
    });
});
