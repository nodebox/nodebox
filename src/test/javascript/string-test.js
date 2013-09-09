module("string");

test("string", function () {
    ok(string.string("hello") === "hello");
});

test("makeStrings", function () {
    deepEqual(string.makeStrings("a;b", ";"), ["a", "b"]);
    deepEqual(string.makeStrings("a;b", ""), ["a", ";", "b"]);
    deepEqual(string.makeStrings("hello", ""), ["h", "e", "l", "l", "o"]);
    deepEqual(string.makeStrings("a b c", " "), ["a", "b", "c"]);
    deepEqual(string.makeStrings("a; b; c", ";"), ["a", " b", " c"]);
    deepEqual(string.makeStrings(null, ";"), []);
    deepEqual(string.makeStrings(null, null), []);
});

test("length", function () {
    equal(string.length(null), 0);
    equal(string.length(""), 0);
    equal(string.length("bingo"), 5);
});

test("wordCount", function () {
    equal(string.wordCount(null), 0);
    equal(string.wordCount(""), 0);

    equal(string.wordCount("a"), 1);
    equal(string.wordCount("a_b"), 1);

    equal(string.wordCount("a b"), 2);
    equal(string.wordCount("a-b"), 2);
    equal(string.wordCount("a,b"), 2);
    equal(string.wordCount("a.b"), 2);
});

test("concatenate", function () {
    equal(string.concatenate("a", null, null, null), "a");
    equal(string.concatenate("a", "b", null, null), "ab");
    equal(string.concatenate("a", null, null, "d"), "ad");
    equal(string.concatenate(null, null, "c", "d"), "cd");
    equal(string.concatenate(null, null, null, null), "");
});

test("changeCase", function () {
    equal(string.changeCase("abC DEfg hi", "lowercase"), "abc defg hi");
    equal(string.changeCase("abc defg hi", "uppercase"), "ABC DEFG HI");
    equal(string.changeCase("abc defg hi", "titlecase"), "Abc Defg Hi");
});

test("characters", function () {
    deepEqual(string.characters("a b"), ["a", " ", "b"]);
    deepEqual(string.characters("a;b"), ["a", ";", "b"]);
    deepEqual(string.characters("hello"), ["h", "e", "l", "l", "o"]);
});