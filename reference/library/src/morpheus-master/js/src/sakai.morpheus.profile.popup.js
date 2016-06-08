var profilePopupObserver = new MutationObserver(function (records, observer) {
                    console.log('HERE1');

        records.forEach(function (record) {
                        console.log('HERE2');
            
            if (record.type === 'childList') {
                        console.log('HERE3');
                for (var i=0,j=record.addedNodes.length;i<j;i++) {
                    console.log(record.addedNodes.item(i).class);
                    var node = $PBJQ(record.addedNodes.item(i));
                    if (node.hasClass('profile-popup')) {
                        console.log('profile popup element added');
                        node.qtip({
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
                    }
                }
            }
        });
    });

profilePopupObserver.observe(document.body, { childList: true, subtree: true });
