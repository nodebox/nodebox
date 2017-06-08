from websocket_server import WebsocketServer
import json



# Called for every client connecting (after handshake)
def new_client(client, server):
	print("New client connected and was given id %d" % client['id'])


# Called for every client disconnecting
def client_left(client, server):
	print("Client(%d) disconnected" % client['id'])


# Called when a client sends a message
def message_received(client, server, message):
	if message == "ping":
		server.send_message(client, "pong")
		return()

	jsonData = json.loads(message)
	#idStr = jsonData["id"]
	print("id: " + jsonData["id"])
	print("msg: " + json.dumps(jsonData["msg"]))

	# Test function, assumes msg has as 'string' field
	if jsonData["type"] == "dta":
		if hasattr(jsonData["msg"], 'string'):
			jsonData["msg"]["string"] += "_SERVER"

		if hasattr(jsonData["msg"], 'geometry'):
			print("Num Geos: " + len(jsonData["msg"]["geometry"]))
		server.send_message(client, json.dumps(jsonData))

	elif jsonData["type"] == "rsp":
		print(jsonData["msg"])

	elif jsonData["type"] == "rly":
		if jsonData["id"] == "play":
			msgPlay()
			print("relay: Play")
		elif jsonData["id"] == "stop":
			msgStop()
			print("relay: Stop")
		elif jsonData["id"] == "rewind":
			msgRewind()
			print("relay: Rewind")
		elif jsonData["id"] == "reload":
			msgReload()
			print("relay: Reload")
		elif jsonData["id"] == "setframe":
			msgSetframe(jsonData["msg"])
			print("relay: setframe")
		elif jsonData["id"] == "load":
			msgLoad(jsonData["msg"])
			print("relay: Load")
		elif jsonData["id"] == "exportrange":
			msgExportRange(jsonData["msg"])
			print("relay: exportrange")
		elif jsonData["id"] == "getframe":
			msgGetframe(jsonData["msg"])
			print("relay: msgGetframe")


def msgPlay():
	msgData = {
	'type': "cmd",
    'id': 'play',
    'msg': {}
	}
	server.send_message_to_all(json.dumps(msgData))

def msgStop():
	msgData = {
		'type': "cmd",
		'id': 'stop',
		'msg': {}
	}
	server.send_message_to_all(json.dumps(msgData))

def msgRewind():
	msgData = {
		'type': "cmd",
		'id': 'rewind',
		'msg': {}
	}
	server.send_message_to_all(json.dumps(msgData))

def msgReload():
	msgData = {
		'type': "cmd",
		'id': 'reload',
		'msg': {}
	}
	server.send_message_to_all(json.dumps(msgData))

def msgSetframe(msg):
	msgData = {
		'type': "cmd",
		'id': 'setframe',
		'msg': msg
	}
	server.send_message_to_all(json.dumps(msgData))

def msgGetframe(msg):
	msgData = {
		'type': "req",
		'id': 'getframe',
		'msg': msg
	}
	server.send_message_to_all(json.dumps(msgData))

def msgLoad(msg):
	msgData = {
		'type': "cmd",
		'id': 'load',
		'msg': msg
	}
	server.send_message_to_all(json.dumps(msgData))

def msgExportRange(msg):
	msgData = {
		'type': "cmd",
		'id': 'exportrange',
		'msg': msg
	}
	server.send_message_to_all(json.dumps(msgData))



PORT=9001
server = WebsocketServer(PORT)
server.set_fn_new_client(new_client)
server.set_fn_client_left(client_left)
server.set_fn_message_received(message_received)
server.run_forever()


