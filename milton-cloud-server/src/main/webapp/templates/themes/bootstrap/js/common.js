/**
 *  common.js - this is for helper functions which do not depend on jquery
 */
function endsWith(str, suffix) {
    return str.match(suffix+"$")==suffix;
}
function startsWith(str, prefix) {
    return str.indexOf(prefix) === 0;
}

Date.prototype.formatMMDDYYYY = function(){
    return (this.getMonth()+1) + 
    "/" +  this.getDate() +
    "/" +  this.getFullYear();
}

Date.prototype.formatDDMMYYYY = function(){
    return this.getDate() +
    "/" +  (this.getMonth()+1) + 
    "/" +  this.getFullYear();
}

/**
 * Adds a contains function to String objects
 */
String.prototype.contains = function(it) {
    return this.indexOf(it) != -1;
};

String.prototype.startsWith = function(prefix) {
    return this.indexOf(prefix) === 0;
};
String.prototype.endsWith = function(suffix) {
    return this.match(suffix+"$")==suffix;
};

function ensureObject(target) {
    if( typeof target == "string") {
        target = $("#" + target);
    }
    return target;
}


/**
 * varargs function to output to console.log if console is available
 */
function log() {
    if( typeof(console) != "undefined" ) {
        if (navigator.appName == 'Microsoft Internet Explorer' ) {
            if( typeof(JSON) == "undefined") {
                if( arguments.length == 1 ) {
                    console.log(arguments[0]);
                } else if( arguments.length == 2 ) {
                    console.log(arguments[0], arguments[1]);
                } else if( arguments.length > 2 ) {
                    console.log(arguments[0], arguments[1], arguments[2]);
                }
                
            } else {
                var msg = "";
                for( i=0; i<arguments.length; i++) {
                    msg += arguments[i] + ",";
                    //msg += JSON.stringify(arguments[i]) + ",";
                }
                console.log(msg);
            }
        } else {
            console.log(arguments);
        }
    }
}

function pad2(i) {
    var j = i - 0; // force to be a number
    if( j < 10 ) {
        return "0" + j;
    } else {
        return i;
    }
}


function toFileSize(num) {
    if( num > 1000000 ) {
        return Math.round(num/1000000) + 'Mb';
    } else {
        return Math.round(num/1000) + 'Kb';
    }
}

function toDisplayDateNoTime(dt) {
    return (dt.date) + "/" + (dt.month+1) + "/" + (dt.year+1900);
}

function now() {
    var dt = new Date();
    return {
        day: dt.getDay(),
        month: dt.getMonth(),
        year: dt.getYear()
    };
}



function reverseDateOrd(post1,post2){
    return dateOrd(post1,post2) * -1;
}

function dateOrd(post1,post2){
    var n = post1.date;
    if( n == null ) {
        if( m == null ) {
            return 0;
        } else {
            return -1;
        }
    }
    var m = post2.date;

    if( n.year < m.year ) {
        return -1;
    } else if( n.year > m.year ) {
        return 1;
    }
    if( n.month < m.month ) {
        return -1;
    } else if( n.month > m.month ) {
        return 1;
    }
    if( n.day < m.day ) {
        return -1;
    }
    else if( n.day > m.day ) {
        return 1;
    }
    if( n.hours < m.hours ) {
        return -1;
    } else if( n.hours > m.hours ) {
        return 1;
    }
    if( n.minutes < m.minutes ) {
        return -1;
    } else if( n.minutes> m.minutes ) {
        return 1;
    }
    if( n.seconds < m.seconds ) {
        return -1;
    } else if( n.seconds> m.seconds ) {
        return 1;
    }
    return 0;
}

function isNumber(n) {
    return !isNaN(parseFloat(n)) && isFinite(n);
}

function ajaxLoadingOn(sel) {
    log('ajax ON', sel);
}

function ajaxLoadingOff(sel) {
    log('ajax OFF', sel);

}


/**
 * DHTML date validation script. Courtesy of SmartWebby.com (http://www.smartwebby.com/dhtml/)
 */
// Declaring valid date character, minimum year and maximum year
var dtCh= "/";
var minYear=1900;
var maxYear=2100;

function isInteger(s){
    var i;
    for (i = 0; i < s.length; i++){
        // Check that current character is number.
        var c = s.charAt(i);
        if (((c < "0") || (c > "9"))) return false;
    }
    // All characters are numbers.
    return true;
}

function stripCharsInBag(s, bag){
    var i;
    var returnString = "";
    // Search through string's characters one by one.
    // If character is not in bag, append to returnString.
    for (i = 0; i < s.length; i++){
        var c = s.charAt(i);
        if (bag.indexOf(c) == -1) returnString += c;
    }
    return returnString;
}

function daysInFebruary (year){
    // February has 29 days in any year evenly divisible by four,
    // EXCEPT for centurial years which are not also divisible by 400.
    return (((year % 4 == 0) && ( (!(year % 100 == 0)) || (year % 400 == 0))) ? 29 : 28 );
}
function DaysArray(n) {
    for (var i = 1; i <= n; i++) {
        this[i] = 31
        if (i==4 || i==6 || i==9 || i==11) {
            this[i] = 30
        }
        if (i==2) {
            this[i] = 29
        }
    }
    return this
}

function isDate(dtStr){
    var daysInMonth = DaysArray(12)
    var pos1=dtStr.indexOf(dtCh)
    var pos2=dtStr.indexOf(dtCh,pos1+1)
    var strDay=dtStr.substring(0,pos1)
    var strMonth=dtStr.substring(pos1+1,pos2)
    var strYear=dtStr.substring(pos2+1)
    strYr=strYear
    if (strDay.charAt(0)=="0" && strDay.length>1) strDay=strDay.substring(1)
    if (strMonth.charAt(0)=="0" && strMonth.length>1) strMonth=strMonth.substring(1)
    for (var i = 1; i <= 3; i++) {
        if (strYr.charAt(0)=="0" && strYr.length>1) strYr=strYr.substring(1)
    }
    month=parseInt(strMonth)
    day=parseInt(strDay)
    year=parseInt(strYr)
    if (pos1==-1 || pos2==-1){
        log("The date format should be : dd/mm/yyyy");
        return false
    }
    if (strMonth.length<1 || month<1 || month>12){
        log("Please enter a valid month");
        return false
    }
    if (strDay.length<1 || day<1 || day>31 || (month==2 && day>daysInFebruary(year)) || day > daysInMonth[month]){
        log("Please enter a valid day");
        return false
    }
    if (strYear.length != 4 || year==0 || year<minYear || year>maxYear){
        log("Please enter a valid 4 digit year between "+minYear+" and "+maxYear);
        return false
    }
    if (dtStr.indexOf(dtCh,pos2+1)!=-1 || isInteger(stripCharsInBag(dtStr, dtCh))==false){
        log("Please enter a valid date");
        return false
    }
    return true
}

function ValidateForm(){
    var dt=document.frmSample.txtDate
    if (isDate(dt.value)==false){
        dt.focus()
        return false
    }
    return true
}

function getFileName(path) {
    var arr = path.split('/');
    if( arr.length === 1) {
        return "";
    }
    var name = arr[arr.length-1];
    if( name == null || name.length == 0 ) { // might be empty if trailing slash
        name = arr[arr.length-2];
    }    
    if( name.contains("#")) {
        var pos = name.lastIndexOf("#");
        name = name.substring(0, pos);
    }
    path = path.replaceAll(" ", "%20"); // safari bug. path is returned encoded from window.location.pathname
    return name;
}

/**
 * 
 * Get the path of the resource which contains the given path
 * 
 */
function getFolderPath(path) {
    path = stripFragment(path); // remove any fragment like #section
    if( path.endsWith("/")) {
        path = path.substring(0, path.length-1);
    }
    var pos = path.lastIndexOf("/");
    return path.substring(0, pos);
}

/**
 *  If the given path is a folder (ie ends with a slash) return it. Otherwise, 
 *  strip the file portion and remove the collection path
 */
function toFolderPath(path) {
    path = stripFragment(path); // remove any fragment like #section
    var pos = path.lastIndexOf("/");
    return path.substring(0, pos);
}

/**
 * just removed the server portion of the href
 */
function getPathFromHref(href) {
    // eg http://blah.com/a/b -->> /a/b
    var path = href.substring(8); // drop protocol
    var pos = path.indexOf("/");
    path = path.substring(pos);
    return path;
}


function suffixSlash(href) {
    if( href.endsWith("/")) {
        return href;
    }
    return href + "/";
}


function profileImg(user) {
    var profileHref;
    if( user.photoHref ) {
        profileHref = user.photoHref;
    } else if( user.photoHash ) {
        profileHref = "/_hashes/files/" + user.photoHash + "";
    } else {
        profileHref = "/templates/apps/user/profile.png";
    }         
    var profilePic = "<img src='" + profileHref + "' alt='' />";            
    return profilePic;
}


/**
 * ReplaceAll by Fagner Brack (MIT Licensed)
 * Replaces all occurrences of a substring in a string
 */
String.prototype.replaceAll = function(token, newToken, ignoreCase) {
    var str, i = -1, _token;
    if((str = this.toString()) && typeof token === "string") {
        _token = ignoreCase === true? token.toLowerCase() : undefined;
        while((i = (
            _token !== undefined? 
            str.toLowerCase().indexOf(
                _token, 
                i >= 0? i + newToken.length : 0
                ) : str.indexOf(
                token,
                i >= 0? i + newToken.length : 0
                )
            )) !== -1 ) {
            str = str.substring(0, i)
            .concat(newToken)
            .concat(str.substring(i + token.length));
        }
    }
    return str;
};

/**
 * Evaluate a relative path from an absolute path to get an absolute path to he relative path from the absolute path
 * 
 */
function evaluateRelativePath(startFrom, relPath) {
    var arr = relPath.split("/");
    var href = startFrom;
    for( i=0; i<arr.length; i++) {        
        var part = arr[i];
        if( part == "..") {
            href = getFolderPath(href);
        } else if( part == ".") {
        // do nothing
        } else {
            href += "/" + part;
        }
    }
    return href;
}

function replaceSpecialChars(nameToUse) {
    nameToUse = nameToUse.replace("/", "");
    nameToUse = nameToUse.replace("'", "");
    nameToUse = nameToUse.replace("\"", "");
    nameToUse = nameToUse.replace("@", "-");
    nameToUse = nameToUse.replace(" ", "-");
    nameToUse = nameToUse.replace("?", "-");
    nameToUse = nameToUse.replace(":", "-");
    nameToUse = nameToUse.replace("--", "-");
    nameToUse = nameToUse.replace("--", "-");
    return nameToUse;
}

function pulseBorder(node) {
    node.animate({
        boxShadow: '0 0 30px #ED9DAE'
    }, 2000, function() {
        setTimeout(function() {
            node.animate({
                boxShadow: '0 0 0 #FFF'
            }, 1000);
        }, 5000);
    });
}

function stripFragment(href) {
    var i = href.indexOf("#");
    if( i > 0 ) {
        href = href.substring(0, i-1);
    }
    return href;
}

/*
 * http://javascriptbase64.googlecode.com/svn/trunk/base64.js
 * 
Copyright (c) 2008 Fred Palmer fred.palmer_at_gmail.com

Permission is hereby granted, free of charge, to any person
obtaining a copy of this software and associated documentation
files (the "Software"), to deal in the Software without
restriction, including without limitation the rights to use,
copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the
Software is furnished to do so, subject to the following
conditions:

The above copyright notice and this permission notice shall be
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
OTHER DEALINGS IN THE SOFTWARE.
*/
function StringBuffer()
{ 
    this.buffer = []; 
} 

StringBuffer.prototype.append = function append(string)
{ 
    this.buffer.push(string); 
    return this; 
}; 

StringBuffer.prototype.toString = function toString()
{ 
    return this.buffer.join(""); 
}; 

var Base64 =
{
    codex : "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=",

    encode : function (input)
    {
        var output = new StringBuffer();

        var enumerator = new Utf8EncodeEnumerator(input);
        while (enumerator.moveNext())
        {
            var chr1 = enumerator.current;

            enumerator.moveNext();
            var chr2 = enumerator.current;

            enumerator.moveNext();
            var chr3 = enumerator.current;

            var enc1 = chr1 >> 2;
            var enc2 = ((chr1 & 3) << 4) | (chr2 >> 4);
            var enc3 = ((chr2 & 15) << 2) | (chr3 >> 6);
            var enc4 = chr3 & 63;

            if (isNaN(chr2))
            {
                enc3 = enc4 = 64;
            }
            else if (isNaN(chr3))
            {
                enc4 = 64;
            }

            output.append(this.codex.charAt(enc1) + this.codex.charAt(enc2) + this.codex.charAt(enc3) + this.codex.charAt(enc4));
        }

        return output.toString();
    },

    decode : function (input)
    {
        var output = new StringBuffer();

        var enumerator = new Base64DecodeEnumerator(input);
        while (enumerator.moveNext())
        {
            var charCode = enumerator.current;

            if (charCode < 128)
                output.append(String.fromCharCode(charCode));
            else if ((charCode > 191) && (charCode < 224))
            {
                enumerator.moveNext();
                var charCode2 = enumerator.current;

                output.append(String.fromCharCode(((charCode & 31) << 6) | (charCode2 & 63)));
            }
            else
            {
                enumerator.moveNext();
                var charCode2 = enumerator.current;

                enumerator.moveNext();
                var charCode3 = enumerator.current;

                output.append(String.fromCharCode(((charCode & 15) << 12) | ((charCode2 & 63) << 6) | (charCode3 & 63)));
            }
        }

        return output.toString();
    }
}


function Utf8EncodeEnumerator(input)
{
    this._input = input;
    this._index = -1;
    this._buffer = [];
}

Utf8EncodeEnumerator.prototype =
{
    current: Number.NaN,

    moveNext: function()
    {
        if (this._buffer.length > 0)
        {
            this.current = this._buffer.shift();
            return true;
        }
        else if (this._index >= (this._input.length - 1))
        {
            this.current = Number.NaN;
            return false;
        }
        else
        {
            var charCode = this._input.charCodeAt(++this._index);

            // "\r\n" -> "\n"
            //
            if ((charCode == 13) && (this._input.charCodeAt(this._index + 1) == 10))
            {
                charCode = 10;
                this._index += 2;
            }

            if (charCode < 128)
            {
                this.current = charCode;
            }
            else if ((charCode > 127) && (charCode < 2048))
            {
                this.current = (charCode >> 6) | 192;
                this._buffer.push((charCode & 63) | 128);
            }
            else
            {
                this.current = (charCode >> 12) | 224;
                this._buffer.push(((charCode >> 6) & 63) | 128);
                this._buffer.push((charCode & 63) | 128);
            }

            return true;
        }
    }
}

function Base64DecodeEnumerator(input)
{
    this._input = input;
    this._index = -1;
    this._buffer = [];
}

Base64DecodeEnumerator.prototype =
{
    current: 64,

    moveNext: function()
    {
        if (this._buffer.length > 0)
        {
            this.current = this._buffer.shift();
            return true;
        }
        else if (this._index >= (this._input.length - 1))
        {
            this.current = 64;
            return false;
        }
        else
        {
            var enc1 = Base64.codex.indexOf(this._input.charAt(++this._index));
            var enc2 = Base64.codex.indexOf(this._input.charAt(++this._index));
            var enc3 = Base64.codex.indexOf(this._input.charAt(++this._index));
            var enc4 = Base64.codex.indexOf(this._input.charAt(++this._index));

            var chr1 = (enc1 << 2) | (enc2 >> 4);
            var chr2 = ((enc2 & 15) << 4) | (enc3 >> 2);
            var chr3 = ((enc3 & 3) << 6) | enc4;

            this.current = chr1;

            if (enc3 != 64)
                this._buffer.push(chr2);

            if (enc4 != 64)
                this._buffer.push(chr3);

            return true;
        }
    }
};
