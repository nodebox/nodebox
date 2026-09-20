# The Network Editor

The Network Editor allows you to create and change nodes. A node is represented as a rectangular block with inputs and
outputs:

![Screenshot of the Network Editor](network-editor-screenshot.png)

## Navigating the network

- You can **zoom** in and out by scrolling the mouse wheel.
- You can **pan** the view by dragging it with the left mouse button.

The same controls work in the [viewer](guide:viewer).

## The parameter panel

Clicking on a node will bring up the parameter panel:

![Screenshot of the parameter panel](network-editor-parameter-panel.png)

Here you can change the values of each parameter. Each node will have different parameters, depending on its function.

There are multiple ways to edit parameters:

- **Click** to change the value as text, press ENTER to confirm.
- **Click and drag** to interactively change the values. Positions (points) can be changed by dragging left-right and up-down.
- Some parameters have a menu associated with them with a list of options.

Currently, there is no color picker. You can input colors in multiple ways:

- Use **named colors** such as `red`, `green` or `yellow`. See the list of [CSS color names](http://www.w3schools.com/cssref/css_colornames.asp)
- Use **coded rgb values**. They look like this: `rgb(230, 12, 0)`. For transparency, use rgba: `rgba(230, 12, 0, 0.5)`.
  R/G/B values range between 0-255. Alpha values range between 0-1.
- Use **coded hsb values**. They look like this: `hsb(360, 12, 0)`. For transparency, use hsba: `hsba(230, 12, 0, 0.5)`.

## Creating a new node

New nodes can be created using the <button class="btn"><i class="fa fa-plus"></i><label>New Node</label></button> button.
Once selected, the function will appear in the network as a node. We say that a node is an _instance_ of a function:
a copy of the function with (potentially) different parameter values.

All nodes have zero or more inputs and a single outputs.

To view the result of the node, double-click it. The node will have a triangle in the bottom-left corner.

Only one node can be rendered at the same time. If you want to combine the output of multiple nodes, use `g.merge`
(for shapes) or `list.combine`.

Users can **render a node by double clicking it**. We call this the **rendered node**. The rendered node can be recognised by it's black color. It also turns all incoming connection into black and shows a extra outline in the node itself.

**Clicking a node once** will make it the **selected node**. This will show an extra panel called the parameter window.

## Types of data

The color of the connection show the **type** of data that flows through them:

- <span class="node-type" style="background: #5DA5DA">Numbers</span>: both integers (`5`) and floating-point values (`3.141`).
- <span class="node-type" style="background: #60BD68">Points</span>: a 2-dimensional position on the canvas.
- <span class="node-type" style="background: #E8AA00">Shapes</span>: circles, lines, rectangles...
- <span class="node-type" style="background: #F17CB0">Strings</span>: textual information.
- <span class="node-type" style="background: #B2912F">Booleans</span>: a value that can contain `true` or `false`.
- <span class="node-type" style="background: #B276B2">Lists</span>: a generic list of items of other types.
- <span class="node-type" style="background: #DECF3F">Colors</span>: a value containing a red, green and blue component.
