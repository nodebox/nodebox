test("pick", function () {
    deepEqual(list.pick([1, 2, 3], 1, 1), [1]);
    deepEqual(list.pick([1, 2, 3], 1, 5), [2]);
    deepEqual(list.pick([6], 3, 1), [6, 6, 6]);
    deepEqual(list.pick(_.range(0, 9), 3, 1), [2, 4, 3]);
});


test("repeat", function () {
    deepEqual(list.repeat([1, 2, 3], 2), [1, 2, 3, 1, 2, 3]);
    deepEqual(list.repeat([1, 2, 3], 0), []);
    deepEqual(list.repeat([1, 2, 3], -5), []);
    deepEqual(list.repeat([1, 2, 3], 2, true), [1, 1, 2, 2, 3, 3]);
});

test("reverse", function () {
    var a = [1, 2, 3];
    deepEqual(list.reverse(a), [3, 2, 1]);
    // Make sure the original list is unchanged.
    deepEqual(a, [1, 2, 3]);
});

test("shift", function () {
    var l = [1, 2, 3];
    deepEqual(list.shift(l, 0), [1, 2, 3]);
    deepEqual(list.shift(l, 1), [2, 3, 1]);
    deepEqual(list.shift(l, 2), [3, 1, 2]);
    deepEqual(list.shift(l, 3), [1, 2, 3]);
    deepEqual(list.shift(l, 4), [2, 3, 1]);

    deepEqual(list.shift(l, -1), [3, 1, 2]);
    deepEqual(list.shift(l, -2), [2, 3, 1]);

    deepEqual(list.shift([], 9), []);
    deepEqual(list.shift([1], 9), [1]);
});

test("shuffle", function () {
    deepEqual(list.shuffle([], 42), []);
    deepEqual(list.shuffle([1], 42), [1]);

    deepEqual(list.shuffle([1, 2, 3, 4, 5], 33), [1, 4, 5, 2, 3]);
    deepEqual(list.shuffle([1, 2, 3, 4, 5], 99), [4, 3, 5, 2, 1]);
    deepEqual(list.shuffle([1, 2, 3, 4, 5], 42), [1, 2, 3, 4, 5]);
});

test("slice", function () {
    var l = [1, 2, 3, 4, 5];
    deepEqual(list.slice(l, 0, 1), [1]);
    deepEqual(list.slice(l, 1, 1), [2]);
    deepEqual(list.slice(l, 0, 3), [1, 2, 3]);
    deepEqual(list.slice(l, 0, 3, true), [4, 5]);

    deepEqual(list.slice(l, -1, 5), []);
});

test("sort", function() {
    deepEqual(list.sort([]), []);
    deepEqual(list.sort([1]), [1]);
    deepEqual(list.sort([3, 2, 1]), [1, 2, 3]);
});

test("switch", function () {
    deepEqual(list.switch(1, 2, 3, 0), 1);
    deepEqual(list.switch([1], [2], [3], 0), [1]);
    deepEqual(list.switch([1], [2], [3], 5), [3], "index is wrapped");
    deepEqual(list.switch(1, null, 2, 1), 2, "skip inputs that are null");
    deepEqual(list.switch(null, null, null, 0), null, "return null if there are no inputs");
    deepEqual(list.switch(1, 2, 3, -1), 3, "index can be negative");
});
