import requests


class ProtecAPI:

    def __init__(self, host):
        self.host = host.rstrip("/")

    def set_host(self, host):
        self.host = host.rstrip("/")

    def health(self):
        try:
            response = requests.get(
                f"{self.host}/health",
                timeout=5
            )

            response.raise_for_status()

            data = response.json()

            return data.get("status") == "UP"

        except requests.RequestException:
            return False

    def get_machines(self):
        try:
            response = requests.get(
                f"{self.host}/maquinas/available",
                timeout=5
            )

            response.raise_for_status()

            return response.json()

        except requests.RequestException:
            return []