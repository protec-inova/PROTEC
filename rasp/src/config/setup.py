import customtkinter as ctk
from tkinter import messagebox

from config.config import loadJsonConfig, writeJsonConfig


class ConfigApp:
    def __init__(self, root):
        self.root = root

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
            placeholder_text="192.168.0.100"
        )

        self.entry_host.pack(
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
            text="Select the machine to be configured",
            font=("Arial", 10),
            text_color="#777777"
        ).pack(
            anchor="w",
            pady=(2, 8)
        )

        # Machine options
        # Later these will come from the API
        self.machine_combobox = ctk.CTkComboBox(
            container,
            height=38,
            values=["None"],
            state="readonly"
        )

        self.machine_combobox.set("None")

        self.machine_combobox.pack(
            fill="x",
            pady=(0, 30)
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
            # Load Host
            self.entry_host.insert(
                0,
                config_atual.get_host()
            )

            # Load Machine
            machine_value = config_atual.get_maquina_id()

            if machine_value is None or machine_value == "":
                self.machine_combobox.set("None")
            else:
                # Keep the saved value even if it isn't
                # available from the API yet.
                self.machine_combobox.configure(
                    values=["None", machine_value]
                )

                self.machine_combobox.set(
                    machine_value
                )

        else:
            messagebox.showinfo(
                "Warning",
                "config.json não encontrado. A new one will be created"
            )

    def saveData(self):
        new_host = self.entry_host.get().strip()
        new_machine = self.machine_combobox.get()

        # Host is required
        if not new_host:
            messagebox.showwarning(
                "Warning",
                "You need to fill in the Host Address"
            )
            return

        # Convert "None" from the UI to Python None
        if new_machine == "None":
            new_machine = None

        try:
            # Rewrite configuration
            writeJsonConfig(
                new_host,
                new_machine
            )

            messagebox.showinfo(
                "Success",
                "Config was saved successfully!"
            )

            # Close window after saving
            self.root.destroy()

        except Exception as e:
            messagebox.showerror(
                "Error",
                f"Not possible to save: {e}"
            )


def runSetup():
    root = ctk.CTk()

    ConfigApp(root)

    root.mainloop()