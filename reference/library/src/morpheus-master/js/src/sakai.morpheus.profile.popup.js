var profilePopupObserver = new MutationObserver(function (records, observer) {

        records.forEach(function (record) {
            
            if (record.type === 'childList') {
                for (var i=0,j=record.addedNodes.length;i<j;i++) {
                    var nakedNode = record.addedNodes.item(i);

                    console.log('type: ' + nakedNode.nodeType);
                    if (nakedNode.nodeType === 1) {
                        console.log('name: ' + nakedNode.nodeName);
                        var node = $PBJQ(nakedNode);
                        if (node.hasClass('profile-popup')) {
                            console.log("NAME : " + node.nodeName);
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
            }
        });
    });

profilePopupObserver.observe(document.body, { childList: true, subtree: true });
