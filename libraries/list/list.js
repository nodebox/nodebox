var list = {};

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

// Count the number of items in the list.
list.count = _.size;

// Remove all items where the corresponding boolean is false.
list.cull = function (l, booleans) {
    if (_.isEmpty(l)) return [];
    if (_.isEmpty(booleans)) return l;
    var results = [];
    for (var i = 0; i < l.length; i++) {
        // Cycle through the list of boolean values.
        var keep = booleans[i % booleans.length];
        if (keep) {
            results.push(l[i]);
        }
    }
    return results;
};

// Remove all duplicate items from the list.
list.distinct = _.uniq;

// Take the first item of the list.
list.first = _.first;

// Take the last item of the list.
list.last = _.last;

// Pick items from the list in random order.
list.pick = function (l, amount, seed) {
    if (_.isEmpty(l) || amount <= 0) return [];
    var rand = core.randomGenerator(seed);
    var results = [];
    for (var i = 0; i < amount; i++) {
        results.push(l[Math.floor(rand(0, l.length))]);
    }
    return results;
};

// Repeat the list a number of times. If perItem, item 1 will be repeated first, then item 2, and so on.
list.repeat = function (l, amount, perItem) {
    if (amount <= 0) return [];
    var newList = [];
    if (!perItem) {
        _.times(amount, function () {
            newList.push.apply(newList, l);
        });
    } else {
        _.map(l, function (v) {
            for (var i = 0; i < amount; i++) {
                newList.push(v);
            }
        });
    }
    return newList;
};

// Take all but the first item of the list.
list.rest = _.rest;

// Reverse the list.
list.reverse = function (l) {
    return _.clone(l).reverse();
};

// Take the second item of the list.
list.second = function (l) {
    if (l != null && l.length >= 2) {
        return l[1];
    } else {
        return null;
    }
};

// Move items at the beginning of the list to the end.
list.shift = function (l, amount) {
    // If the amount is bigger than the number of items, wrap around.
    amount = amount % l.length;
    var head = l.slice(0, amount);
    var tail = l.slice(amount);
    return tail.concat(head);
};

// Randomize the ordering of items in the list.
list.shuffle = function (l, seed) {

    function swap(l, i, j) {
        var tmp = l[i];
        l[i] = l[j];
        l[j] = tmp;
    }

    var rand = core.randomGenerator(seed);
    var shuffled = _.clone(l);
    for (var i = shuffled.length; i > 1; i--) {
        swap(shuffled, i - 1, Math.floor(rand(0, i)));
    }
    return shuffled;
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

// Sort items in the list.
list.sort = function (l) {
    return _.clone(l).sort();
};

// Switch between multiple inputs.
list.switch = function (l1, l2, l3, index) {
    var inputs = [];
    if (l1 !== null) inputs.push(l1);
    if (l2 !== null) inputs.push(l2);
    if (l3 !== null) inputs.push(l3);
    if (_.isEmpty(inputs)) return null;
    index = index % inputs.length;
    if (index < 0) index += inputs.length;
    return inputs[index];
};
