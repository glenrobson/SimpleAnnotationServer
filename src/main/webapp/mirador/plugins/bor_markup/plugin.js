/**
 * plugin.js
 *
 * Copyright, Moxiecode Systems AB
 * Released under LGPL License.
 *
 * License: http://www.tinymce.com/license
 * Contributing: http://www.tinymce.com/contributing
 */

/*jshint unused:false */
/*global tinymce:true */

/**
 * Example plugin that adds a toolbar button and menu item.
 */
tinymce.PluginManager.add('bor_markup', function(editor, url) {
	// Add a button for rank
	editor.addButton('rank', {
		text: 'rank',
		icon: false,
		onclick: function() {
			selection = tinyMCE.activeEditor.selection.getContent();

			tinyMCE.activeEditor.selection.setContent('<span property="ns:rank" class="rank">' + selection 	+ '</span>'); 
		}
	}
	)
	editor.addButton('name', {
		text: 'name',
		icon: false,
		onclick: function() {
			selection = tinyMCE.activeEditor.selection.getContent();

			tinyMCE.activeEditor.selection.setContent('<span property="ns:name" class="name">' + selection 	+ '</span>'); 
		}
	}
	)
	editor.addButton('ship', {
		text: 'ship',
		icon: false,
		onclick: function() {
			selection = tinyMCE.activeEditor.selection.getContent();

			tinyMCE.activeEditor.selection.setContent('<span property="ns:ship" class="ship">' + selection 	+ '</span>'); 
		}
	}
	)
	editor.addButton('place', {
		text: 'place',
		icon: false,
		onclick: function() {
			selection = tinyMCE.activeEditor.selection.getContent();

			tinyMCE.activeEditor.selection.setContent('<span property="ns:place" class="place">' + selection 	+ '</span>'); 
		}
	}
	)
	editor.addButton('service', {
		text: 'service',
		icon: false,
		onclick: function() {
			selection = tinyMCE.activeEditor.selection.getContent();

			tinyMCE.activeEditor.selection.setContent('<span property="ns:service" class="service">' + selection 	+ '</span>'); 
		}
	}
	)

});

