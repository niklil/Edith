define(['jquery', 'underscore', 'backbone', 'vent', 'handlebars', 'spinner', 'raw-selection'],
       function($, _, Backbone, vent, Handlebars, spinner, rawSelection) {
  var isInverseSelection = function(selection) {
    if (selection.anchorNode === selection.focusNode) {
      return selection.anchorOffset > selection.focusOffset;
    } else {
      return selection.anchorNode.compareDocumentPosition(selection.focusNode) == 2;
    }
  }

  var whitespaceRe = new RegExp(/\s+/g);

  var getBaseSelection = function() {
    var selection = rawSelection();

    if (selection.toString() === '') {
      return;
    }
    
    var startNode = selection.anchorNode;
    var endNode = selection.focusNode;
    var startOffset = selection.anchorOffset;
    var endOffset = selection.focusOffset;
    var selectionString = selection.toString().trim();
    if (isInverseSelection(selection)) {
      startNode = selection.focusNode;
      endNode = selection.anchorNode;
      startOffset = selection.focusOffset;
      endOffset = selection.anchorOffset;
    }
    if (startNode.textContent.substr(startOffset).trim().length <= 0) {
      // FIXME: This is not exhaustive (might be parent or sibling even more on the right)
      startNode = _.first(startNode.nextSibling.childNodes);
      startOffset = startNode.textContent.substr(startOffset).length;
    }
    if (endOffset === 0) {
      // FIXME: This is not exhaustive (might be parent or sibling even more on the left)
      endNode = _.last(endNode.previousSibling.childNodes);
      endOffset = endNode.textContent.length - 1;
    }
    var i = 0;
    while (whitespaceRe.test(selection.toString().charAt(i))) {
      --startOffset;
      --endOffset;
      ++i;
    }
    i = selection.toString().length - 1;
    while (whitespaceRe.test(selection.toString().charAt(i))) {
      --endOffset;
      --i;
    }
    return {startNode: startNode,
            endNode: endNode,
            startOffset: startOffset,
            endOffset: endOffset,
            selectionString: selectionString};
  }

  var getCharIndex = function(str, char, offset) {
    var prevStr = str.substring(0, offset);
    var idx = -1;
    for (var i = 0; i < prevStr.length; ++i) {
      if (prevStr.charAt(i) === char) {
        ++idx
      }
    }
    return ++idx;
  }

  var previousSiblingsToString = function(node) {
    var sibling = null;
    if (node.className && node.className.indexOf('notecontent') !== -1 && node.previousSibling == null && node.nodeType === 3) {
      sibling = node.parentNode.previousSibling;
    } else {
      sibling = node.previousSibling;
    }
    if (sibling) {
      var str = '';
      if (sibling.nodeType === 3 || sibling.className.indexOf('notecontent') !== -1) {
        str = sibling.textContent;
      }
      return previousSiblingsToString(sibling) + str;
    }
    return '';
  }

  var documentNoteTemplate = Handlebars.compile(documentNoteTemplate);
  var DocumentView = Backbone.View.extend({
    events: {'mouseup': 'selectionChange',
             'click .noteanchor': 'selectNote'},

    initialize: function() {
      _.bindAll(this, 'render', 'selectionChange', 'selectNote', 'getSelection');
      var self = this;
        
      vent.on('document:open annotation:change document-note:deleted', function(id) {
        self.documentId = id;
        self.render(id);
      });
      vent.on('document-note:select', function(id, skipScroll) {
        if (skipScroll) {
          return;
        }
        var elem = self.$('#start' + id);
        if (elem.size() == 1) {
          elem[0].scrollIntoView(true);
        }
      });
      vent.on('document:reset-raw-selection', function() {
        if (self.savedRawSelection) {
          var sel = window.getSelection();
          sel.removeAllRanges();
          for (var i = 0, len = self.savedRawSelection.length; i < len; ++i) {
              sel.addRange(self.savedRawSelection[i]);
          } 
        }
      });

      vent.on('note:new', function() {
        var selection = self.getSelection();
        if (selection && selection.selection.length > 0) {
          vent.trigger('document-note:create', self.documentId, selection);
        }
      });
    },

    render: function(id) {
      spinner('document:loaded');
      var self = this;
      $.get('/api/documents/' + id, function(data) {
        // FIXME: Create dedicated view
        var title = _.last(data.path.split("/documents/trunk/"));
        window.document.title = title;
        $('#document-title').html(title);
      }); 

      $.get('/api/documents/' + id + '/raw', function(data) {
        self.$el.html(data)
                .effect('highlight', {color: 'lightblue'}, 500);
        vent.trigger('document:loaded', id);

        // Hover on anchors highlights the annotation text (#64)
        self.$el.find('span[id^="end"]').each(function() {
          $(this).mouseenter(function() {
            self.$el.find('span.n' + $(this).attr('id').substring(3)).addClass('highlight');
          }).mouseleave(function() {
            self.$el.find('span.n' + $(this).attr('id').substring(3)).removeClass('highlight');
          });
        });
      });
    },
    
    selectNote: function(evt) {
      var id = parseInt($(evt.target).attr('id').substring(3));
      vent.trigger('document-note:select', id, true);
    },

    getSelection: function() {
      var baseSelection = getBaseSelection();
      if (!baseSelection) {
        return;
      }
      var str = baseSelection.selectionString;
      var startChar = str.charAt(0);
      var endChar = str.charAt(str.length - 1);
      var previousFromStart = previousSiblingsToString(baseSelection.startNode);
      var additionalStartOffset = previousFromStart.length;
      var previousFromEnd = previousSiblingsToString(baseSelection.endNode);
      var additionalEndOffset = previousFromEnd.length;
      var startCharIndex = getCharIndex(previousFromStart + baseSelection.startNode.textContent, startChar,
          baseSelection.startOffset + additionalStartOffset);
      var endCharIndex = getCharIndex(previousFromEnd + baseSelection.endNode.textContent, endChar,
          (baseSelection.endOffset - 1) + additionalEndOffset);
      var startNode = baseSelection.startNode;
      startNode = startNode.nodeType === 3 ? startNode.parentNode : startNode;
      var endNode = baseSelection.endNode;
      endNode = endNode.nodeType === 3 ? endNode.parentNode : endNode;
      var selection = {selection: str,
                       startCharIndex: startCharIndex,
                       startNode: startNode.id || startNode.getAttribute('data-node'),
                       endNode: endNode.id || endNode.getAttribute('data-node'),
                       endCharIndex: endCharIndex};
      return selection;
    },

    selectionChange: function() {
      this.savedRawSelection = [];
      var sel = rawSelection();
      if (sel.rangeCount) {
          for (var i = 0, len = sel.rangeCount; i < len; ++i) {
              this.savedRawSelection.push(sel.getRangeAt(i));
          }
      }

      vent.trigger('document:selection-change', this.documentId, this.getSelection());
    }
  });

  return DocumentView;
});

