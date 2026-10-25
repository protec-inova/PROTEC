import customtkinter as ctk
from tkinter import messagebox

from app.core.comm import ProtecAPI
from config.config import loadJsonConfig, writeJsonConfig


class ConfigApp:
    def __init__(self, root, notice=None):
        self.root = root
        self.notice = notice
        self.saved = False

        # Machines returned by the API: name -> {"id", "nome", "riscos"}
        self.maquinas = {}

        # Window
        self.root.title("Protec Setup")
        self.root.geometry("500x600")
        self.root.resizable(False, False)

        # Theme
        ctk.set_appearance_mode("light")
        ctk.set_default_color_theme("blue")

        self.create_header()
        self.create_form()

        self.loadData()

    def create_header(self):
        header = ctk.CTkFrame(
            self.root,
            height=65,
            corner_radius=0,
            fg_color="#FFFFFF"
        )

        header.pack(fill="x")
        header.pack_propagate(False)

        # Title
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

        # API status
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

        # -------------------------
        # Notice (e.g. machine already in use)
        # -------------------------

        if self.notice:
            ctk.CTkLabel(
                container,
                text=self.notice,
                font=("Arial", 11, "bold"),
                text_color="#B45309",
                wraplength=420,
                justify="left"
            ).pack(
                anchor="w",
                pady=(0, 15)
            )

        # -------------------------
        # Host Address
        # -------------------------

        ctk.CTkLabel(
            container,
            text="Host Address",
            font=("Arial", 13, "bold")
        ).pack(
            anchor="w"
        )

        ctk.CTkLabel(
            container,
            text="IP of the computer running the Protec API (port 8081 if omitted)",
            font=("Arial", 10),
            text_color="#777777"
        ).pack(
            anchor="w",
            pady=(2, 6)
        )

        self.entry_host = ctk.CTkEntry(
            container,
            height=38,
            placeholder_text="192.168.0.100"
        )

        self.entry_host.pack(
            fill="x",
            pady=(0, 10)
        )

        # Enter key also connects
        self.entry_host.bind("<Return>", lambda event: self.connect())

        self.btn_connect = ctk.CTkButton(
            container,
            text="Connect",
            height=34,
            fg_color="#6B7280",
            hover_color="#4B5563",
            command=self.connect
        )

        self.btn_connect.pack(
            fill="x",
            pady=(0, 25)
        )

        # -------------------------
        # Machine
        # -------------------------

        ctk.CTkLabel(
            container,
            text="Machine",
            font=("Arial", 13, "bold")
        ).pack(
            anchor="w"
        )

        ctk.CTkLabel(
            container,
            text="Machine this device controls (only free machines are listed)",
            font=("Arial", 10),
            text_color="#777777"
        ).pack(
            anchor="w",
            pady=(2, 8)
        )

        # Starts empty.
        # Options are filled by the API when you press Connect.
        self.machine_combobox = ctk.CTkComboBox(
            container,
            height=38,
            values=[],
            state="readonly",
            command=self.on_machine_selected
        )

        # Without this, an empty combobox shows (and returns) "CTkComboBox"
        self.machine_combobox.set("")

        self.machine_combobox.pack(
            fill="x",
            pady=(0, 8)
        )

        self.riscos_label = ctk.CTkLabel(
            container,
            text="",
            font=("Arial", 10),
            text_color="#777777",
            wraplength=420,
            justify="left"
        )

        self.riscos_label.pack(
            anchor="w",
            pady=(0, 22)
        )

        # -------------------------
        # Save Button
        # -------------------------

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

    def loadData(self):
        config_atual = loadJsonConfig()

        if config_atual:
            # Load only the Host.
            # Machine will be provided by the API.
            host = config_atual.get_host()

            if host:
                self.entry_host.insert(
                    0,
                    host
                )

                # Already knows the host: fill the machine list right away
                self.root.after(200, self.connect)

        else:
            messagebox.showinfo(
                "Warning",
                "config.json not found. A new one will be created"
            )

    def connect(self):
        host = self.entry_host.get().strip()

        if not host:
            messagebox.showwarning(
                "Warning",
                "You need to fill in the Host Address"
            )
            return

        # Note: the window may freeze for up to ~5s if the API is unreachable
        api = ProtecAPI(host)
        online = api.health()

        self.api_status.configure(
            text="● API Connected" if online else "● API Disconnected",
            text_color="#16A34A" if online else "#DC2626"
        )

        maquinas = api.get_available_maquinas() if online else []

        self.maquinas = {m["nome"]: m for m in maquinas}

        self.machine_combobox.configure(values=list(self.maquinas))
        self.machine_combobox.set("")
        self.riscos_label.configure(text="")

        if not online:
            messagebox.showwarning(
                "Warning",
                "Could not reach the API. Check the address."
            )
        elif not maquinas:
            messagebox.showinfo(
                "Info",
                "No machine available. All of them are in use."
            )

    def on_machine_selected(self, nome):
        maquina = self.maquinas.get(nome)

        if maquina:
            self.riscos_label.configure(text=f"Risks: {maquina['riscos']}")

    def saveData(self):
        new_host = self.entry_host.get().strip()
        maquina = self.maquinas.get(self.machine_combobox.get().strip())

        # -------------------------
        # Validation
        # -------------------------

        if not new_host:
            messagebox.showwarning(
                "Warning",
                "You need to fill in the Host Address"
            )
            return

        if not maquina:
            messagebox.showwarning(
                "Warning",
                "Press Connect and select a Machine"
            )
            return

        # -------------------------
        # Save
        # -------------------------

        try:
            # Saves the UUID (the API identifies machines by id, not by name)
            writeJsonConfig(
                new_host,
                maquina["id"]
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


def runSetup(notice=None):
    root = ctk.CTk()

    app = ConfigApp(root, notice)

    root.mainloop()

    return app.saved