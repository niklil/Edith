jQuery.noConflict();

var escapeXml = function(s) {
	return s.replace(/&/g, "&amp;").replace(/"/g, "&quot;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
};

var whitespaceRe = new RegExp(/\s+/g);
var asteriskRe = new RegExp(/\*/g);
var asteriskRe = new RegExp(/\*/g);
var hashRe = new RegExp(/#/g);

var TextSelector = {
	startId : null,
	endId : null,
	startIndex : null,
	endIndex : null,
	selection : null,

	getOccurrenceInString : function(str, substr, minIndex) {
		var occurrence = 0;
		for (var i = 0; i < str.length; ++i) {
		  /* FIXME: The selection "tamies.\n Leenan-Ka" breaks with Chrome.
		   * There is a trailing comma which is handled on Firefox. */
			if (str.substring(i).indexOf(substr) == 0) {
				++occurrence;
				if (i >= minIndex) {
					return occurrence;
				}
			}
		}
		return 0;
	},
	
	prevAllString : function(element) {
		var text = new Array();
		element = element.previousSibling;
		while (element != null) {
			if (element.id === undefined || element.id === "") {
				text.push(element.nodeValue != null ? element.nodeValue : element.innerHTML);
			}
			element = element.previousSibling;
		}
		text.reverse();
		return text.join("");
	},

	nextAllString : function(element) {
		var text = new Array();
		element = element.nextSibling;
		while (element != null) {
			if (element.id === undefined || element.id === "") {
				text.push(element.nodeValue != null ? element.nodeValue : element.innerHTML);
			}
			element = element.nextSibling;
		}
		return text.join("");
	},
	
	getOccurrenceInElement : function(element, offset, substr) {
		var text = new Array();
		if (element.parent().hasClass("notecontent")) {
			text.push(this.prevAllString(element.parent().get(0)));
			offset += text[0].length;
		}
		text.push(this.prevAllString(element.get(0)));
		offset += text[text.length-1].length;
		text.push(escapeXml(element.text()));
		if (element.parent().hasClass("notecontent")){
			text.push(this.nextAllString(element.parent().get(0)));
		}	
		text.push(this.nextAllString(element.get(0)));
		text = text.join("");
		
		return this.getOccurrenceInString(text, escapeXml(substr), offset);
	},

	isInverseSelection : function(selection) {
		var isInDifferentElements = selection.anchorNode != selection.focusNode;
		if (!isInDifferentElements) {
			return selection.anchorOffset > selection.focusOffset;
		} else {
			return selection.anchorNode.compareDocumentPosition(selection.focusNode) == 2;
		}
	},

	updateIndices : function(selection) {
		var startNode = jQuery(selection.anchorNode);
		var endNode = jQuery(selection.focusNode);
		var startOffset = selection.anchorOffset;
		var endOffset = selection.focusOffset;
		
		if (this.isInverseSelection(selection)) {
			startNode = jQuery(selection.focusNode);
			endNode = jQuery(selection.anchorNode);
			startOffset = selection.focusOffset;
			endOffset = selection.anchorOffset;
		}
		
		var selectionString = selection.toString();
		if (whitespaceRe.test(selectionString.charAt(selectionString.length - 1))) {
			--endOffset;
		}
		selectionString = selectionString.replace(hashRe, "").replace(asteriskRe, "").trim();
		var words = selectionString.split(whitespaceRe);
		this.startIndex = this.getOccurrenceInElement(startNode, startOffset, words[0]);
		this.endIndex = this.startIndex;
		if (words.length > 1) {
			var lastWord = words[words.length - 1];
			this.endIndex = this.getOccurrenceInElement(endNode, endOffset - lastWord.length - 1, lastWord);
		}
		
		this.startId = (startNode.parent().attr("id") != "" ? startNode.parent().attr("id") : startNode.parent().parent().attr("id"));
		this.endId = (endNode.parent().attr("id") != "" ? endNode.parent().attr("id") : endNode.parent().parent().attr("id"));
		this.selection = selectionString;
//		 Tapestry.Logging.info("Selection: " + this.selection);
//		 Tapestry.Logging.info("Start ID: " + this.startId);
//		 Tapestry.Logging.info("End ID: " + this.endId);
//		 Tapestry.Logging.info("Start word: " + words[0]);
//		 Tapestry.Logging.info("End word: " + words[words.length - 1]);
//		 Tapestry.Logging.info("Start index: " + this.startIndex);
//		 Tapestry.Logging.info("End index: " + this.endIndex);
		return this.isValidSelection();
	},

	getSelection : function() {
		if (window.getSelection) {
			return window.getSelection();
		} else if (document.getSelection) {
			return document.getSelection();
		} else {
			return document.selection.createRange().text;
		}
	},
	
	getStartIndex : function() { return this.startIndex; },
	getEndIndex : function() { return this.endIndex; },
	
	isValidSelection : function() {
		return this.selection != "" && 
			this.startId != null && this.endId != null &&
			this.startIndex != null && this.startIndex != 0 &&
			this.endIndex != null && this.endIndex != 0;
	}
}