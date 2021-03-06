define([], function() {
  var ckEditorSetup = {removePlugins: 'elementspath',
                       height: '200px',
                       skin: 'kama',
                       entities: false,
                       extraPlugins: 'autogrow,onchange',
                       autoGrow_minHeight: '200',
                       resize_enabled: false,
                       startupFocus: false,
                       toolbarCanCollapse: false,
                       toolbar: 'edith',
                       toolbar_edith: [{name: 'basicstyles',
                                        items: ['SpecialChar', 'Bold','Italic',
                                                'Underline', 'Subscript',
                                                'Superscript', '-', 'RemoveFormat']},
                                                {name: 'links', items: ['Link', 'Unlink']},
                                                {name: 'document', items: ['Source']}]};
  return ckEditorSetup;
});
