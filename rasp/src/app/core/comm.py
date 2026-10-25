from urllib.parse import urlsplit, urlunsplit

import requests

defaultPort = 8081


def normalizeHost(host):
    """'192.168.0.100' -> 'http://192.168.0.100:8081'"""
    host = (host or "").strip().rstrip("/")
    if not host:
        return None
    if "://" not in host:
        host = "http://" + host
    try:
        parts = urlsplit(host)
        port = parts.port
    except ValueError:
        return None
    if not parts.hostname:
        return None
    netloc = parts.netloc if port else f"{parts.netloc}:{defaultPort}"
    return urlunsplit((parts.scheme, netloc, "", "", ""))


class ProtecAPI:
    def __init__(self, host):
        self.set_host(host)

    def set_host(self, host):
        self.host = normalizeHost(host)

    def health(self):
        if not self.host:
            return False
        try:
            requests.get(f"{self.host}/health", timeout=5).raise_for_status()
            return True
        except requests.RequestException:
            return False

    def get_available_maquinas(self):
        if not self.host:
            return []
        try:
            response = requests.get(f"{self.host}/maquinas/available", timeout=5)
            response.raise_for_status()
            return response.json()
        except (requests.RequestException, ValueError):
            return []

    def ws_url(self, maquina_id):
        if not self.host:
            return None
        # http -> ws, https -> wss
        return self.host.replace("http", "ws", 1) + f"/ws/pi?maquinaId={maquina_id}"