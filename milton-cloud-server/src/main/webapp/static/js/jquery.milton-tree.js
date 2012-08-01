


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
                includeContentTypes: [],
                onselect: function(n) {
                    log("def: selected", n);
                },                
                onselectFile: function(n) {
                    log("def: file selected", n);
                },
                onselectFolder: function(n) {
                    log("def: folder selected", n);
                },
                ondelete: function(n) {
                    log("def: ondelete", n);
                },
                onnewfolder: function(n) {
                    log("def: onnewfolder", n);
                }
            
            }, options);  
            config.hrefMap = new Object();
            config.nodeMap = new Object();
            log("set options on", this);
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
            tree.data("options", config); // set data on tree and tree container for ease of use
            log("set options on", tree);
            
            container.append(tree);
            initTree(tree, config);
        },
        getSelectedUrl : function(x ) {
            var options = this.data("options");
            node = $(options.selectedItem);
            var url = toFullUrl(node, options);
            return url;
        },
        // if the selected item is a folder. If its a file, returns the parent url
        getSelectedFolderUrl : function(x ) {
            var options = this.data("options");
            node = $(options.selectedItem);            
            var icon = node.find("> a > ins.jstree-icon");
            log("check if folder", node, icon);
            if( icon.hasClass("file")) {                
                node = node.parent().closest("li");
                log("is file, get parent", node);
            } else {
                log("is folder");
            }            
            var url = toFullUrl(node, options);
            return url;
        },
        refreshSelected: function() {
            var options = this.data("options");
            var tree = this.find(".jstree")[0];
            $.jstree._reference(tree).refresh(options.selectedItem);
        },
        // Add a file node to the current node
        addFile: function(name, href) {
            var options = this.data("options");
            var tree = this.find(".jstree");
            var js = {
                data: name,
                attr: {
                    "id": createNodeId(href, options)
                }
            };
            var parentNode = $(options.selectedItem);

            log("is open, so just add it");
            var r = $.jstree._reference(tree[0]).create_node(options.selectedItem, "inside", js);
            log("addFile: r=", r);
            r.find("a ins").addClass("file");  
            parentNode.removeClass("jstree-closed");
            parentNode.addClass("jstree-open");
        //tree.mtree("select", r);
        },
        // Make the given node selected
        select: function(node, callback) {
            log("select", node);
            var options = this.data("options");            
            if( !options ) {
                log("Could not find options data in", this);
            }
            var tree;
            if( this.hasClass("jstree")) {
                tree = this; // this is the tree
            } else {
                tree = this.find(".jstree"); // given node is parent
            }                
            tree.find(".jstree-clicked").removeClass("jstree-clicked");
            node.find("> a").addClass("jstree-clicked");
            var parentNode = node.closest("li");
            log("open node", parentNode);
            var treeDom = tree[0];
            var treeRef = $.jstree._reference(treeDom);
            if( !treeRef ) {
                log("Couldnt find tree for: ", this, tree);
            }
            treeRef.open_node(parentNode, callback);
            
            options.onselect(node);
            options.selectedItem = node;
            var icon = node.find("> a > ins.jstree-icon");
            var url = toFullUrl(node, options);
            if( icon.hasClass("file")) {                
                options.onselectFile(node, url);
            } else {
                options.onselectFolder(node, url);
            }
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
    
    tree.bind("loaded.jstree", function() {
        log("tree loaded", config.pagePath);
        if( config.pagePath ) {
            urlParts = config.pagePath.split("/");
            if( urlParts.length > 0 ) {
                autoOpen(tree, config, "/", 1, urlParts);
            }
        }
    });
    
    tree.jstree({
        "plugins" : [ "themes", "json_data","ui" ],
        "load_open" : true,
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
                            if( key > 0 && isDisplayable(value, config) ) {
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
        config.onselect(config.selectedItem);
        var node = $(this);
        $.jstree._reference(tree[0]).open_node(node);
        
        var icon = node.find("> a > ins.jstree-icon");        
        log("base url", toUrl(node, config));
        var url = toFullUrl(node, config);
        if( icon.hasClass("file")) {
            config.onselectFile(node, url);
        } else {
            config.onselectFolder(node, url);
        }
    });
}

function autoOpen(tree, config, loadUrl, part, urlParts) {
    log("autoOpen", loadUrl, part, urlParts);
    if( part >= urlParts.length ) {
        return;
    }
    loadUrl = loadUrl + urlParts[part] + "/";
    var nodeId = "#" + toNodeId(loadUrl, config);
    var node = $(nodeId);
    if( node.length == 0 ) {
        log("Couldnt find node", nodeId);
        return;
    }
    tree.mtree("select", node, function() {
        autoOpen(tree, config, loadUrl, part+1, urlParts );
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

function toNodeId(url, config) {
    var nodeId = config.hrefMap[url];
    if( nodeId ) {
        log("found", url, nodeId);
    } else {
        log("not found", url, "in map", config.hrefMap);
    }
    return nodeId;
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

function createNodeId(href, config) {
    var newId = "node_" + config.nodeMapNextId;
    config.nodeMapNextId = config.nodeMapNextId + 1;
    var newHref = href.replace(config.basePath, "");
    config.nodeMap[newId] = newHref;
    config.hrefMap[newHref] = newId;
    return newId;
}

function toPropFindUrl(path, config) {
    var url;
    if( path == "") {
        url = config.basePath + "/";
    } else {
        url = config.basePath + path;
    }
    url = url + "_DAV/PROPFIND?fields=name,getcontenttype>contentType,href,iscollection&depth=1";
    //log("toPropFindUrl","base:", config.basePath, "path:", path,"final url:", url);
    return url;
}

function isDisplayable(item, config) {
    // If includeContentTypes is set, then must be of correct content type, or a collection
    if( !item.iscollection ) { // only consider content types for files
        if( config.includeContentTypes.length > 0 ) {
            var isCorrectType;
            for( i=0; i<config.includeContentTypes.length; i++) {
                var ct = config.includeContentTypes[i];
                if( item.contentType && item.contentType.contains(ct)) {
                    isCorrectType = true;
                    break;
                }
            }
            if( !isCorrectType ) {
                return false;
            }
        }
    }
    if( isExcluded(item.href, config)) {
        return false;
    } else if( !isDisplayableFileHref(item.href, config)) {
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