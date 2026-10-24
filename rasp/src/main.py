import sys

from config.setup import runSetup
from config.config import loadJsonConfig


def isConfigValid():
    config = loadJsonConfig()

    if config is None:
        return False

    host = config.get_host()
    maquina = config.get_maquina_id()

    if not host:
        return False

    # Machine can be None
    if not maquina:
        return False

    return True


def main():
    # Force setup
    if "--setup" in sys.argv:
        runSetup()

    # Keep opening setup until the configuration is valid
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