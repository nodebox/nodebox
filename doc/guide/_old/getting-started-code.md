# Start with code

If you prefer writing functions in code, use the code editor to create JavaScript functions.

The code editor is currently very minimal. Here are some of its features:

- Editing is live. The function is immediately executed using the argument values coming from the [metadata](guide:metadata) panel.
- Errors are displayed at the bottom of the editor. Click the <i class="fa fa-caret-square-o-right"></i> expand
  arrow to show details about the error.
- Syntax can be cleaned up by pressing the <button class="btn"><i class="fa fa-indent"></i><label>Beautify</label></button> button.

## A user function returning a single value.

Let's start by creating an add function (which is already in NodeBox Live). Press the <button class="btn"><i class="fa fa-plus"></i><label>Code</label></button> button. The application will ask you for a new name. (e.g. "myAdd").

The code editor should read:

    tester.myAdd = function (v) {
        return v;
    };

Change the code so we can send the function two arguments and return the sum of the two:

    tester.myAdd = function (a, b) {
        return a+b;
    };

Then press the <button class="btn"><label>Metadata</label></button> button on top of the [network editor](guide:network-editor) and click on the <button class="btn"><i class="fa fa-refresh"></i><label>Sync</label></button> button. The metadata should now reveil the two parameters while the new node has two input ports.

Drag the new function into the main network and test it.

![Screenshot of the myAdd example](start-code-myAdd.png)

## A user function returning a list.

This function will create a list of numbers based on a starting values where each value is 2/3 of the previous one and with a minimum set to a specific number (f.i '5').

Press the <button class="btn"><i class="fa fa-plus"></i><label>Code</label></button> button. Name the new function "myNumbers") and enter the following code:

    tester.myNumbers = function (start, min) {
        var list = [start];
        do {
            start = start / 3 * 2;
            list.push(start);
        }
        while (start > min);
        return list;
    };

Press the <button class="btn"><label>Metadata</label></button> button on top of the [network editor](guide:network-editor) and click on the <button class="btn"><i class="fa fa-refresh"></i><label>Sync</label></button> button.

Since we want a list of values we also need to toggle on the **returns list** option.

![Screenshot of the Metadata panel](start-code-metadata.png)

Drag the new function into the main network and try it out.

1.  Set the **start** parameter of `myNumbers1` to **60** and the **min** parameter to **5**.
2.  Create a `rect` function and connect `myNumbers1` to the **height** parameter.
3.  Create a `stack` function, connect `rect1` to the **shapes** parameter and set the **direction** parameter to **North**.

![Screenshot of the stacked myNumbers example](start-code-myNumbers.png)

## More:

- [Working with g.js](getting-started-g)
