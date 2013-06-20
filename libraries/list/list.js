var list = {};

// Count the number of items in the list.
list.count = _.size;

// Take the first item of the list.
list.first = _.first;

// Take the second item of the list.
list.second = function (l) {
    if (l != null && l.length >= 2) {
        return l[1];
    } else {
        return null;
    }
};

// Take all but the first item of the list.
list.rest = _.rest;

// Take the last item of the list.
list.last = _.last;

// Combine multiple lists into one.
list.combine = function () {
    var result = [];
    for (var i = 0; i < arguments.length; i++) {
        var l = arguments[i];
        if (l) {
            result = result.concat(l);
        }
    }
    return result;
};

// Take a portion of the original list.
list.slice = function (l, startIndex, size, invert) {
    if (l == null) return [];

    if (!invert) {
        return l.slice(startIndex, startIndex + size);
    } else {
        var firstList = l.slice(0, startIndex);
        var secondList = l.slice(startIndex + size);
        return firstList.concat(secondList);
    }
};