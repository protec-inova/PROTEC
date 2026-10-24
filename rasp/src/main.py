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

    #if not maquina:
    #    return False

    return True


def main():
    # Force setup
    if "--setup" in sys.argv:
        runSetup()
        return

    # Validate configuration
    if not isConfigValid():
        print("Invalid or missing configuration.")
        print("Starting setup...")

        runSetup()
        return

    print("Starting application...")

    # startApplication()


if __name__ == "__main__":
    main()