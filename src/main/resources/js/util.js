define('com.mesilat:confields:util', ['jquery', 'jira/util/formatter'], function($, Formatter){
    function getField(id){
        var url = AJS.params.baseURL + '/rest/confield/1.0/field/' + id;
        return $.ajax({
            url: url,
            type: 'GET',
            dataType: 'json',
            context: {
                url: url
            }
        });
    }
    function getApplicationLinks(){
        var url = AJS.params.baseURL + '/rest/applinks/1.0/applicationlink';
        return $.ajax({
            url: url,
            type: 'GET',
            dataType: 'json',
            context: {
                url: url
            }
        });
    }
    function getFieldSettings(id){
        var url = AJS.params.baseURL + '/rest/confield/1.0/settings/' + id;
        return $.ajax({
            url: url,
            type: 'GET',
            dataType: 'json',
            context: {
                url: url
            }
        });
    }
    function postFieldSettings(settings){
        var url = AJS.params.baseURL + '/rest/confield/1.0/settings/';
        return $.ajax({
            url: url,
            type: 'POST',
            data: JSON.stringify(settings),
            contentType: 'application/json',
            processData: false,            
            context: {
                url: url,
                data: settings
            }
        });
    }
    function format(){
        return Formatter.format.apply(null, Array.from(arguments));
    }
    function onError(err, field){
        if (_.isUndefined(err)){
            AJS.flag({
                type: 'error',
                title: AJS.I18n.getText('com.mesilat.confluence-field.err.unknown'),
                body: (this && this.url)? format(AJS.I18n.getText('com.mesilat.confluence-field.err.resource'), this.url): ''
            });
        } else {
            var msg = err.responseText||'';
            try {
                var obj = JSON.parse(msg);
                if (obj && obj.message) msg = obj.message;
            } catch(ignore){
            }

            if (err.status === 401){
                AJS.flag({
                    type: 'error',
                    title: AJS.I18n.getText('com.mesilat.confluence-field.err.authentication'),
                    body: _.isUndefined(field)? msg:
                        AJS.I18n.getText('com.mesilat.confluence-field.err.authDance', msg, AJS.params.baseURL + '/plugins/servlet/confield/dance?field-id=' + field.id)
                });
            } else {
                AJS.flag({
                    type: 'error',
                    title: AJS.I18n.getText('com.mesilat.confluence-field.name') + (_.isUndefined(field)? '': ': ' + field.name),
                    body: msg
                });
            }
        }
    }
    function isCreateIssueDialog($input){
        return _.isUndefined($input)
            ? AJS.$('div#create-issue-dialog').length > 0
            : $input.closest('div#create-issue-dialog').length > 0
            ;
    }
    function updateFieldHeight($input){
        var $group = $input.closest('div.field-group');
        var $container = $group.find('div.select2-container');
        var $choices = $container.find('ul.select2-choices');
        $container.height($choices.height() + 16);
    }
    function listFieldValues(fieldId){
        var url = AJS.params.baseURL + '/rest/confield/1.0/field/' + fieldId + '/pages';
        return AJS.$.ajax({
            url: url,
            type: 'GET',
            dataType: 'json',
            context: {
                url: url
            }
        });
    }
    function listPages(fieldId, pageId){
        var _pageId = [];
        (_.isArray(pageId)? pageId: _.isUndefined(pageId)? []: [pageId]).forEach(function(id){
            _pageId.push('page-id=' + id);
        });
        var url = AJS.params.baseURL + '/rest/confield/1.0/field/' + fieldId + '/pages-by-id?' + _pageId.join('&');
        return AJS.$.ajax({
            url: url,
            type: 'GET',
            dataType: 'json',
            context: {
                url: url
            }
        });
    }
    function formatOptionValue(id, title){
        return JSON.stringify({
                id: '' + id,
                title: title
            }).replace(/\,/g,'~[$]~');
    }
    function convertData(results){
        var list = [];
        if (!_.isUndefined(results)){
            results.forEach(function(rec){
                list.push({
                    id: formatOptionValue(rec.id, rec.title),
                    text: rec.title
                });
            });
            list.sort(function(a,b){
                return a.text.toLowerCase().localeCompare(b.text.toLowerCase());
            });
        }
        return list;
    }
    function getFieldValues($input){
        var values = $input.data('com-mesilat-confields-values');
        return _.isString(values)? JSON.parse(values): values;
    }

    return {
        getField: getField,
        getApplicationLinks: getApplicationLinks,
        getFieldSettings: getFieldSettings,
        postFieldSettings: postFieldSettings,
        format: format,
        onError: onError,

        isCreateIssueDialog: isCreateIssueDialog,
        updateFieldHeight: updateFieldHeight,
        listFieldValues: listFieldValues,
        listPages: listPages,
        convertData: convertData,
        getFieldValues: getFieldValues
    };
});
