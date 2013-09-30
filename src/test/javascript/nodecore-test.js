module("nodecore");

// Test functions.
var t = {};

// A function with side effects.
t.always42 = function () {
    return 42;
};

test("evaluate simple network", function () {
    var net;
    net = {
        name: 'root',
        function: 'core/zero',
        outputRange: 'LIST',
        renderedChild: 'negate1',
        children: [
            {
                name: 'number1',
                function: 'math/number',
                outputRange: 'VALUE',
                ports: [
                    {
                        name: 'value',
                        type: 'float',
                        value: 42
                    }
                ]
            },
            {
                name: 'negate1',
                function: 'math/negate',
                outputRange: 'VALUE',
                ports: [
                    {
                        name: 'value',
                        type: 'float'
                    }
                ]
            }
        ],
        connections: [
            {
                outputNode: 'number1',
                inputNode: 'negate1',
                inputPort: 'value'
            }
        ]
    };

    deepEqual(nodecore.evaluateNetwork(net), [-42]);
});

test("evaluate nodes that return lists", function () {
    var net;
    net = {
        name: 'root',
        function: 'core/zero',
        outputRange: 'LIST',
        renderedChild: 'negate1',
        children: [
            {
                name: 'makeNumbers1',
                function: 'math/makeNumbers',
                outputRange: 'LIST',
                ports: [
                    {
                        name: 's',
                        type: 'string',
                        value: '11;22;33'
                    },
                    {
                        name: 'sep',
                        type: 'string',
                        value: ';'
                    },
                ]
            },
            {
                name: 'negate1',
                function: 'math/negate',
                outputRange: 'VALUE',
                ports: [
                    {
                        name: 'value',
                        type: 'float'
                    }
                ]
            }
        ],
        connections: [
            {
                outputNode: 'makeNumbers1',
                inputNode: 'negate1',
                inputPort: 'value'
            }
        ]
    };

    deepEqual(nodecore.evaluateNetwork(net), [-11, -22, -33]);
});

test("evaluate nodes that take in lists", function () {
    var net;
    net = {
        name: 'root',
        function: 'core/zero',
        outputRange: 'LIST',
        renderedChild: 'count1',
        children: [
            {
                name: 'makeNumbers1',
                function: 'math/makeNumbers',
                outputRange: 'LIST',
                ports: [
                    {
                        name: 's',
                        type: 'string',
                        value: '11;22;33'
                    },
                    {
                        name: 'sep',
                        type: 'string',
                        value: ';'
                    },
                ]
            },
            {
                name: 'count1',
                function: 'list/count',
                outputRange: 'VALUE',
                ports: [
                    {
                        name: 'list',
                        type: 'list',
                        range: 'LIST'
                    }
                ]
            }
        ],
        connections: [
            {
                outputNode: 'makeNumbers1',
                inputNode: 'count1',
                inputPort: 'list'
            }
        ]
    };

    deepEqual(nodecore.evaluateNetwork(net), [3]);
});

test("evaluate nodes that return lists and take in lists", function () {
    var net;
    net = {
        name: 'root',
        function: 'core/zero',
        outputRange: 'LIST',
        renderedChild: 'repeat1',
        children: [
            {
                name: 'makeNumbers1',
                function: 'math/makeNumbers',
                outputRange: 'LIST',
                ports: [
                    {
                        name: 's',
                        type: 'string',
                        value: '11;22;33'
                    },
                    {
                        name: 'sep',
                        type: 'string',
                        value: ';'
                    },
                ]
            },
            {
                name: 'repeat1',
                function: 'list/repeat',
                outputRange: 'LIST',
                ports: [
                    {
                        name: 'list',
                        type: 'list',
                        range: 'LIST'
                    },
                    {
                        name: 'amount',
                        type: 'int',
                        value: 3,
                        range: 'VALUE'
                    },
                    {
                        name: 'per_item',
                        type: 'boolean',
                        value: false,
                        range: 'VALUE'
                    }

                ]
            }
        ],
        connections: [
            {
                outputNode: 'makeNumbers1',
                inputNode: 'repeat1',
                inputPort: 'list'
            }
        ]
    };

    deepEqual(nodecore.evaluateNetwork(net), [11, 22, 33, 11, 22, 33, 11, 22, 33]);
});

test("evaluate nodes that return lists and take in lists", function () {
    var net;

    net = {
        name: 'root',
        function: 'core/zero',
        renderedChild: 'sideEffect',
        children: [
            {
                name: 'sideEffect',
                function: 't/always42',
                ports: []
            }
        ]
    };

    deepEqual(nodecore.evaluateNetwork(net), [42]);
});