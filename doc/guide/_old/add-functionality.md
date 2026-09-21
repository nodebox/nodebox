# Add functionality

Suppose we want to create a [Truchet](http://en.wikipedia.org/wiki/Truchet_tiles) pattern. Each tile in this specific pattern has 2 possible orientations (the original tile and a 90° rotation or orthogonal flip).

![Screenshot of a truchet pattern](add-function-truchet.png)

Create a new project and call it **truchet**.

## Creating a single tile

The idea is to create a funtion that will make a single tile, then combine a number of those tiles in a grid pattern. Press the <button class="btn"><i class="fa fa-plus"></i><label>Network</label></button> button. For the name, type **tile**.

Once created, press the <button class="btn"><i class="fa fa-plus"></i><label>New Node</label></button> button in the
[network editor](guide:network-editor) or double click somewhere in the editor to add functionality.

1.  Create an `arc` function. Set the **position** to **-50,-50** and change the **arcType** to **Open**.
2.  Create a `colorize` function and set **fill** to **transparent**, **stroke** to **red** and **strokeWidth** to **10**. Connect `arc1` function to the **shape** port.
3.  Create a `reflect` (**Be careful**: there are _two_ reflect nodes. Use the one that says "Mirror the geometry around an invisible axis".) function and set the **angle** parameter to **135**. Connect `colorize1` to the **shape** port
4.  Create a `rotate` function and connect `reflect1` to the `shape` port. (Nothing will happen since the default rotation is 0.)
5.  Create a `translate` function and connect `rotate1` to the `shape` port. (Again, nothing will happen since the default translation is 0,0.)

In order to change this new network from the main network it needs metadata. The metadata pane allows you to define the incoming parameters for custom functions. Press the <button class="btn"><label>Metadata</label></button> button on top of the [network editor](guide:network-editor). New Parameters can be created by pressing the <button class="btn"><i class="fa fa-plus"></i><label>Add parameter</label></button>button.

6.  Create a new parameter called **angle**. Set type to **float** and the value to **0**.
7.  Create a new parameter called **position**. Set type to **point** and value to **0, 0**.

Click the _Network_ tab. The two new parameters should appear on the upper left corner of the network editor and can now be connected to a node using its port.

8. Connect **angle** to the `angle` port of the `rotate` function.
9. Connect **position** to the `translate` port of the `translate` function.

![Screenshot of the truchet tile network](add-function-truchettile.png)

## The pattern.

Go back to the `main` network and drag the tile function on the network editor.

Now that we have a base tile to work on. We can create a number of instances of them and give all of them a different location and angle.

1.  Create a `grid` function. Set **rows** and **columns** to **5**, and **width** , **height** to **400**. Connect it to the `position` port of `tile1`.
2.  Create a `count` function and connect `grid1` to it in order to find the amount of points.
3.  Create a `randomNumbers` function and connect `count1` to the **amount** port. Set **min** and **max** to **0** and **2**.
4.  Create an `integer` function and connect `randomNumbers1` to it to round the numbers.
5.  Create a `multiply` function and connect `integer1` to the **a** parameter. Set **b** to **90**.
6.  Connect `multiply1` to `angle` port of `tile1`.
7.  Render `tile1` and change the **seed** parameter in `randomNumbers1` to change the pattern.

![Screenshot of the truchet network](add-function-truchetnet.png)

## More:

- [Start with Code](guide:getting-started-code)
- [Concepts]
