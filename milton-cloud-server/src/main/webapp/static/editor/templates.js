/*
Copyright (c) 2003-2011, CKSource - Frederico Knabben. All rights reserved.
For licensing, see LICENSE.html or http://ckeditor.com/license
*/

CKEDITOR.addTemplates('default',{
    imagesPath: "/static/editor/",
    templates:[
    
    {
        title:'Activity box',
        image:'icon_activity.gif',
        description:'A box for a learning activity',
        html:"<div class='activity'><p>Type the text here</p></div>"
    },    
    {
        title:'Dropdown1',
        image:'icon_dropdown.gif',
        description:'A sub-heading with a dropdown button which reveals its content.',
        html:"<div class='dropdown'><h6>Type the title here</h6><div><p>Type the text here</p></div></div>"
    },
    {
        title:'Dropdown2',
        image:'icon_dropdown.gif',
        description:'A sub-heading with a dropdown button which reveals its content, with an accentuated bacground.',
        html:"<div class='dropdown accentuated'><h6>Type the title here</h6><div><p>Type the text here</p></div></div>"
    },
    
    {
        title:'Show/Hide Button',
        image:'icon_show.gif',
        description:'A button which causes all following content to be initially hidden, and is only shown when clicked',
        html:"<h6 class='btnHideFollowing'>Type the title here</h6>"
    },
    {
        title:'Accentuated box',
        image:'icon_accentuated.gif',
        description:'A box to add empasis to any block of content',
        html:"<div class='accentuated'><p>Type the text here</p></div>"
    },
    {
        title:'Lightbulb dropdown section',
        image:'icon_lightbulb.gif',
        description:'A sub-heading 2 with a dropdown button which reveals its content.',
        html:"<div class='lightbulb'><div class='dropdown'><h6>Type the title here</h6><div><p>Type the text here</p></div></div></div>"
    },
    {
        title:'Key learning point section',
        image:'icon_key_learning.gif',
        description:'A sub-heading 2 with a dropdown button which reveals its content.',
        html:"<div class='keyPoint'><h4>Key learning point</h4><div><p>Type the text here</p></div></div>"
    },
    {
        title:'Text over image2',
        image:'icon_floating_type.gif',
        description:'A resizable box with text floating over an image',
        html:"<div class='textOverImage'><img src='/static/editor/gear.png' class='bgImage'/><div>Text goes here</div></div>"
    },
    {
        title:'Striped table',
        image:'',
        description:'A table with alternating row colours',
        html:"<table width='100%' class='striped'><thead><tr><th></th><th></th><th></th></tr></thead><tbody><tr><td></td><td></td><td></td></tr><tr><td></td><td></td><td></td></tr></tbody></table>"
    },
    {
        title:'2 Column Page layout',
        image:'',
        description:'A page width table with 2 columns',
        html:"<table width='100%' class='plain page-2cols'><tbody><tr><td width='50%'></td><td></td><td width='50%'></td></tr></tbody></table>"
    }
    ]
});

