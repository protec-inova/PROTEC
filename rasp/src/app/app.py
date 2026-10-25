import asyncio
import sys
import time

from config.setup import runSetup
from config.config import loadJsonConfig, clearMaquina
from app.core.comm import ProtecAPI
from app.core.websocket import connect_once

POLICY_VIOLATION = 1008        # server refused: machine in use or unknown
CONFLICT_GRACE_SECONDS = 20    # keep retrying this long before calling it a real conflict
CONFLICT_RETRY_DELAY = 3
RETRY_DELAY_MIN = 1
RETRY_DELAY_MAX = 10
HEALTHY_SECONDS = 30           # a connection that lasted this long resets the retry delay


def isConfigValid():
    config = loadJsonConfig()

    if config is None:
        return False

    # Host is required and must be a usable address (checked locally, no network)
    if ProtecAPI(config.get_host()).host is None:
        return False

    # Machine is required
    if not config.get_maquina_id():
        return False

    return True


def run():
    notice = None

    # ---------------------- force setup
    if "--setup" in sys.argv:
        if not runSetup():
            print("Setup cancelled.")
            return

    while True:

        # ---------------------- setup until the config is valid
        while not isConfigValid():
            print("Invalid or missing configuration.")
            print("Starting setup...")

            # User closed the setup without saving
            if not runSetup(notice):
                print("Setup cancelled.")
                return

            notice = None

        config = loadJsonConfig()
        url = ProtecAPI(config.get_host()).ws_url(config.get_maquina_id())

        print("Configuration is valid.")
        print("Starting application...")

        delay = RETRY_DELAY_MIN
        conflict_since = None

        # ---------------------- connect, reconnect, detect refusal
        try:
            while True:
                started = time.monotonic()
                code = asyncio.run(connect_once(url))
                now = time.monotonic()

                if code == POLICY_VIOLATION:
                    if conflict_since is None:
                        conflict_since = now

                    if now - conflict_since >= CONFLICT_GRACE_SECONDS:
                        break  # real conflict, leave the loop and reopen the setup

                    print("Machine refused, retrying...")
                    time.sleep(CONFLICT_RETRY_DELAY)
                    continue

                conflict_since = None

                if now - started > HEALTHY_SECONDS:
                    delay = RETRY_DELAY_MIN

                print(f"Disconnected. Reconnecting in {delay}s...")
                time.sleep(delay)
                delay = min(delay * 2, RETRY_DELAY_MAX)

        except KeyboardInterrupt:
            print("Stopped.")
            return

        # Only reaches here when the server kept refusing the machine:
        # forget the machine (keeps the host) so isConfigValid() fails
        # and the loop above opens the setup again with a notice.
        clearMaquina()
        notice = "This machine is not available (in use by another device). Select another one."
