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

math.pi = function () {
    return Math.PI;
};

math.e = function () {
    return Math.E;
};

math.sample = function (amount, start, end) {
    if (amount === 0) return [];
    if (amount === 1) return [start + (end - start) / 2];

    var step = (end - start) / (amount - 1);
    var values = [];
    for (var i=0;i<amount;i++) {
        values.push(start + step * i);
    }
    return values;
};

math.round = Math.round;

math.radians = function (degrees) {
    return degrees * Math.PI / 180;
};

math.degrees = function (radians) {
    return radians * 180 / Math.PI;
};

math.distance = function (p1, p2) {
    return Math.sqrt(Math.pow(p2.x - p1.x, 2) + Math.pow(p2.y - p1.y, 2));
};

math.angle = function (p1, p2) {
    return math.degrees(Math.atan2(p2.y - p1.y, p2.x - p1.x));
};

math.coordinates = function (p, angle, distance) {
    var x = p.x + Math.cos(math.radians(angle)) * distance;
    var y = p.y + Math.sin(math.radians(angle)) * distance;
    return {x: x, y: y};
};

math.reflect = function (p1, p2, angle, distance) {
    distance *= math.distance(p1, p2);
    angle += math.angle(p1, p2);
    return math.coordinates(p1, angle, distance);
};

math.makeNumbers = function (s, separator) {
    if (s == null || _.isEmpty(s)) return [];
    if (separator == null || _.isEmpty(separator))
        separator = "";
    return _.map(s.split(separator), parseFloat);
};

math.range = function (start, end, step) {
    if (step === 0 || start === end || (start < end && step < 0) || (start > end && step > 0))
        return [];
    var newList = [];
    var next = start;
    while ((step > 0 && next < end) || (step <= 0 && next > end)) {
        newList.push(next);
        next += step;
    }
    return newList;
};

math.clamp = function (v) {
  return 0 > v ? 0 : 1 < v ? 1 : v;
};

math.convertRange = function (value, srcMin, srcMax, targetMin, targetMax, overflowMethod) {
    if (overflowMethod === "wrap") {
        value = srcMin + value % (srcMax - srcMin);
    } else if (overflowMethod === "mirror") {
        var rest = value % (srcMax - srcMin);
        if ((value / (srcMax - srcMin)) % 2 == 1)
            value = srcMax - rest;
        else
            value = srcMin + rest;
    } else if (overflowMethod === "clamp") {
        value = math.clamp(value, srcMin, srcMax);
    }

    // Convert value to 0.0-1.0 range.
    try {
        value = (value - srcMin) / (srcMax - srcMin);
    } catch (e) {
        value = srcMin;
    }

    // Convert value to target range.
    return targetMin + value * (targetMax - targetMin);
};

math.runningTotal = function (numbers) {
    if (numbers == null || _.isEmpty(numbers)) return [0.0];
    var currentTotal = 0;
    var b = [];
    for (var i = 0; i < numbers.length; i++) {
        b.push(currentTotal);
        currentTotal += numbers[i];
    }
    return b;
};

