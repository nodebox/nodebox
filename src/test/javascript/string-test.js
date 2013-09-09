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

test("characterAt", function() {
    equal(string.characterAt("default", 1), "d");
    equal(string.characterAt("default", 3), "f");
    equal(string.characterAt("default", 8), "d");
});

test("contains", function () {
    ok(string.contains("default", "") === true);
    ok(string.contains("default") === false);
    ok(string.contains("default", "efa") === true);
    ok(string.contains("default", "efi") === false);
});

test("endsWith", function () {
    ok(string.endsWith("default", "") === true);
    ok(string.endsWith("default") === false);
    ok(string.endsWith("default", "def") === false);
    ok(string.endsWith("default", "lt") === true);
    ok(string.endsWith("default", "lT") === false);
    ok(string.endsWith("default", "alt") === false);
});

test("startsWith", function () {
    ok(string.startsWith("default", "") === true);
    ok(string.startsWith("default") === false);
    ok(string.startsWith("default", "def") === true);
    ok(string.startsWith("Default", "def") === false);
    ok(string.startsWith("default", "lt") === false);
    ok(string.startsWith("default", "dex") === false);
});

test("subString", function () {
    equal(string.subString("Hello world!", 3, 7, false), "llo ");
    equal(string.subString("Hello world!", 1, 4, false), "Hel");
    equal(string.subString("Hello world!", 1, 4, true), "Hell");
});

test("trim", function() {
    equal(string.trim("  Hello world!"), "Hello world!");
    equal(string.trim("Hello world!   "), "Hello world!");
    equal(string.trim("  Hello world!   "), "Hello world!");
    equal(string.trim("Hello   world!"), "Hello   world!");
});