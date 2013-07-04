module("math");

test("number", function () {
    ok(math.number(42) === 42);
});

test("integer", function() {
    equal(math.integer(42), 42);
    equal(math.integer(42.9), 42);
    equal(math.integer(42.1), 42);
});

test("makeBoolean", function() {
    equal(math.makeBoolean(true), true);
    equal(math.makeBoolean(false), false);
    equal(math.makeBoolean(1), true);
    equal(math.makeBoolean(0), false);
    equal(math.makeBoolean("true"), true);
    equal(math.makeBoolean("false"), true);
    equal(math.makeBoolean("foo"), true);
});

test("add", function() {
    equal(math.add(1, 2), 3);
});

test("subtract", function() {
    equal(math.subtract(3, 2), 1);
});

test("multiply", function() {
    equal(math.multiply(2, 5), 10);
});

test("divide", function() {
    equal(math.divide(10, 2), 5);
    equal(math.divide(3, 2), 1.5);
});

test("mod", function() {
    equal(math.mod(10, 2), 0);
    equal(math.mod(10, 3), 1);
});

test("even", function() {
    ok(math.even(0) === true);
    ok(math.even(1) === false);
    ok(math.even(2) === true);
    ok(math.even(3) === false);
});

test("odd", function() {
    ok(math.odd(0) === false);
    ok(math.odd(1) === true);
    ok(math.odd(2) === false);
    ok(math.odd(3) === true);
});

test("noValues", function() {
    ok(math.noValues(null) === true);
    ok(math.noValues([]) === true);
    ok(math.noValues([0]) === false);
    ok(math.noValues([1, 2, 3]) === false);
});

test("sum", function() {
    equal(math.sum([]), 0);
    equal(math.sum([42]), 42);
    equal(math.sum([1, 2, 3]), 6);
});

test("average", function() {
    equal(math.average([]), 0);
    equal(math.average([42]), 42);
    equal(math.average([1, 2, 3]), 2);
});

test("compare", function() {
    ok(math.compare(2, 8, '<') === true);
    ok(math.compare(8, 2, '<') === false);
    ok(math.compare(5, 5, '<') === false);

    ok(math.compare(2, 8, '>') === false);
    ok(math.compare(8, 2, '>') === true);
    ok(math.compare(5, 5, '>') === false);

    ok(math.compare(2, 8, '<=') === true);
    ok(math.compare(8, 2, '<=') === false);
    ok(math.compare(5, 5, '<=') === true);

    ok(math.compare(2, 8, '>=') === false);
    ok(math.compare(8, 2, '>=') === true);
    ok(math.compare(5, 5, '>=') === true);

    ok(math.compare(2, 8, '==') === false);
    ok(math.compare(8, 2, '==') === false);
    ok(math.compare(5, 5, '==') === true);

    ok(math.compare(2, 8, '!=') === true);
    ok(math.compare(8, 2, '!=') === true);
    ok(math.compare(5, 5, '!=') === false);
});
