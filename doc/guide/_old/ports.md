# Ports

Functions represented as nodes can send data to each other using their ports. Ports represent the **inputs and output of a node**.

A node takes data in from its inputs, processes it, and sets a value in its output port, ready to be picked up by the next node.

## Port Types

Port types are indicated by a **color** and each port expects a certain **type** of value:

- <span class="node-type" style="background: #5DA5DA">Numbers</span> both integers and floating point numbers. </br>**Floats** are numbers with a fractional part. They are used to specify dimensions: _the width / height of an object, the distance between two points, etc_.
  </br>**Integers** are numbers without a fractional part. They are _used to specify amounts: 10 copies, 50 points, etc._
- <span class="node-type" style="background: #60BD68">Points</span> are **two-dimensional coordinates on the canvas** that contains an X and Y value.
  </br>They are used to specify a place in 2D space: _the position of a rectangle, the scale of an object, the origin of a tranformation._
- <span class="node-type" style="background: #E8AA00">Shapes</span> contain information about **curves, lines and points**. They are the primary visual building blocks of NodeBox Live.
- <span class="node-type" style="background: #F17CB0">Strings</span> contain **pieces of text**.
  </br>They are used wherever we need textual input: _the filename of a SVG file, the lookup value for a CSV file or the text in a textpath node._
- <span class="node-type" style="background: #B2912F">Booleans</span> are values that can be either **true or false**. </br>They are used to specify logical conditions: _whether something is enabled or visible, whether elements need to be filtered out or not, etc._
- <span class="node-type" style="background: #B276B2">Lists</span> **contain a generic list of items of other types**. A list can therefore contain colors, shapes, numbers, etc.
- <span class="node-type" style="background: #DECF3F">Colors</span> contain **color values** set in red/green/blue/alpha (rgba) or hue/saturation/brightness/alhpa (hsba) values.
  </br>They are used to specify color information: _the fill color of an object, the stroke color of a line, etc._

## Parameter window

The input and output ports of a node are displayed in the parameter window. You can access it by clicking a node.

![Screenshot of the Parameter Window](ports-screenshot.png)

Parameter values can be changed by entering a new value or by using the drag option within the inputfield.

- **Single number** values can be changed by a horizontal movement.
- **Double number** values, a point for instance, can be changed over a horizontal and vertical movement, the horizontal axis will change the first value, the vertical one the second.
- **Boolean** values can be toggled on/off.
- Some ports have **a menu** in the parameter section. Use the mouse functions to change the value.
- Ports that are used to read in a file will go through the assets manager window.
