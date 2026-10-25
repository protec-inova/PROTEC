import json
from pathlib import Path

# Always src/config.json, no matter where the program is started from
CONFIG_PATH = Path(__file__).resolve().parent.parent / "config.json"


class Config:
    def __init__(self, host, maquinaId):
        self.host = host
        self.maquinaId = maquinaId

    def get_host(self):
        return self.host

    def get_maquina_id(self):
        return self.maquinaId


def loadJsonConfig():
    try:
        with open(CONFIG_PATH, "r", encoding="utf-8") as file:
            data = json.load(file)

        host = data["host"]
        maquina = data["maquina"]

        return Config(host, maquina)

    except (FileNotFoundError, json.JSONDecodeError, KeyError) as e:
        print(f"Invalid or missing config.json: {e}")
        return None


def writeJsonConfig(host, maquinaId):
    config = {
        "host": host,
        "maquina": maquinaId
    }

    with open(CONFIG_PATH, "w", encoding="utf-8") as file:
        json.dump(config, file, ensure_ascii=False, indent=4)


def clearMaquina():
    """Keeps the host but forgets the machine, so the setup opens again."""
    config = loadJsonConfig()
    writeJsonConfig(config.get_host() if config else "", "")