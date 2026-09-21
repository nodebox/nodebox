# Start working with g.js

When working with code it is also possible to use the graphic functions behind g.js. Using code is for some cases faster than working with nodes.

All functions / nodes are documented. You can call for the documentation by selecting a node to reveil the parameter window and then by clicking on the <button class="btn"><i class="fa fa-question"></i></button> button in the parameter window.

![Screenshot of the Documentation panel](start-code-g-doc.png).

## De Jong Attractor

Complex things such as attractors are more easy to write in code compared to creating them over nodes. De Jong's attractor is based on the following function:

    xn+1 = sin(a yn) - cos(b xn)
    yn+1 = sin(c xn) - cos(d yn)

Press the <button class="btn"><i class="fa fa-plus"></i><label>Code</label></button> button. Name the new function "dejong" and enter the following code:

    tester.dejong = function (amount, a, b, c, d, scaler) {
    var x, y, x1, y1;
    var all = [];
    x = 0;
    y = 0;
    x1 = 0;
    y1 = 0;
    for (var i = 0; i < amount; i++) {
        x = Math.sin(a * y1) - Math.cos(b * x1);
        y = Math.sin(c * x1) - Math.cos(d * y1);
        x1 = x;
        y1 = y;
        var p = new g.ellipse({x: scaler * x,y: scaler * y}, 2, 2);
        all.push(p);
        }
    return all;
    };

Press the <button class="btn"><label>Metadata</label></button> button on top of the [network editor](guide:network-editor) and click on the <button class="btn"><i class="fa fa-refresh"></i><label>Sync</label></button> button. In order to obtain a list of values toggle on the **returns list** option in the same panel.

Drag the new function into the main network and try it out.

1. Set **amount** parameter to **20000**.
2. Set parameters **a** to **0.97**, **b** to **-1.5**, **c** to **1.51** and **d** to **1.67**.
3. Set **scale** parameter to **100**.

![Screenshot of the De Jong Attractor example](start-code-g-dejong.png).

## Recursive drawing.

Following example shows to work with feedback. Press the <button class="btn"><i class="fa fa-plus"></i><label>Code</label></button> button. Name the new function "recursive" and enter the following code:

    tester.recursive = function (x, y, level, radius) {
    var all = [];
    tester.drawShape(x, y, radius / 2, level, all);
    return all;
    };

    tester.drawShape = function (x, y, radius, level, all) {
    var c = new g.rect({x: x, y: y}, radius, radius);
    // var c = new g.ellipse({x: x, y: y}, radius, radius);
    all.push(c);
    if (level > 1) {
        var levell = level - 1;
        tester.drawShape(x - radius / 2, y/2, radius / 2, levell, all);
        tester.drawShape(x + radius / 2, y/2, radius / 2, levell, all);

        }
    };

Press the <button class="btn"><label>Metadata</label></button> button on top of the [network editor](guide:network-editor) and click on the <button class="btn"><i class="fa fa-refresh"></i><label>Sync</label></button> button. In order to obtain a list of values toggle on the **returns list** option in the same panel.

Drag the new function into the main network and try it out.

1. Set **x** parameter to **0**.
1. Set **y** parameter to **0**.
1. Set **level** parameter to **6**.
1. Set **radius** parameter to **1000**.

The output are g.rect so it can be connected to core functions.

Create a `colorize` node and set **fill** to **transparent**, **stroke** to **black** and **strokeWidth** to **1**. Connect `recursive1` to the **shape** port.

![Screenshot of the recursive example](start-code-g-recursive.png).

## Total g.

All libraries can be called for in a script. Below is a moiree example that incorporates several of the available functions.

    tester.moiree = function (v) {
    var gr = g.grid(30, 30, 300, 300, {x: 0, y: 0});
    var sorted = g.sort(gr, 'distance', {x: 0, y: 0});
    var sliced = list.slice(sorted,0,370);
    var shape = g.ellipse({x: 0, y: 0}, 5, 5);
    var tg = []
    for (var i = 0; i < sliced.length; i++) {
        var p = g.translate(shape, {x: sliced[i].x, y: sliced[i].y});
        tg.push(p);
    }
    var grouped = g.group(tg);
    var animate = anim.wave(0, 360, .2, 'sine');
    var offset = 9;
    var copies = g.copy(grouped, 4,'rts',{x: 0, y: 0},animate+offset,{x: 0, y: 0});
    return copies
    };

Click on the <button class="btn"><i class="fa fa-play"></i><label>Play</label></button> button on top of the viewer window to see it animate.

![Screenshot of the moiree example](start-code-g-moiree.png).

## interchangeable use.

No matter how you create your functions, they can be used interchangeably. The screenhot below shows an example that incorporates:

1.  code functions (the dejong example as shown before).
2.  user node/network functions(a custom tail function, a custom eye function.
3.  core functions such as `takeEvery`, `sort` and `centroid`.

![Screenshot of the moiree example](start-code-g-bunny.png).

![Screenshot of the moiree example](start-code-g-bunnynet.png).
