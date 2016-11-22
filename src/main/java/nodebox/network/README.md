WebSocket message format
All WebSocket communications use JSON as an underlying format.  The base structure has a "id" and "msg" properties:
 {
    "type":"data" or "cmd" or "relay"
    "id": <random UUID as string> or PLAY etc,
    "msg": [
        {"key": <String>, "value":<Value Object>},
        ...
    ]
 }

