
jQuery.noConflict();

Tapestry.Initializer.termAutocompleter = function(elementId) {
	jQuery("#" + elementId).autocomplete({
		select: function(event, ui) { 
			jQuery("textarea[name='meaning']").attr("value", ui.item.meaning);
		}
		
	});
}