var math = {};

math.number = function (v) {
    return v;
};

math.integer = function (v) {
    return Math.floor(v);
};

math.makeBoolean = function (v) {
    return v ? true : false;
};

math.add = function (v1, v2) {
    return v1 + v2;
};

math.subtract = function (v1, v2) {
    return v1 - v2;
};

math.multiply = function (v1, v2) {
    return v1 * v2;
};

math.divide = function (v1, v2) {
    return v1 / v2;
};

math.mod = function (v1, v2) {
    return v1 % v2;
};

math.sqrt = Math.sqrt;

math.pow = Math.pow;

math.log = Math.log;

math.even = function (v) {
    return v % 2 === 0;
};

math.odd = function (v) {
    return v % 2 !== 0;
};

math.negate = function (v) {
    return -v;
};

math.abs = Math.abs;

math.noValues = _.isEmpty;

math.sum = function (values) {
    if (values.length === 0) {
        return 0;
    } else {
        var sum = values[0];
        for (var i = 1; i < values.length; i++) {
            sum += values[i];
        }
        return sum;
    }
};

math.average = function (values) {
    if (values === null || values.length === 0) return 0;
    return math.sum(values) / values.length;
};

math.max = _.max;

math.min = _.min;

math.ceil = Math.ceil;

math.floor = Math.floor;

math.compare = function (v1, v2, comparator) {
    if (comparator === '<') {
        return v1 < v2;
    } else if (comparator === '>') {
        return v1 > v2;
    } else if (comparator === '>') {
        return v1 > v2;
    } else if (comparator === '<=') {
        return v1 <= v2;
    } else if (comparator === '>=') {
        return v1 >= v2;
    } else if (comparator === '==') {
        return v1 == v2;
    } else if (comparator === '!=') {
        return v1 != v2;
    } else {
        return false;
    }
};

math.logicOR = function (v1, v2, comparator) {
    if (comparator === 'or') {
        return v1 || v2;
    } else if (comparator === 'and') {
        return v1 && v2;
    } else if (comparator === 'xor') {
        return v1 ^ v2;
    } else {
        return false;
    }
};

math.sin = Math.sin;
math.cos = Math.cos;
math.tan = Math.tan;

Math.pi = function () {
    return Math.PI;
};

math.e = function () {
    return Math.E;
};

math.sample = function(amount, start, end) {
    if (amount === 0) return [];
    if (amount === 1) return [start + (end - start) / 2];

    var step = (end - start) / (amount - 1);
    var values = [];
    for (var i=0;i<amount;i++) {
        values.push(start + step * i);
    }
    return values;
};
