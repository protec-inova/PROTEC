import asyncio
import json

from websockets.asyncio.client import connect
from websockets.exceptions import ConnectionClosed, WebSocketException

heartbeatInterval = 5

async def connect_once(url):
    async with connect(url, open_timeout=5) as ws:
        print("connected")

        # Runs a task that says to server that this rasp stay alive
        task = asyncio.create_task(heartbeat(ws))

        try:
            async for msg in ws:
               print(f"recived: {msg}")
               read_msg(msg)
            
        except ConnectionClosed as closed:
            if closed.rcvd:
                return closed.rcvd.code
            return None
        
        finally:
            task.cancel()

        return ws.close_code
    

# This function send a periodic sinal to server
# A simple way to understand is this's a mensage from rasp saying
# "HEY API! I'm still here!"   
async def heartbeat(ws):
    try:
        while True:
            await ws.send(json.dumps({"type": "HEARTBEAT"}))
            await asyncio.sleep(heartbeatInterval)

    except ConnectionClosed:
        pass
    

# In this function we recive a signal from server
# and decide what rasp will with that
def read_msg(raw):
    try:
        # try to convert json
        data = json.loads(raw)
    
    #if not possible traslate json return
    except ValueError:
        print(f"Ignored (not JSON): {raw}")
        return

    # Here we know that json is valid
    # but we make a verify if it is expected
    if not isinstance(data, dict):
        return

    if data.get("type") != "SET":
        return

    enabled = data.get("enabled")

    if not isinstance(enabled, bool):
        return
    
