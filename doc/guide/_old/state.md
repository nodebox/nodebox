# State

Physics simulations, particles, real-time animation: all of these are examples of projects that use some kind of global _state_. They _retain_ data between frames, passed on through _global variables_. In NodeBox Live, this happens through `stateLoad` and `stateSave`.

### Loading / saving state

[stateLoad](ref:g.stateLoad) — Load the global state

[stateSave](ref:g.stateSave) — Save the global state
