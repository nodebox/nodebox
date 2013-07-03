// Import and manipulate all kinds of data.

var data = {};

// Import a CSV file from a file and return a list of maps. The first row is parsed as the header row: its names
// will serve as keys for the maps we return.
//
// url                 - The url of the file to read in.
// delimiter           - The name of the character delimiting column values.
// quotationCharacter  - The name of the character acting as the quotation separator.
// numberSeparator     - The character used to separate the fractional part.
//
// Returns A list of maps.
data.importCSV = function (url, delimiter, quotationCharacter, numberSeparator) {
    delimiter = delimiter || ',';
    quotationCharacter = quotationCharacter || '"';
    numberSeparator = numberSeparator || '.';

    if (/^http(s)?::\//.test(url)) {
        // TODO Importing a file should happen using an asynchronous request.
    } else {
        // TODO The parsing doesn't take quotation into account.
        // Assume the url is the actual data of the CSV file to import.
        var data = url;

        // Split by rows
        var rows = data.split("\n");
        var headerRow = rows[0].split(delimiter);

        return _.map(rows.slice(1), function (row) {
            var obj = {};
            row = row.split(delimiter);
            for (var i = 0; i < row.length; i++) {
                obj[headerRow[i]] = row[i];
            }
            return obj;
        });
    }
};

// Lookup a key in an object.
// If the key contains dots ("foo.bar") the lookup will be recursive.
data.lookup = function (obj, key) {
    if (key === null) return null;
    var keys = key.split(".");
    for (var i = 0; i < keys.length; i++) {
        obj = data._fastLookup(obj, key[i]);
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
