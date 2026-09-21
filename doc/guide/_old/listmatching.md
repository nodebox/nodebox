# Lists and listmatching.

In essence, NodeBox Live is a **list processing machine**. Although initially invisible, the input and output of all nodes are lists, even the ones containing one element.

NodeBox Live does not have a “for loop” like in programming because looping is made implicit: generally, if a node receives a list of data, NodeBox Live will execute that node for each element in the list, and give back a list with the same size.

Calling for the amount of elements in a list can be done by using the `count` function.

## Lists with unequal sizes.

But what if the lists have different sizes? Imagine that you want to change the colors of a number of text paths. You have five text paths (A-E) and three colors: red, green and blue:

By default, NodeBox Live executes the `colorize` node (which is what we’re using to change the color of the text paths) for each of the elements in the list belonging to the first connected parameter, in this case the shape parameter.

If the other, shorter list runs out of data, it wraps around:

![Concepts List Matching](concepts-list-matching.png)

Within a node, the list to which all other lists should match is what we call the 'master list'. The master list can only refer to a parameter that is connected to another node, not to a single value parameter. You can check the master list of a node by selecting it: the parameter that has a blue or black list icon on the right has the master list.

If the icon is blue, it means that the master list has not been set by the user but chosen by NodeBox. The default is always the firstly connected parameter. If the icon is black, it means that the master list has been explicitly set by the user. A greyed out icon means the user can change this list to be master list. In our example, if we click the master list icon of the fill parameter, NodeBox Live will match the shapes to the amount of colors, so only leaving you with three items instead:

![Concepts List Matching](concepts-list-matching-2.png)

**Another example**: say we have created 10 ellipses on a line

The amount of numbers wraps around:

<small> -> 10 ellipses, 10 size values</small>![Concepts List Matching Ellipse 10](concepts-list-matching-ellipse-10.png)
<small> -> 10 ellipses, 9 size values</small>![Concepts List Matching Ellipse 9](concepts-list-matching-ellipse-9.png)
<small> -> 10 ellipses, 5 size values</small>![Concepts List Matching Ellipse 5](concepts-list-matching-ellipse-5.png)
<small> -> 10 ellipses, 2 size values</small>![Concepts List Matching Ellipse 2](concepts-list-matching-ellipse-2.png)
<small> -> 10 ellipses, 1 size values</small>![Concepts List Matching Ellipse 1](concepts-list-matching-ellipse-1.png)

Note that the last one is the same as specifying one value: the value list “wraps around” for every ellipse, effectively providing only one value.
