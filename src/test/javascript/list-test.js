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
