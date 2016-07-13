(function ($) {

    var snapPoll = $('#snap-poll');
    var sendButton = $('#snap-poll-send');
    var commentBox = $('#snap-poll-comment-box');

    var reset = function () {

            snapPoll.fadeOut();
            $('.snap-poll-option').removeClass('fa-3x');
            sendButton.prop('disabled', true);
            commentBox.val('');
        };

    $('.snap-poll-option').click(function (e) {

        $(this).addClass('fa-3x').siblings().removeClass('fa-3x');
        sendButton.prop('disabled', false);
    });

    $('#snap-poll-ignore').click(reset);

    $('#snap-poll-send').click(function (e) {

        var option = $('.snap-poll-option.fa-3x').data('option');
        var tool = snapPoll.data('tool');
        var context = snapPoll.data('context');
        var comment = commentBox.val();
        var siteId = portal.siteId;

        var url = '/direct/snap-poll/submitResponse?option=' + option + '&siteId=' + siteId
                    + '&tool=' + tool + '&context=' + context + '&comment=' + comment;

        $.ajax({url: url, cache: false, method: 'POST'})
            .done(function (data, textStatus, jqXHR) {
            }).fail(function (jqXHR, textStatus, errorThrown) {
            });

        console.log(url);

        reset();
    });
}) ($PBJQ);
