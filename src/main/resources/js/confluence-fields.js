define('com.mesilat:confields:confluence-field', ['underscore', 'com.mesilat:confields:util'], function(_, Util){
    function getLabel($input){
        return $input.closest('.field-group').find('label').text();
    }
    function updateField($input, values){
        var initial = Util.convertData(values.results);

        if (!_.isUndefined(initial) && initial.length > 0){
            var _values = [];
            initial.forEach(function(val){
                _values.push(val.id);
            });
            $input.val(_values.join(','));
        }

        $input.auiSelect2({
            multiple: true,
            ajax: {
                url: AJS.params.baseURL + '/rest/confield/1.0/field/' + $input.attr('id').substr(12) + '/pages',
                dataType: 'json',
                data: function(text){
                    return {
                        q: text,
                        'max-results': 10
                    };
                },
                results: function(data){
                    return {
                        start: data.start,
                        limit: data.limit,
                        size: data.size,
                        results: Util.convertData(data.results)
                    };
                },
                delay: 250
            },
            initSelection: function(input, callback){
                callback(initial);
            }
        });
    }
    function init($input, val){
        //console.debug('com-mesilat-confields', 'init()', val);
        if (_.isUndefined(val) || val.length === 0){
            return AJS.$.when(function(){ return [];})
            .then(function(){
                updateField($input, []);
            }, function(err){
                Util.onError(err, { name: getLabel($input), id: $input.attr('id').substr(12) });
            });
        } else {
            var pageId = [];
            val.forEach(function(v){
                pageId.push(v.id);
            });

            return Util.listPages($input.attr('id').substr(12), pageId)
            .then(function(data){
                updateField($input, data);
            }, function(err){
                Util.onError(err, { name: getLabel($input), id: $input.attr('id').substr(12) });
            });
        }
    }

    function initSearch($input){
        return Util.listAllCompanies()
        .then(function(data){
            var companies = Util.map2list(data);
            $input.auiSelect2({
                multiple: true,
                data: companies,
                formatSelection: Util.formatOption
            });
        }, function(err){
            Util.onError(err, { name: getLabel($input), id: $input.attr('id').substr(12) });
        });
    }

    return {
        fieldType: 'CONFLUENCE PAGE',
        init: init,
        initNew: init,        
        initSearch: initSearch
    };
});



require([
    'underscore',
    'com.mesilat:confields:util',
    'com.mesilat:confields:confluence-field'
], function(_, Util, Field){

    function postInit($input){
        $input.data('com-mesilat-confields-initialized', true);
        $input.on('change', function(){
            Util.updateFieldHeight($input);
        });
        setTimeout(function(){
            Util.updateFieldHeight($input);
        }, 100);
    }
    function initCustomFieldsNewIssue(force){
        AJS.$('input[com-mesilat-confields="true"]').each(function(){
            var $input = AJS.$(this);
            if (!$input.data('com-mesilat-confields-initialized') || force){
                Field.initNew($input, [])
                .then(function(){
                    postInit($input);
                });
            }
        });
    }
    function initCustomFieldsExistingIssue(force){
        AJS.$('input[com-mesilat-confields="true"]').each(function(){
            var $input = AJS.$(this);
            if (!$input.data('com-mesilat-confields-initialized') || force){
                Field.init($input, Util.getFieldValues($input))
                .then(function(){
                    postInit($input);
                });
            }
        });
    }
    function initCustomFields(force){
        if (Util.isCreateIssueDialog()){
            initCustomFieldsNewIssue(force);
        } else {
            initCustomFieldsExistingIssue(force);
        }
    }
    function initSearchFields(){
        AJS.$('.erp-company-search').each(function(){
            var $input = AJS.$(this);
            if (!$input.data('platforma-erp-initialised')){
                CompanyField.initSearch($input)
                .then(function(){
                    $input.data('platforma-erp-initialised', true);
                });
            }
        });
    }
    function initEditDialog(){
        AJS.$('#create-issue-dialog .buttons-container, #edit-issue-dialog .buttons-container').each(function(){
            var $buttonsContainer = AJS.$(this);
            if (!$buttonsContainer.data('platforma-erp-initialised')){
                var $dateFilterForm = AJS.$(Templates.DataPlatform.Erp.dateFilterForm(Util.getSettings()));
                $dateFilterForm.appendTo($buttonsContainer);
                $dateFilterForm.on('click', function(e){
                    setTimeout(function(){
                        var settings = Util.getSettings();
                        settings.enableFilterByDate = $(e.target).is(':checked');
                        Util.saveSettings(settings);
                        initCustomFields(true);
                    });
                });
                $buttonsContainer.data('platforma-erp-initialised', true);
            }
        });
    }

    AJS.$(function(){
        initCustomFields();

        JIRA.bind(JIRA.Events.NEW_CONTENT_ADDED, function(){
            initCustomFields();
/*
            setTimeout(function(){
                initSearchFields();
                initEditDialog();
            });
*/
        });
    });
});