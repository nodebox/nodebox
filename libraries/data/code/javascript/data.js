// Import and manipulate all kinds of data.

var data = {};

data.getCSVParser = function () {
    if (data._csv === undefined) {
        var jQuery = {};
        (function f() {
            var xhr = new XMLHttpRequest();
            xhr.open('GET', 'jquery.csv.js', false);
            xhr.onreadystatechange = function(){
                if (xhr.readyState === 4) {
                    if(window.location.protocol === "file:" || xhr.status === 200) {
                        eval(xhr.response);
                        //onCompleteFunction(assetURL, xhr.response);
                    }
                }
            };
            xhr.send(null);
        })();
        data._csv = jQuery.csv;
    }
    return data._csv;
}

data.separators = {"period": '.',
                   "comma": ',',
                   "semicolon": ';',
                   "tab": '\t',
                   "space": ' ',
                   "double": '\"',
                   "single": '\''};

// Import a CSV file from a file and return a list of maps. The first row is parsed as the header row: its names
// will serve as keys for the maps we return.
//
// url                 - The url of the file to read in.
// separator           - The name of the character separating column values.
// quotationCharacter  - The name of the character acting as the quotation separator.
// numberSeparator     - The character used to separate the fractional part.
//
// Returns A list of maps.
data.importCSV = function (url, separator, quotationCharacter, numberSeparator) {
    separator = data.separators[separator] || ',';
    quotationCharacter = quotationCharacter || '"';
    numberSeparator = numberSeparator || '.';

    var d = ndbx.assets['data/' + url],
        options = {separator: separator, delimiter: quotationCharacter},
        csv = data.getCSVParser(),
        objects = csv.toObjects(d);

    if (!_.isEmpty(objects)) {
        _.each(Object.keys(objects[0]), function(name) {
            var nan = false;
            _.each(objects, function(obj) {
                if (isNaN(obj[name])) {
                    nan = true;
                }
            });
            if (!nan) {
                _.each(objects, function(obj) {
                    obj[name] = parseFloat(obj[name]);
                });
            }
        });

    }

    return objects;
};

// Lookup a key in an object.
// If the key contains dots ("foo.bar") the lookup will be recursive.
data.lookup = function (obj, key) {
    if (key === null) return null;
    var keys = key.split(".");
    for (var i = 0; i < keys.length; i++) {
        obj = data._fastLookup(obj, keys[i]);
        if (obj === null) break;
    }
    return obj;
};

// Filter the data based on key/value.
data.filterData = function (data, key, op, value) {
    if (_.isEmpty(value)) return data;
    return _.filter(data, function (row) {
        var obj = data._fastLookup(row, key);
        if (op === '==' && obj == value) {
            return true;
        } else if (op === '!=' && obj != value) {
            return true;
        } else if (op === '>' && obj > value) {
            return true;
        } else if (op === '>=' && obj >= value) {
            return true;
        } else if (op === '<' && obj < value) {
            return true;
        } else if (op === '<=' && obj <= value) {
            return true;
        } else {
            return false;
        }
    });
};

data._fastLookup = function (obj, key) {
    if (obj === null || key === null) return null;
    return obj[key];
};

// Execute an Ajax request asynchronously. This function returns immediately; you should supply a callback function
// that will be called some time in the future with a response.
data.xhr = function (url, response) {
    var request = new XMLHttpRequest();

    var respond = function () {
        var statusCode = request.status;
        response(request.responseText, {statusCode: statusCode});
    };


    if ("onload" in request) {
        request.onload = respond;
        request.onerror = respond;
    } else {
        request.onreadystatechange = function () {
            if (request.readyState > 3) {
                respond();
            }
        }
    }

    request.open('get', url, true);
    request.send(null);
};
