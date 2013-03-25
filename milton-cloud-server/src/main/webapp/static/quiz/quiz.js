// aaa

var $content;
var selectedProgram = null;
var selectedCourse = null;
var selectModule = null;
var parentOrgHref;


function loadQuizEditor(modal, data) {
    modal.find("#quizQuestions").html(data.body);
    var olQuiz = modal.find("ol.quiz");
    if( olQuiz.length == 0 ) {
        olQuiz = $("<ol class='quiz'></ol>");
        modal.find("#quizQuestions").append(olQuiz);
    }
    // set answers into the quiz
    var quizItems = modal.find("ol.quiz > li");
    for( prop in data) {
        if( prop.startsWith("answer")) {
            var n = prop.replace("answer",""); // get the answer number
            var li = $(quizItems[n]);
            log("answer",n, prop, data[prop], li);
            var input = li.find("input[type=text],select,textarea");
            if( input.length > 0 ) {
                input.val(data[prop]); // set answer on textboxes, selects and textareas
                log("restored input", input, data[prop]);
            } else {
                var radios = li.find("input[type=radio]");
                radios.attr("checked", "");
                var radio = radios.filter("[value=" + data[prop] + "]");
                radio.attr("checked", "true"); // set radio buttons
                log("restored radio", radio);
            }                        
        }
    }
    modal.find("input[type=radio]").closest("ol").each(function(i,n) {
        var ol = $(n);
        ensureOneEmptyRadio(ol);
    });    
}

function prepareQuizForSave(form, data) {
    log("build data object for quiz");
    var quiz = form.find("#quizQuestions");
    // Set names onto all inputs. Just number them answer0, answer1, etc. And add a class to the li with the name of the input, to use in front end validation
    var questions = quiz.find("ol.quiz > li");
    questions.each(function(q, n) {
        var li = $(n);
        setClass(li, "answer", q); // will remove old classes
        li.find("input,select,textarea").each(function(inpNum, inp) {
            $(inp).attr("name", "answer" + q);
        });
    });
        
    if( data ) {
        data.pageName = form.find("input[name=pageName]").val();
        data.pageTitle = form.find("input[name=pageTitle]").val();
        data.template = form.find("input[name=template]").val();
    } else {
        data = {
            pageName: form.find("input[name=pageName]").val(),
            pageTitle: form.find("input[name=pageTitle]").val(),
            template: form.find("input[name=template]").val()
        };
    }
    // Find all inputs and add them to the data object
    var inputs = quiz.find("input[type=text],select,textarea,input[type=radio]:checked").not(".newQuestionType,input[name=pageTitle],input[name=pageName]");
    log("add inputs", inputs);
    inputs.each(function(i,n){
        var inp = $(n);
        data["answer" + i] = inp.val();
    });
        
    // remove any "temp" elements that have been added as part of editing
    form.find(".temp").remove();
    quiz.find("input[type=radio]").removeAttr("checked");        
    removeEmptyRadios(quiz.find("ol ol"));
                        
    data.body = quiz.html();    
    return data;
}


function initQuizBuilder() {
    var form = $("#quizQuestions");
    log("form", form);
    form.on("click", "h3,p,label", function(e){        
        var target = $(e.target);
        log("editable item clicked", target);
        e.stopPropagation();
        e.preventDefault();       
        
        var inp = $("<input class='" + target[0].tagName + "' type='text'/>");
        log("created inp", inp);
        var txt = target.text();
        if( txt.startsWith("[")) { // place holder text
            txt = "";
        }
        inp.val(txt);
        inp.insertAfter(target);
        target.detach();
        log("detached target", target);
        inp.focus();
        inp.focusout(function() {
            // put back original element            
            var newText = inp.val().trim();
            log("focusout", inp, target, "newText", newText);            
            target.text(inp.val());
            
            // If its a label, and its empty, then remove it
            if( target.hasClass("LABEL") && newText.length == 0 ) {
                inp.closest("li").remove();
            } else {
                target.insertAfter(inp);
                inp.remove();                
            }
            if( target.is("label")) {
                ensureOneEmptyRadio(target.closest("ol"));
            }
        });
    });
    form.on("keyup", "input.radioLabel", function(e){
        var inp = $(e.target);
        var li = inp.closest("li");
        var ul = li.closest("ul");
        var last = ul.find("li").filter(":last");
        log("last", li, last, li==last);
        if( li.is(last) ) {
            addRadioToMulti(ul);
        }
    });    
    // Suppress enter key to prevent users from submitting, and closing, the modal edit window accidentally
    form.on("keypress", "input", function(e) {
        if( e.which == 13 ) {
            e.preventDefault();
            e.stopPropagation();
            $(e.target).focusout();
        }
    });    
    
    // insert a delete X on hover of a question li
    form.on("mouseenter", "ol.quiz > li", function(e) {
        var li = $(e.target).closest("li");
        if( li.find("span.delete").length > 0 ) {
            return;
        }
        var span = $("<span class='delete temp'></span>");
        li.prepend(span);
        li.mouseleave(function() {
            li.find("span.delete").remove();
        });
    });
    form.on("click", "span.delete", function(e) {
        var li = $(e.target).closest("li");
        li.remove();
    });
    
    $("select.newQuestionType").click(function() {        
        var $this = $(this);
        log("new question", $this);
        var type = $this.val();
        $this.val("");
        if( type && type.length > 0 ) {
            addQuestion(type);
        }        
    });
    function addQuestion(type) {
        log("add question", type);
        if( type === "textbox") {
            addQuestionTextbox();
        } else if( type === "multi") {
            addQuestionMulti();
        }
    }

    function addQuestionTextbox() {
        var questions = form.find("ol.quiz");
        log("addQuestionTextbox", questions);
        var li = createQuestionLi(questions);
        li.append($("<textarea class='wide autoresize' cols='50' rows='1'></textarea>"));
    }

    function addQuestionMulti() {
        var questions = form.find("ol.quiz");
        log("addQuestionMulti", questions);
        var li = createQuestionLi(questions);
        var olRadios = $("<ol></ol>");                
        olRadios.attr("id", "answers_" + li.attr("id"));
        li.append(olRadios);        
        addRadioToMulti(olRadios);        
    }        
    function createQuestionLi(form) {
        var id = Math.floor(Math.random()*1000000);
        var li = $(
            "<li id='f" + id + "'>" +
            "<h3>[Enter the question here]</h3>" +
            "<p>[Enter help text here]</p>" +
            "</li>"
            );
        form.append(li);        
        return li;
    }    
}

function removeEmptyRadios(ol) {
    ol.find("li").each(function(i, n) {
        var li = $(n);
        var txt = li.find("label").text().trim()
        if( txt == "" || txt.startsWith("[")) {
            li.remove();
        }
    });
        
}
function ensureOneEmptyRadio(ol) {
    // remove any li's containing empty labels, then add one empty one
    removeEmptyRadios(ol);
    addRadioToMulti(ol);
}

function addRadioToMulti(ol) {    
    var question = ol.closest("li").attr("class");
    log("addRadioToMulti", ol, question);
    var answerId = Math.floor(Math.random()*1000000);        
    var li = $("<li></li>");
    li.append($("<input type='radio' id='answer_" + answerId +"' value='" + answerId + "'/>"));
    li.append($("<label for='answer_" + answerId + "'>[Enter answer text here]</label>"));        
    li.find("input").attr("name", question); // make the name of all radios the question
    ol.append(li);
}

/**
 * Remove all other answer classes, and and the new one
 */
function setClass(element, prefix, suffix) {
    var el = $(element);
    log("setClass", el, el.attr("class"));
    var classes = el.attr("class");
    if( classes ) {
        $.each(classes.split(" "), function(i, n) {
            if( n.startsWith(prefix)) {
                el.removeClass(n);
            }
        });
    }
    el.addClass(prefix + suffix);
}