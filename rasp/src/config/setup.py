import customtkinter as ctk
from tkinter import messagebox

from config.config import loadJsonConfig, writeJsonConfig
from app.core.comm import ProtecAPI


class ConfigApp:

    def __init__(self, root):
        self.root = root
        self.saved = False
        self.api = None
        self.machines = []
        self.api_connected = False
        self.last_host = ""

        # Window
        self.root.title("Protec Setup")
        self.root.geometry("500x400")
        self.root.resizable(False, False)

        # Theme
        ctk.set_appearance_mode("light")
        ctk.set_default_color_theme("blue")

        self.create_header()
        self.create_form()

        self.loadData()
        self.start_api_monitor()

    # ==========================================================
    # HEADER
    # ==========================================================

    def create_header(self):
        header = ctk.CTkFrame(
            self.root,
            height=65,
            corner_radius=0,
            fg_color="#FFFFFF"
        )

        header.pack(fill="x")
        header.pack_propagate(False)

        title = ctk.CTkLabel(
            header,
            text="PROTEC SETUP",
            font=("Arial", 20, "bold"),
            text_color="#222222"
        )

        title.pack(
            side="left",
            padx=25
        )

        self.api_status = ctk.CTkLabel(
            header,
            text="● API Disconnected",
            font=("Arial", 11),
            text_color="#DC2626"
        )

        self.api_status.pack(
            side="right",
            padx=20
        )

    # ==========================================================
    # FORM
    # ==========================================================

    def create_form(self):
        container = ctk.CTkFrame(
            self.root,
            fg_color="transparent"
        )

        container.pack(
            fill="both",
            expand=True,
            padx=40,
            pady=25
        )

        # ------------------------------------------------------
        # Host Address
        # ------------------------------------------------------

        ctk.CTkLabel(
            container,
            text="Host Address",
            font=("Arial", 13, "bold")
        ).pack(
            anchor="w"
        )

        ctk.CTkLabel(
            container,
            text="Address of the Protec API",
            font=("Arial", 10),
            text_color="#777777"
        ).pack(
            anchor="w",
            pady=(2, 6)
        )

        self.entry_host = ctk.CTkEntry(
            container,
            height=38,
            placeholder_text="http://192.168.0.100:8080"
        )

        self.entry_host.pack(
            fill="x",
            pady=(0, 25)
        )

        # Connect when leaving the Host field
        self.entry_host.bind(
            "<FocusOut>",
            self.on_host_changed
        )

        # ------------------------------------------------------
        # Machine
        # ------------------------------------------------------

        ctk.CTkLabel(
            container,
            text="Machine",
            font=("Arial", 13, "bold")
        ).pack(
            anchor="w"
        )

        ctk.CTkLabel(
            container,
            text="Select the machine to be configured",
            font=("Arial", 10),
            text_color="#777777"
        ).pack(
            anchor="w",
            pady=(2, 8)
        )

        # Starts empty.
        # The API will provide the available machines.
        self.machine_combobox = ctk.CTkComboBox(
            container,
            height=38,
            values=[],
            state="readonly"
        )

        self.machine_combobox.pack(
            fill="x",
            pady=(0, 30)
        )

        # ------------------------------------------------------
        # Save Button
        # ------------------------------------------------------

        self.btn_salvar = ctk.CTkButton(
            container,
            text="Save Configuration",
            height=40,
            font=("Arial", 13, "bold"),
            command=self.saveData
        )

        self.btn_salvar.pack(
            fill="x"
        )

    # ==========================================================
    # LOAD CONFIGURATION
    # ==========================================================

    def loadData(self):
        config_atual = loadJsonConfig()

        if not config_atual:
            messagebox.showinfo(
                "Warning",
                "config.json não encontrado. A new one will be created"
            )
            return

        host = config_atual.get_host()

        if host:
            self.entry_host.insert(
                0,
                host
            )

            # Try connecting automatically
            self.connect_api()

    # ==========================================================
    # HOST CHANGED
    # ==========================================================

    def on_host_changed(self, event=None):
        self.connect_api()

    # ==========================================================
    # API CONNECTION
    # ==========================================================

    def connect_api(self):
        host = self.entry_host.get().strip()

        if not host:
            self.api = None
            self.api_connected = False
            self.set_api_status(False)
            self.clear_machines()
            return

        self.set_api_connecting()

        api = ProtecAPI(host)

        if not api.health():
            self.api = None
            self.api_connected = False
            self.set_api_status(False)
            self.clear_machines()
            return

        self.api = api
        self.api_connected = True

        self.set_api_status(True)

        self.load_machines()

    # ==========================================================
    # LOAD MACHINES
    # ==========================================================

    def load_machines(self):
        if self.api is None:
            return

        machines = self.api.get_machines()

        self.machines = machines

        machine_names = [
            machine["nome"]
            for machine in machines
            if machine.get("nome")
        ]

        self.machine_combobox.configure(
            values=machine_names
        )

        self.machine_combobox.set("")

        # Try to restore previously configured machine
        config_atual = loadJsonConfig()

        if not config_atual:
            return

        saved_machine_id = config_atual.get_maquina_id()

        if not saved_machine_id:
            return

        for machine in machines:
            if str(machine.get("id")) == str(saved_machine_id):
                self.machine_combobox.set(
                    machine["nome"]
                )
                break

    # ==========================================================
    # API STATUS
    # ==========================================================

    def set_api_connecting(self):
        self.api_status.configure(
            text="● Connecting...",
            text_color="#CA8A04"
        )

    def set_api_status(self, connected):
        if connected:
            self.api_status.configure(
                text="● API Connected",
                text_color="#16A34A"
            )
        else:
            self.api_status.configure(
                text="● API Disconnected",
                text_color="#DC2626"
            )

    # ==========================================================
    # GET SELECTED MACHINE
    # ==========================================================

    def get_selected_machine_id(self):
        selected_name = self.machine_combobox.get().strip()

        if not selected_name:
            return None

        for machine in self.machines:
            if machine.get("nome") == selected_name:
                return machine.get("id")

        return None

    # ==========================================================
    # SAVE
    # ==========================================================

    def saveData(self):
        new_host = self.entry_host.get().strip()

        selected_machine_id = self.get_selected_machine_id()

        # ------------------------------------------------------
        # Host validation
        # ------------------------------------------------------

        if not new_host:
            messagebox.showwarning(
                "Warning",
                "You need to fill in the Host Address"
            )
            return

        # ------------------------------------------------------
        # API validation
        # ------------------------------------------------------

        if self.api is None:
            messagebox.showwarning(
                "Warning",
                "The API is not connected"
            )
            return

        # ------------------------------------------------------
        # Machine validation
        # ------------------------------------------------------

        if not selected_machine_id:
            messagebox.showwarning(
                "Warning",
                "You need to select a Machine"
            )
            return

        # ------------------------------------------------------
        # Save configuration
        # ------------------------------------------------------

        try:
            writeJsonConfig(
                new_host,
                selected_machine_id
            )

            self.saved = True

            messagebox.showinfo(
                "Success",
                "Config was saved successfully!"
            )

            self.root.destroy()

        except Exception as e:
            messagebox.showerror(
                "Error",
                f"Not possible to save: {e}"
            )
    
    def start_api_monitor(self):
        self.update_api_connection()

        self.root.after(
            2000,
            self.start_api_monitor
        )

    def update_api_connection(self):
        host = self.entry_host.get().strip()

        if not host:
            if self.api_connected:
                self.api_connected = False

            self.api = None
            self.set_api_status(False)
            self.clear_machines()
            return

        # Se o endereço mudou, precisamos criar uma nova conexão
        if host != self.last_host:
            self.last_host = host
            self.api = ProtecAPI(host)
            self.api_connected = False

        if self.api is None:
            self.api = ProtecAPI(host)

        connected = self.api.health()

        # API acabou de conectar
        if connected and not self.api_connected:
            self.api_connected = True
            self.set_api_status(True)

            self.load_machines()

        # API continua conectada
        elif connected and self.api_connected:
            self.set_api_status(True)

            # Atualiza a lista de máquinas
            self.load_machines()

        # API caiu
        elif not connected and self.api_connected:
            self.api_connected = False
            self.api = None

            self.set_api_status(False)
            self.clear_machines()

        # Continua desconectada
        elif not connected:
            self.api_connected = False
            self.set_api_status(False)

        
    def clear_machines(self):
        self.machines = []

        self.machine_combobox.configure(
            values=[]
        )

        self.machine_combobox.set("")


def runSetup():
    root = ctk.CTk()

    app = ConfigApp(root)

    root.mainloop()

    return app.saved