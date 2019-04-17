define('com.mesilat:confields:field-configure', ['jquery', 'com.mesilat:confields:util'], function($, Util){
    function formatOption(option){
        var $span = AJS.$('<span class="ml-select2-option">').text(option.name);
        if (option.iconUrl){
            $span.text(' ' + $span.text());
            AJS.$('<img>').attr('src', option.iconUrl).prependTo($span);
        }
        return $span;
    }
    function showDialog(field, settings, confluenceLinks){
        var caption = Util.format(AJS.I18n.getText('com.mesilat.confluence-field.configure.dlg.caption'), field.name);
        var $dlg = $(Mesilat.Confield.Templates.configureDialog({
            caption: caption,
            confluenceLinks: confluenceLinks,
            rec: settings
        }));

        var dlg = AJS.dialog2($dlg);
        dlg.on('show', function(e){
            var $dlg = $(e.target),
                $confluenceId = $dlg.find('#ml-configure-confluenceLink'),
                $filter = $dlg.find('#ml-configure-filter'),
                $test = $dlg.find('#ml-configure-test'),
                $form = $dlg.find('form'),
                $btnOk = $dlg.find('button.aui-button-primary'),
                $btnCancel = $dlg.find('button.cancel');

            $confluenceId.auiSelect2({
                data: confluenceLinks,
                formatResult: formatOption,
                formatSelection: formatOption
            });
            if (settings.confluenceId){
                $confluenceId.val(settings.confluenceId).trigger('change');
            }
            if (settings.filter){
                $filter.val(settings.filter).trigger('change');
            }
            $test.auiSelect2({
                multiple: true,
                ajax: {
                    url: AJS.params.baseURL + '/rest/confield/1.0/field/test',
                    dataType: 'json',
                    data: function(text){
                        return {
                            'confluence-id': $confluenceId.val(),
                            filter: $filter.val(),
                            q: text,
                            'max-results': 10
                        };
                    },
                    results: function(data){
                        $form.find('.aui-message').remove();
                        return {
                            start: data.start,
                            limit: data.limit,
                            size: data.size,
                            results: Util.convertData(data.results)
                        };
                    },
                    transport: function (params) {
                        var $request = $.ajax(params);
                        $request.fail(function(err){
                            var msg = err.responseText||'';
                            try {
                                var obj = JSON.parse(msg);
                                if (obj && obj.message) msg = obj.message;
                            } catch(ignore){
                            }

                            if (msg.indexOf('do not have an authorized access token') >= 0){
                                AJS.messages.error($form, {
                                    body: AJS.I18n.getText('com.mesilat.confluence-field.err.authDance', msg, AJS.params.baseURL + '/plugins/servlet/applinks/oauth/login-dance/access?applicationLinkID=' + $confluenceId.val()),
                                    closeable: true,
                                    insert: 'append'
                                });
                            } else {
                                AJS.messages.error($form, {
                                    body: msg,
                                    closeable: true,
                                    insert: 'append'
                                });
                            }
                        });
                        return $request;
                    },
                    delay: 250
                }
            });
            

            $btnCancel.on('click', function(e){
                e.preventDefault();
                dlg.hide();
            });
            $btnOk.on('click', function(e){
                e.preventDefault();

                Util.postFieldSettings({
                    id: field.id,
                    confluenceId: $confluenceId.val(),
                    filter: $filter.val()
                })
                .then(function(){
                    console.log('com.mesilat:confields:field-configure saved', this.data);
                    dlg.hide();
                }, function(err){
                    Util.onError(err);
                });
            });
        });
        dlg.show();
    }

    function init(field){
        //console.log('com.mesilat:confields:field-configure', 'Do configure...');
        var $a = $('<a href="javascript:;">')
            .attr('title', AJS.I18n.getText('com.mesilat.confluence-field.configure.title'))
            .text(AJS.I18n.getText('com.mesilat.confluence-field.name'));
        $('<li>').append($a).appendTo($('.aui-page-panel-content table.jiraform.jirapanel ul.square'));

        $a.on('click', function(){
            $.when(
                Util.getFieldSettings(field.id),
                Util.getApplicationLinks()
            )
            .then(function(){
                //console.log('com.mesilat:confields:field-configure saved', arguments);
                var settings = arguments[0][0],
                    confluenceLinks = arguments[1][0].applicationLinks.filter(function(applicationLink){
                            return applicationLink.typeId === 'confluence';
                        });
                if (_.keys(settings).length === 0){
                    settings.id = field.id;
                    var primary = confluenceLinks.filter(function(confluenceLink){
                        return confluenceLink.isPrimary;
                    });
                    if (primary.length > 0){
                        settings.confluenceId = primary[0].id;
                    }
                }
                showDialog(field, settings, confluenceLinks);
            }, function(err){
                Util.onError(err);
            });
        });
    }
    return {
        init: init
    };
});

require(['jquery', 'com.mesilat:confields:util', 'com.mesilat:confields:field-configure'], function($, Util, FieldConfigure){
    $(function(){
        var re = /(.+)\/secure\/admin\/ConfigureCustomField\!default\.jspa\?customFieldId=(\d+)/;
        var match = re.exec(window.location);
        if (match){
            Util.getField(match[2])
            .then(function(field){
                if (field.type.key === 'com.mesilat.confluence-fields:confluence-field'){
                    FieldConfigure.init(field);
                }
            }, function(err){
                Util.onError(err);
            });
        }
    });
});