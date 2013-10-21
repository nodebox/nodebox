var string = {};

string.string = function (s) {
    return s;
}

string.makeStrings = function (s, separator) {
    if (s == null) return [];
    if (separator == null || _.isEmpty(separator))
        separator = "";
    return s.split(separator);
};

string.length = function (s) {
    if (s == null) return 0;
    return s.length;
};

string.wordCount = function (s) {
    if (s == null) return 0;
    split = s.split(new RegExp("\\w+"));
    return split.length - 1;
};

string.concatenate = function (s1, s2, s3, s4) {
    s1 = s1 != null ? s1 : "";
    s2 = s2 != null ? s2 : "";
    s3 = s3 != null ? s3 : "";
    s4 = s4 != null ? s4 : "";
    return s1 + s2 + s3 + s4;
};

string.changeCase = function (value, caseMethod) {
    caseMethod = caseMethod.toLowerCase();
    if (caseMethod === "lowercase") {
        return value.toLowerCase();
    } else if (caseMethod === "uppercase") {
        return value.toUpperCase();
    } else if (caseMethod === "titlecase") {
        return _.reduce(value, function(x, y) {
          if (x.length === 0 || x[x.length - 1] === " ")
            return x + y.toUpperCase();
          else
            return x + y;
        }, "");
    } else {
        return value;
    }
};

string.characters = function (s) {
    if (s == null) return [];
    return s.split("");
};

string.characterAt = function (s, index) {
    if (s == null || _.isEmpty(s)) return null;
    index--;
    index = index % s.length;
    return s.charAt(index);
};

string.contains = function (s, value) {
    if (s == null || value == null) return false;
    return s.indexOf(value) !== -1;
};

string.endsWith = function (s, value) {
    if (s == null || value == null) return false;
    return s.indexOf(value, s.length - value.length) !== -1;
};

string.startsWith = function (s, value) {
    if (s == null || value == null) return false;
    return s.indexOf(value) === 0;
};

string.subString = function (s, start, end, endOffset) {
    if (s == null) return null;

    start--;
    end--;
    start = start % s.length;

    if (endOffset) {
        end = (end % s.length) + 1;
    } else {
        end = end % (s.length + 1);
    }
    return s.substring(start, end);
};

string.trim = function (s) {
    if (s == null) return null;
    return s.trim();
};

string.equal = function (s, value, caseSensitive) {
    if ((s == null) || (value == null)) {
        return false;
    }
    if (caseSensitive) {
        return s === value;
    } else {
        return s.toLowerCase() === value.toLowerCase();
    }
};
