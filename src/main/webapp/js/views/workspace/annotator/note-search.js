define(['jquery', 'underscore', 'backbone', 'vent', 'handlebars', 'slickback', 'slickgrid', 'moment', 'localize',
        'text!/templates/workspace/annotator/note-search.html'],
       function($, _, Backbone, vent, Handlebars, Slickback, Slick, moment, localize, searchTemplate) {
  
  var searchTemplate = Handlebars.compile(searchTemplate);
  
  var DocumentNote = Backbone.Model.extend({
    initialize: function() {
      var self = this;
      this.on('change', function() {
        self.dirty = true;
      });
    }
  });
  
  var DocumentNotesCollection = Slickback.PaginatedCollection.extend({
    model: DocumentNote,
    url: '/api/document-notes/query',
    
    setRefreshHints: function() {
      // TODO
    },
    
    sync: function(method, coll, options) {
      var data = options.data;
      if (data.per_page) {
        data.perPage = data.per_page;
        delete data.per_page;
      }
      $.ajax('api/document-notes/query',
        {type: 'post',
         contentType: 'application/json;charset=utf-8',
         data: JSON.stringify(data),
         success: options.success});
    }
  });
  

  var documentNotes = new DocumentNotesCollection();
  
  var UsersCollection = Backbone.Collection.extend({
    model: Backbone.Model,
    url: '/api/users'
  });
  
  var users = new UsersCollection();
  users.fetch();
  
  var LinkFormatter = function(row, cell, value, columnDef, data) {
    var value = data.get('shortenedSelection');
    return value;
  };
  
  var DateFormatter = function(row, cell, value, columnDef, data) {
    var value = data.get('note').editedOn;
    return moment.unix(value / 1000).format("DD.MM.YYYY");
  };
  
  var allColumns = [
      {sortable: true, id: 'shortenedSelection', width: 120, name: localize('shortenedSelection-label'), 
        field: 'shortenedSelection', formatter: LinkFormatter},
      {sortable: true, id: 'fullSelection', name: localize('fullSelection-label'), field: 'fullSelection'},
      // TODO types
      {sortable: true, id: 'description', name: localize('description-label'), field: 'note.description'},
      {sortable: true, id: 'editedOn', name: localize('editedOn-label'), field: 'note.editedOn', formatter: DateFormatter},
      {sortable: true, id: 'lastEditedBy', name: localize('lastEditedBy-label'), field: 'note.lastEditedBy.username'},
      {sortable: true, id: 'status', name: localize('status-label'), field: 'note.status'},
      {sortable: true, id: 'document', name: localize('document-label'), field: 'document.title'}];
  
  var options = {
    formatterFactory: Slickback.BackboneModelFormatterFactory,
    autoHeight: true,
    autoEdit: false,
    defaultColumnWidth: 100
  };
  
  var GridView = Backbone.View.extend({
    
    initialize: function() {
      _.bindAll(this);
      this.render();
    },
    
    render: function() {
      var grid = new Slick.Grid(this.$el, documentNotes, allColumns, options);
      this.grid = grid;
      grid.setSelectionModel(new Slick.RowSelectionModel());
      
      var pager = new Slick.Controls.Pager(documentNotes, grid, this.options.$pager);
      
      grid.onSort.subscribe(function(e, msg) {
        documentNotes.extendScope({
          order: msg.sortCol.field,
          direction: (msg.sortAsc ? 'ASC' : 'DESC')
        })
        documentNotes.fetchWithScope();
      });
      
      documentNotes.onRowCountChanged.subscribe(function() {
        grid.updateRowCount();
        grid.render();
      });
      
      documentNotes.onRowsChanged.subscribe(function() {
        grid.invalidateAllRows();
        grid.render();
      });
      
      documentNotes.on('change', function() {
        $(grid.getActiveCellNode()).css('background', 'red');
      });
    }
    
  });
  
  var userOption = Handlebars.compile("<option value='{{id}}'>{{username}}</option>");
  
  var dateFields = ['createdAfter', 'createdBefore', 'editedAfter', 'editedBefore'];
    
  var NoteSearch = Backbone.View.extend({
    
    events: {'keyup .search': 'search',
             'change form input': 'search',
             'change form select': 'search'},
    
    initialize: function() {
      var self = this;
      _.bindAll(this, 'render', 'search');
      vent.on('tab:open', function(view) {
        if (view === self) {
          documentNotes.fetchWithPagination();
        }
      });
      this.render();
    },
    
    search: function() {
      var arr = this.$("form").serializeArray();
      var data = _(arr).reduce(function(acc, field) {
             if (field.value) {
               if (field.name == 'creators' || field.name == 'types') {
                 if (!acc[field.name]) acc[field.name] = [];
                 acc[field.name].push(field.value);
               } else if (dateFields.indexOf(field.name) > -1) {  
                 acc[field.name] = moment(field.value, 'DD.MM.YYYY').valueOf();
               } else {
                 acc[field.name] = field.value;       
               }  
             } else {
               acc[field.name] = null;
             }     
             return acc;
           }, {});
      
      // tree nodes
      var nodes = this.$(".directory-tree").dynatree("getSelectedNodes");
      data.documents = _.map(nodes, function(n) { return n.data.documentId; });
      
      documentNotes.extendScope(data);
      documentNotes.fetchWithScope();
    },
  
    render: function() {
      var self = this;
      this.$el.html(searchTemplate());
      var gridView = new GridView({el: this.$('.documentNoteGrid'), $pager: this.$('.documentNotePager')});     
      
      var cb = function() { 
        var userOpts = _.map(users.toJSON(), userOption).join("");
        this.$(".creators").html(userOption({}) + userOpts); 
      };
      if (users.length > 0) cb(); else users.fetch({success: cb});
      
      this.$(".date").datepicker({ dateFormat: localize('dateformat') });
      
      this.$(".columns").multiselect({
        onChange:function(element, checked){
          var columns = self.$(".columns option:selected")
            .map(function(idx, el) {
              return allColumns[$(el).val()];
            });
          gridView.grid.setColumns(columns);
        }
      });
      
      this.$('.directory-tree').dynatree({
        checkbox: true,
        selectMode: 3,
        initAjax: {
          url: '/api/files'
        },

        onLazyRead: function(node) {
          node.appendAjax({
            url: '/api/files/',
            data: 'path=' + node.data.path
          });
        },
        
      });
    }
    
  });
  
  return NoteSearch;   
  
});