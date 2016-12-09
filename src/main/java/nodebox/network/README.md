# WebSocket Integration


## System Components
There are three main types of communication between Nodebox and a server:
- Data
- Commands
- Requests

There is a fourth type: Relay, but this is only used on the server side and should never net sent to Nodebox so can be safely ignored unless you're created a complex system.


## WebSocket message format
All WebSocket communications use JSON as an underlying format.  The base structure has a "id" and "msg" properties:

```
 {
    "type":"data" or "cmd" or "relay"
    "id": <random UUID as string> or PLAY etc,
    "msg": [
        {"key": <String>, "value":<Value Object>},
        ...
    ]
 }
```


