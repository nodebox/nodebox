var nodecore = {};

if (!console) {
    var console = {};
    var out = Packages.java.lang.System.out;

    console.log = function (s) {
        for (var i = 0; i < arguments.length; i++) {
            out['print(java.lang.Object)'](arguments[i]);
            out.print(' ');
        }
        out.print('\n');
    };
}

nodecore.findNode = function (network, nodeName) {
    return _.find(network.children, function (n) {
        return n.name === nodeName;
    });
};

nodecore.findPort = function (node, portName) {
    return _.find(node.ports, function (n) {
        return n.name === portName;
    });
};

nodecore.findConnectionByInput = function (network, nodeName, portName) {
    return _.find(network.connections, function (conn) {
        return conn.inputNode === nodeName && conn.inputPort === portName;
    });
};

// Get the size of the of the largest list.
nodecore.maxListSize = function (lists) {
    if (lists.length === 0) return 0;
    var listSizes = _.map(lists, function (l) {
        return l.length;
    });
    return _.reduce(listSizes, function (x, y) {
        return Math.max(x, y);
    });
};

// Cycle the items of the list up to a maximum amount of n.
nodecore.cycleList = function (n, l) {
    var length = l.length;
    var newList = [];
    for (var i = 0; i < n; i += 1) {
        newList.push(l[i % length]);
    }
    return newList;
};

// Cycle the items of all the lists.
// Find the size of the largest list and cycle the other ones.
nodecore.cycleLists = function (lists) {
    var n = nodecore.maxListSize(lists);
    return _.map(lists, _.partial(nodecore.cycleList, n));
};

// Given a list of argument lists, apply the function to them and return a list of results.
// The argument lists do not have to be the same size: smaller lists wrap around.
// Example: given you have a function "plus" that takes two numbers and adds them:
//    var addFn = function(a, b) { return a + b };
// You can call cycleMap with a list of argument lists:
//    var aList = [1, 2, 3, 4, 5];
//    var bList = [10, 20];
//    cycleMap(addFn, [aList, bList]);
//    ;=> [10, 22, 13, 24, 15]
nodecore.cycleMap = function (fn, argLists) {
    var results = [];
    var n = nodecore.maxListSize(argLists);
    var cycledArgLists = nodecore.cycleLists(argLists);
    for (var i = 0; i < n; i += 1) {
        var argList = [];
        for (var j = 0; j < cycledArgLists.length; j++) {
            argList.push(cycledArgLists[j][i]);
        }
        results.push(fn.apply(null, argList));
    }
    return results;
};

nodecore.lookupFunction = function (functionName) {
    var names = functionName.split('/');
    var ns = names[0];
    var name = names[1];
    var functionNamespace = window[ns];
    if (!functionNamespace) {
        console.log('Function namespace "' + functionNamespace + '" not found.');
        return null;
    }
    var fn = functionNamespace[name];
    if (!fn) {
        console.log('Function "' + functionName + '" not found.');
        return null;
    } else {
        return fn;
    }
};

nodecore.evaluateNetwork = function (network) {
    return nodecore.evaluateChild(network, network.renderedChild);
};

// Evaluate the result of a port in the network.
// Returns a list of results.
nodecore.evaluatePort = function (network, nodeName, portName) {
    var childNode = nodecore.findNode(network, nodeName);
    var childPort = nodecore.findPort(childNode, portName);
    var connection = nodecore.findConnectionByInput(network, nodeName, portName);
    if (connection) {
        var result = nodecore.evaluateChild(network, connection.outputNode);
        // TODO convert the result.
        return result;
    } else {
        // Wrap the value in a list of 1. List cycling takes care of the rest.
        return [childPort.value];
    }
};

// Evaluate a child node in the network.
// Returns a list of results.
nodecore.evaluateChild = function (network, nodeName) {
    var childNode = nodecore.findNode(network, nodeName);
    var portNames = _.pluck(childNode.ports, 'name');
    var argLists = _.map(portNames, _.partial(nodecore.evaluatePort, network, nodeName));
    var fn = nodecore.lookupFunction(childNode['function']);
    if (fn !== null) {
        var results = nodecore.cycleMap(fn, argLists);
        if (childNode.outputRange === 'LIST') {
            return results[0];
        } else {
            return results;
        }
    } else {
        console.log("Function " + childNode['function'] + " not found.", nodeName);
        return [];
    }
};

nodecore.renderLibrary = function (network, animate) {
    var result = nodecore.evaluateNetwork(network);
    console.log(result);
    var ctx = document.getElementById('c').getContext('2d');
    ctx.setTransform(1, 0, 0, 1, 0, 0);
    ctx.clearRect(0, 0, 300, 300);
    ctx.translate(150, 150);
    //grasp.drawOrigin(sourceCtx);
    ctx.fillStyle = 'black';
    g.draw(ctx, result);
    core._currentFrame += 1;
    if (animate) {
        setTimeout(function () {
            nodecore.renderLibrary(network, true);
        }, 16);
    }
};

var core = {};
core.zero = function () {
    return 0.0;
};

core._currentFrame = 1;
core.frame = function () {
    return core._currentFrame;
};

// Generate a random function that is seeded with the given value.
core.randomGenerator = function (seed) {
    // Based on random number generator from
    // http://indiegamr.com/generate-repeatable-random-numbers-in-js/
    return function (min, max) {
        min = min || 0;
        max = max || 1;
        seed = (seed * 9301 + 49297) % 233280;
        var v = seed / 233280;
        return min + v * (max - min);
    };
};

// Generate a random number with the given seed between min and max.
core.randomNumber = function (seed, min, max) {
    return core.randomGenerator(seed)(min, max);
};
