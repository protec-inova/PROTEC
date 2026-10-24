import sys

from config.setup import runSetup
from config.config import loadJsonConfig
from app.core.comm import ProtecAPI


def isConfigValid():
    config = loadJsonConfig()

    if config is None:
        return False

    host = config.get_host()
    maquina = config.get_maquina_id()

    # Host is required
    if not host:
        return False

    # Machine is required
    if not maquina:
        return False

    # Check API
    api = ProtecAPI(host)

    if not api.health():
        return False

    # Check if the configured machine is still available
    if not api.is_machine_available(maquina):
        return False

    return True


def main():

    # ____________________________ force setup
    if "--setup" in sys.argv:

        setup_result = runSetup()

        if not setup_result:
            print("Setup cancelled.")
            return

    while not isConfigValid():
        print("Invalid or missing configuration.")
        print("Starting setup...")

        setup_result = runSetup()

        # User closed the setup without saving
        if not setup_result:
            print("Setup cancelled.")
            return

    print("Configuration is valid.")
    print("Starting application...")

    # startApplication()


if __name__ == "__main__":
    main()