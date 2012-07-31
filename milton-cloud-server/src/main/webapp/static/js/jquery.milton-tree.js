


(function( $ ) {
    
    var methods = {
        init : function( options ) { 
            var container = this;            
            log("init this", this.options, this);
            var config = $.extend( {
                pageUrl : window.location,
                basePath: window.location,
                onlyFolders: false,
                excludedEndPaths: [],
                showToolbar: true,
                theme: "default",
                onselect: function(n) {
                    log("selected", n);
                },
                ondelete: function(n) {
                    log("ondelete", n);
                },
                onnewfolder: function(n) {
                    log("onnewfolder", n);
                }
            
            }, options);  
            this.data("options", config);

            if( config.showToolbar) {
                var toolbar = $("<div class='mtreeToolbar'></div>");
                var deleteBtn = $("<button class='mtreeDelete'>Delete</button>");
                var addFolderBtn = $("<button class='mtreeAddFolder'>Add Folder</button>");
                toolbar.append(deleteBtn);
                toolbar.append(addFolderBtn);
                deleteBtn.click(function(e) {
                    e.preventDefault();
                    deleteTreeItem(config);
                });
                addFolderBtn.click(function(e) {
                    e.preventDefault();
                    createTreeItemFolder(container, config);
                });
                addFolderBtn.click(function() {
                
                    });
                container.prepend(toolbar);
            }

            var tree = $("<div class='mtree'></div>");
            container.append(tree);
            initTree(tree, config);
        },
        getSelectedUrl : function(x ) {
            var options = this.data("options");
            node = $(options.selectedItem);
            log("options", options);
            log("getSelectedUrl", node );
            var url = toFullUrl(node, options);
            return url;
        },
        refreshSelected: function() {
            var options = this.data("options");
            var tree = this.find(".jstree")[0];
            $.jstree._reference(tree).refresh(options.selectedItem);
        }
    };    
    
    $.fn.mtree = function(method) {
        if ( methods[method] ) {
            return methods[ method ].apply( this, Array.prototype.slice.call( arguments, 1 ));
        } else if ( typeof method === 'object' || ! method ) {
            return methods.init.apply( this, arguments );
        } else {
            $.error( 'Method ' +  method + ' does not exist on jQuery.tooltip' );
        }  
 
    };
})( jQuery );

function initTree(tree, config) {
    log("initTree", tree, config);
    config.nodeMap = new Array();
    // map of node id's keyed by href (relative to base path eg href=Documents/Folder1
    config.hrefMap = new Array();
    config.nodeMapNextId = 0;
    
    tree.jstree({
        "plugins" : [ "themes", "json_data","ui" ],
        "json_data" : {
            "ajax" : {
                "url" : function(n) {
                    var url = toUrl(n, config);
                    url = toPropFindUrl(url, config);
                    return url;
                },
                dataType: "json",

                // this function is executed in the instance's scope (this refers to the tree instance)
                // the parameter is the node being loaded (may be -1, 0, or undefined when loading the root nodes)
                "data" : function (n) {
                    // the result is fed to the AJAX request `data` option
                    return "";
                },
                "error" : function(data) {
                    log("error", data);
                },
                "success" : function (data) {                    
                    var newData=new Array();
                    // Add some properties, and drop first result
                    $.each(data, function(key, value) {
                        if( value.iscollection || !config.onlyFolders ) {
                            if( key > 0 && isDisplayable(value.href, config) ) {
                                value.state = "closed"; // set the initial state
                                //value.data = value.name; // copy name to required property
                                var icon = "file";
                                if( value.iscollection ) icon = "folder";
                                value.data = {
                                    title: value.name,
                                    icon: icon
                                };
                                value.metadata = value;
                                value.attr = {
                                    id : createNodeId(value.href, config), // set the id attribute so we know its href
                                    "class" : value.templateName
                                };
                                newData[newData.length] = value;
                            }
                        }
                    });
                    return newData;
                }
            }
        },
        "themes": {
            "theme": config.theme
        },
        "ui" : {
            "select_limit" : 1,
            "select_multiple_modifier" : "alt",
            "selected_parent_close" : "select_parent"
        }
    });    
    
    tree.on("click", "li", function(e) {
        e.preventDefault();
        e.stopPropagation();
        config.selectedItem = this;
        config.onselect(this);
    });
}

// Just get the url for the given node (a LI element)
function toUrl(n, config) {
    // n should be an LI
    if( n.attr) {
        var id = n.attr("id");
        log("toUrl", n, id);
        var url = config.nodeMap[id];
        return url;
    } else {
        return "";
    }
}

function toFullUrl(n, config) {
    var url = toUrl(n, config);
    if( url ) {
        if( config.basePath != "/") {
            url = config.basePath + url;
        }
    }
    return url;
}

function toPropFindUrl(path, config) {
    var url;
    if( path == "") {
        url = config.basePath + "/";
    } else {
        url = config.basePath + path;
    }
    url = url + "_DAV/PROPFIND?fields=name,getcontenttype,href,iscollection&depth=1";
    //log("toPropFindUrl","base:", config.basePath, "path:", path,"final url:", url);
    return url;
}

function isDisplayable(href, config) {
    if( isExcluded(href, config)) {
        return false;
    } else if( !isDisplayableFileHref(href, config)) {
        return false;
    }
    return true;
}

function isExcluded(href, config) {    
    for(i=0; i<config.excludedEndPaths.length; i++) {
        var p = config.excludedEndPaths[i];
        if( href.endsWith(p)) {
            return true;
        }
    }
    return false;
}

function isDisplayableFileHref(href, config) {
    if( href == 'Thumbs.db' ) return false;
    if( endsWith(href, '/regs/') ) return false;
    if( endsWith(href, '.MOI') ) return false;
    if( endsWith(href, '.THM') ) return false;
    return true;
}

function createNodeId(href, config) {
    var newId = "node_" + config.nodeMapNextId;
    config.nodeMapNextId = config.nodeMapNextId + 1;
    var newHref = href.replace(config.basePath, "");
    config.nodeMap[newId] = newHref;
    config.hrefMap[newHref] = newId;
    return newId;
}

function deleteTreeItem(config) {    
    var node = $(config.selectedItem);
    var href = config.basePath + toUrl(node, config);
    var name = node.find("a").text();
    log("deleteTreeItem", config.selectedItem, name, href);
    confirmDelete(href, name, function() {
        config.ondelete(node);
        node.remove();
    });    
}

function createTreeItemFolder(tree, config) {
    var node = $(config.selectedItem);
    var href = config.basePath + toUrl(node, config);
    var name = node.find("a").text();
    
    log("createTreeItemFolder", node, name, href);
    var newName = prompt("Please enter a name for the new folder");
    if( newName ) {
        createFolder(newName, href, function() {
            log("refresh tree", tree);
            var treeNode = tree.find(".jstree")[0];
            $.jstree._reference(treeNode).refresh(config.selectedItem);
            config.onnewfolder(config.selectedItem);
        })        
    }
    
}