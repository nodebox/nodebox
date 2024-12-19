# NodeBox Runtime

## Introduction

The `@ndbx/runtime` package provides a runtime environment to embed NodeBox visualizations directly into React applications. NodeBox is a powerful tool for creating interactive and generative visualizations, and this runtime allows you to integrate those creations into your React-based web apps, controlling them dynamically from within your app’s logic.

## Features

- Embed NodeBox Projects in React: Easily add NodeBox visualizations to your React components with the NodeBoxPlayer component.
- Dynamic Parameter Control: Control and animate parameters of NodeBox projects in real time.
- Seamless Integration: Use NodeBox within any existing or new React app.

## Installation

To use the NodeBox runtime in your project, first install it using npm:

```bash
npm install @ndbx/runtime
```

## Usage

### Basic Embedding

To embed a NodeBox visualization into your React app, import the NodeBoxPlayer component and use it within your JSX. You’ll need a userId and projectId to specify which NodeBox project to load.

```js
import { NodeBoxPlayer } from "@ndbx/runtime";

function App() {
  return <NodeBoxPlayer userId="yourUserId" projectId="yourProjectId" />;
}

export default App;
```

### Controlling Parameters

Often, you’ll want to control the parameters of your NodeBox visualization dynamically. To do this, you can pass a handler function to the onProjectLoaded prop, which will be called once the project has loaded. You can also pass in a values object to define parameters dynamically.

```js
import { useState } from 'react';
import { NodeBoxPlayer, Context, LiteralValue } from '@ndbx/runtime';

function App() {
  const [values, setValues] = useState<Record<string, LiteralValue>>({});

  function handleProjectLoaded(cx: Context) {
    const main = cx.lookupItemByName('self/self/Main');
    console.log(main.parameters); // Logs the available parameters
    setValues({ fullName: "Frederik" }); // Example of setting a parameter
  }

  return (
    <NodeBoxPlayer
      userId="yourUserId"
      projectId="yourProjectId"
      onProjectLoaded={handleProjectLoaded}
      values={values}
    />
  );
}

export default App;
```

In this example, the `handleProjectLoaded` function is used to retrieve information about the loaded project, and we can set parameters that the visualization will use. The setValues function updates the state of the parameters passed to the visualization.

### Available Props

- **userId** (`string`): Your NodeBox user ID.
- **projectId** (`string`): The ID of the project you wish to embed.
- **item** (`string`): The name of the item to load in the project. Defaults to `Main`.
- **onProjectLoaded** (`function`): A callback function that is triggered when the project has been successfully loaded. The Context object is passed to this function, which gives access to the project’s parameters and items.
- **values** (`Record<string, LiteralValue>`): An object containing parameter values to dynamically update the project.

### Example

Here’s a more complete example showing how you can use the NodeBoxPlayer in a typical React app:

```js
import React, { useState } from 'react';
import { NodeBoxPlayer, Context, LiteralValue } from '@ndbx/runtime';

function App() {
  const [name, setName] = useState("John Doe");
  const [values, setValues] = useState<Record<string, LiteralValue>>({ fullName: name });

  function handleProjectLoaded(cx: Context) {
    const main = cx.lookupItemByName('self/self/Main');
    console.log(main.parameters); // Logs parameters of the visualization
  }

  function updateName(newName: string) {
    setName(newName);
    setValues({ fullName: newName });
  }

  return (
    <div>
      <h1>Interactive NodeBox Visualization</h1>
      <input
        type="text"
        value={name}
        onChange={e => updateName(e.target.value)}
        placeholder="Enter a name"
      />
      <NodeBoxPlayer
        userId="yourUserId"
        projectId="yourProjectId"
        onProjectLoaded={handleProjectLoaded}
        values={values}
      />
    </div>
  );
}

export default App;
```

This example provides an interactive input that dynamically updates the fullName parameter in the NodeBox visualization.

## Conclusion

The `@ndbx/runtime` package allows you to control yout NodeBox visualizations from your React applications. For more information, consult the official [NodeBox documentation](https://new.nodebox.live/guide/welcome) or [open an issue on GitHub](https://github.com/nodebox/live) if you have any questions or encounter any issues.
