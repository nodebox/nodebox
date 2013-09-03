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

test("round", function() {
    equal(math.round(2.60), 3);
    equal(math.round(2.50), 3);
    equal(math.round(2.49), 2);
    equal(math.round(-2.60), -3);
    equal(math.round(-2.50), -2);
    equal(math.round(-2.49), -2);
});

test("radians", function() {
    equal(math.radians(0), 0);
    equal(math.radians(180), math.pi());
    equal(math.radians(360), 2 * math.pi());
});

test("degrees", function() {
    equal(math.degrees(0), 0);
    equal(math.degrees(math.pi() / 2), 90);
    equal(math.degrees(math.pi()), 180);
});

test("distance", function() {
    equal(math.distance({x: 0, y: 0}, {x: 0, y: 0}), 0);
    equal(math.distance({x: 0, y: 0}, {x: 100, y: 0}), 100);
    equal(math.distance({x: 100, y: 0}, {x: 0, y: 0}), 100);
    equal(math.distance({x: 0, y: 0}, {x: 0, y: 100}), 100);
    equal(math.distance({x: 0, y: 100}, {x: 0, y: 0}), 100);
    equal(math.distance({x: 0, y: 0}, {x: 40, y: 75}), 85);
    equal(math.distance({x: -4, y: -3}, {x: 4, y: 3}), 10);
});

test("angle", function() {
    equal(math.angle({x: 0, y: 0}, {x: 100, y: 100}), 45);
    equal(math.angle({x: -4, y: -3}, {x: 3, y: -10}), -45);
    equal(math.angle({x: 20, y: 15}, {x: 20, y: 115}), 90);

});

function pointsFuzzyEqual(p1, p2, decimals) {
    equal(p1.x.toFixed(decimals), p2.x);
    equal(p1.y.toFixed(decimals), p2.y);
}

test("coordinates", function() {
    pointsFuzzyEqual(math.coordinates({x: 30, y: 35}, 45, 65), {x: 75.96, y: 80.96}, 2);
    pointsFuzzyEqual(math.coordinates({x: 30, y: 35}, -10, -70), {x: -38.94, y: 47.16}, 2);
    pointsFuzzyEqual(math.coordinates({x: 45, y: 45}, 90, 100), {x: 45, y: 145}, 3);
});

test("reflect", function() {
    pointsFuzzyEqual(math.reflect({x: 50, y: 50}, {x: 100, y: 100}, 120, 2), {x: -86.6, y: 86.6}, 2);
    pointsFuzzyEqual(math.reflect({x: 50, y: 50}, {x: 60, y: 70}, 220, 1), {x: 55.2, y: 28.25}, 2);

});