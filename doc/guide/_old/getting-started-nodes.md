# Getting started with nodes

If you've just created a new project, it should already contain a single rectangle node, called "rect1".

We want to start with a blank canvas so let's delete it first. Select the `rect1` node and then press the <button class="btn"><i class="fa fa-times"></i><label>Delete</label></button> button to remove it.

## Creating your first node

NodeBox Live is all about creating, changing and connecting nodes. You'll do that in the [Network Editor](guide:network-editor).

Creating a new node can be done in two ways:

- **Press** the <button class="btn"><i class="fa fa-plus"></i><label>New Node</label></button> button. The node will be positioned randomly.
- **Double click** somewhere in the [network editor](guide:network-editor). The node will be positioned where you click. (This is the way we do it!)

You'll end up in the "New Node" window:

![Screenshot of the New node Window](new-node.png)

Nodes are categorised according to their function (animation, graphics, math, ...). This list can grow if you [install new dependencies](guide:dependencies).

If you already know the name of the node you want to use, typing the first few letters narrows it down. Use the arrow keys to select the one you want and press enter to create it.

## Create a grid node

From the "New Node" window, create a `grid` node. Either type the name "grid" in the search bar and press enter, or select the _Graphics_ category, scroll down to "grid", and double-click.

NodeBox Live now creates a [grid](ref:g.grid) node called (named `grid1`). In the Viewer you can look at the output: a grid of 9 points (3 x 3) with its dimensions set to 100 x 100.

## Changing parameters

Clicking the node once brings up the parameter panel. Here, you can change the options for each node. Each parameter changes the visual output of the node. For example, setting the width parameter to 200 makes the grid wider.

![Screenshot of the parameter panel](start-nodes-parameter-panel.png)

You can change a parameter in two ways:

- Click the parameter value (e.g. the number `3`), type in a new value, and press enter to confirm. This allows for precise manipulation.
- You can also **drag** the parameter value. Simply hold your mouse cursor over the value and drag left or right. For values like position, you can drag in all four directions. The viewer will update live.

Now change the parameters of `grid1`:

- Set the **columns** to **10**. (Click the number `3` right of the columns cell, type in `10` and press enter)
- Set the **rows** to **20**.
- Set the **width** to **200**.
- Set the **height** to **400**.

Each time you make a change, the viewer updates to show the result.

## Create an ellipse node

Now, create an [ellipse](ref:g.ellipse). Just like before, either click the "New Node" icon or double-click somewhere in the network editor where you want to create the node. The node should be called `ellipse1`. Click it once to bring up the parameter panel, then set the **width** to **20** and the **height** to **20**.

Note that, as we created the ellipse node, the grid has dissappeared. That's because NodeBox Live only shows the output of one node at a time, called the "rendered node". You can **double-click** a node to set its render flag. Try double-clicking the `grid1` node and watch it show up in the viewer. Then, click the `ellipse1` node again to see the ellipse again.

The power of NodeBox Live comes from connecting nodes together. In this example, we want to create a grid of circles. Then, we want to give each circle a different size.

## Connect the two nodes into a flow network.

**NodeBox Live allows you to create connections between nodes**. We can connect the `grid1` and `ellipse1` nodes by dragging from the output port of the `grid1` node (located at te bottom of the node) to the **position** input port of the `ellipse1` node (the first "stub" at the top of `ellipse1`). Drag a connection from the output of `grid1` to the first input (**position**) of `ellipse1`.

Double-click the `ellipse1` node to render it and note that an ellipse is drawn on each point of the grid. Note that the `position` parameter is indicates as `<connected>`. This means that the value of this position comes from somewhere else. In this case, it comes from the points in the grid.

Currently, all ellipses have the same size, but we'll change that now.

![Screenshot of the example so far](start-nodes.png)

## Adding variation

In order to add variation in size for each one of the ellipses we need to **know the total amount** of ellipses. We will **use this information as a parameter** to set a number of random values. This goes over a few steps:

1.  Create a [count](ref:g.count).
2.  Connect `grid1` to the **value** port of `count1`. This will count the number of items in a list, in our case **10** (rows) x **20** (columns) = **200**.
3.  Create a [randomNumbers](ref:g.randomNumbers). The node is part of the math category and creates a list of random values between a minimum and maximum value.
4.  Connect `count1` to the **amount** port and set the **start** parameter to **5** and the **end** parameter to **25**. Render it and see a list of floating values between **5.0** and **25.0**.
5.  Connect `randomNumbers1` to the **width** and **height** port of `ellipse1` to see ellipses with various sizes.
6.  Change the **seed** parameter in `randonNumbers1` to create a new set of values for the ellipses and thus a new variation.

![Screenshot of the example](start-nodes2.png)

## Renaming nodes.

Each node in the network can be renamed, a feature which becomes handy when working in a larger network with lots of identical nodes.

Renaming can be done in two ways:

1.  Select a node and right click on it. Select the rename option.
    ![Screenshot of rightclick rename](start-nodes-rename.png)
2.  Select a node and click the <button class="btn"><i class="fa fa-pencil-square-o"></i><label>Rename</label></button> in the network editor.

A new tab will open. The node name will change immediately after pressing **ok**.

## More:

- [Adding functionality through nodes](guide:add-functionality)
- [Start with Code](guide:getting-started-code)
