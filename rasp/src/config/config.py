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
        with open("config.json", "r", encoding="utf-8") as file:
            data = json.load(file)
        
        host = data["host"]
        maquina = data["maquina"]

        config = Config(host, maquina)
        return config

    except FileNotFoundError:
        print("Error config.json not found.")
        return None 


def writeJsonConfig(host, maquinaId):
    config = {
        "host": host,
        "maquina": maquinaId
    }

    with open("config.json", "w", encoding="utf-8") as file:
        json.dump(config, file, ensure_ascii=False, indent=4)
