/*
Copyright (c) 2003-2011, CKSource - Frederico Knabben. All rights reserved.
For licensing, see LICENSE.html or http://ckeditor.com/license
*/

CKEDITOR.addTemplates('default',{
    imagesPath:CKEDITOR.getUrl(CKEDITOR.plugins.getPath('templates')+'templates/images/'),
    templates:[
    
    {
        title:'Activity box',
        image:'activity.png',
        description:'A box for a learning activity',
        html:"<div class='activity'><p>Type the text here</p></div>"
    },
    {
        title:'Dropdown1',
        image:'dropdownSH1.png',
        description:'A sub-heading with a dropdown button which reveals its content.',
        html:"<div class='dropdown'><h6>Type the title here</h6><div><p>Type the text here</p></div></div>"
    },
    
    /*
    {
        title:'Dropdown 2',
        image:'dropdownSH2.png',
        description:'A sub-heading 2 with a dropdown button which reveals its content.',
        html:"<div class='dropdown sh2'><h4>Type the title here</h4><div><p>Type the text here</p></div></div>"
    },*/
    
    {
        title:'Show/Hide Button',
        image:'dropdownSH2.png',
        description:'A button which causes all following content to be initially hidden, and is only shown when clicked',
        html:"<h6 class='btnHideFollowing'>Type the title here</h6>"
    },
    {
        title:'Accentuated box',
        image:'accentuated.png',
        description:'A box to add empasis to any block of content',
        html:"<div class='accentuated'><p>Type the text here</p></div>"
    },
    {
        title:'Lightbuld dropdown section',
        image:'dropdownSH2.png',
        description:'A sub-heading 2 with a dropdown button which reveals its content.',
        html:"<div class='lightbulb'><div class='dropdown'><h6>Type the title here</h6><div><p>Type the text here</p></div></div></div>"
    },
    {
        title:'Key learning point section',
        image:'keyLearningPoint.png',
        description:'A sub-heading 2 with a dropdown button which reveals its content.',
        html:"<div class='keyPoint'><h4>Key learning point</h4><div><p>Type the text here</p></div></div>"
    }
    ]
});

