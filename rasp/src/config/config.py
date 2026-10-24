import json


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
        with open(
            "config.json",
            "r",
            encoding="utf-8"
        ) as file:

            data = json.load(file)

        host = data.get("host")
        maquina = data.get("maquina")

        return Config(
            host,
            maquina
        )

    except (FileNotFoundError, json.JSONDecodeError):
        print("Error loading config.json.")
        return None


def writeJsonConfig(host, maquinaId):
    config = {
        "host": host,
        "maquina": maquinaId
    }

    with open(
        "config.json",
        "w",
        encoding="utf-8"
    ) as file:

        json.dump(
            config,
            file,
            ensure_ascii=False,
            indent=4
        )