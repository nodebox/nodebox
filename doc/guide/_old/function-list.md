# The Function List

Functions are the main primitives in NodeBox Live. The power of the system comes from combining small functions into bigger
functions. Each function is described as a network of nodes, or a piece of JavaScript code.

The function list shows an overview of all functions in your project, and allows you to create new ones.

![Screenshot of the Function List](function-list-screenshot.png)

No matter how you create your functions, they can be used interchangeably.

Big projects are often composed of multiple functions. For example, if you're creating an army of creatures, a single
creature will be stored as a separate function. If you want to work on one function, but keep looking at the overall
result, you can _pin_ a function. The pinned function will be visible in the viewer, but not in the editor.

By default, the `main` function is selected. This is just a convention: the main function works just like any other
function.

## Creating a Network Function

If you want to work with nodes, create a network function. Press the <button class="btn"><i class="fa fa-plus"></i><label>Network</label></button>
button. The application will ask you for a new name. (e.g. "myfunction").

Once created, press the <button class="btn"><i class="fa fa-plus"></i><label>New Node</label></button> button in the
[network editor](guide:network-editor) to add functionality.

## Creating a Code Function

If you want to work with JavaScript code, create a code function. Press the <button class="btn"><i class="fa fa-plus"></i><label>Code</label></button>
button. The application will ask you for a new name. Make sure you include your project ID prefix (e.g. "myproject.myfunction").

The application will create an almost-empty function that returns a single value. Note that this function already
returns a value. That's because a parameter with a default value was created in the [metadata editor](guide:metadata-editor).

## Organizing your Functions

Big projects are often composed of multiple functions. For example, if you're creating an army of creatures, a single
creature will be stored as a separate function.

## Pinning a Function

When working on a bigger project, you often want to work on a small function while keeping a larger overview. To do this,
you can _pin_ a function. The pinned function will remain visible in the viewer, while you work on some other function
in the editor.

![Function Pinning](function-list-pinning.png)

Pin a function by clicking the <i class="fa fa-thumb-tack"></i> pin icon next to the function name. The viewer will now
keep showing the pinned function, even when you select a different function in the list. To un-pin, just click the pin again.

A common use case is to pin the _main_ function and select a different function, used in your main function, to work on.
