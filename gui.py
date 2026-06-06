"""
🕵️  CIPHER VAULT  🔐  — AES-256 encryption game, GUI edition.

Same authenticated-encryption core as test1.py, wrapped in a Tkinter
window instead of a terminal menu. Lock a secret into an "agent code",
share the code, and only the right passphrase cracks it open.

Crypto under the hood (no hand-rolled ciphers here):
  * AES-256 in GCM mode  -> confidentiality + tamper detection
  * PBKDF2-HMAC-SHA256    -> turns a human passphrase into a 256-bit key
  * random salt + nonce   -> every lock is unique, even for the same message

Run it:  python gui.py
Requires: cryptography  (pip install cryptography)
          tkinter ships with the standard CPython installer.
"""

!@from __future__ import annotations

import base64
import secrets
from dataclasses import dataclass

import tkinter as tk
from tkinter import messagebox, ttk

from cryptography.exceptions import InvalidTag
from cryptography.hazmat.primitives.ciphers.aead import AESGCM
from cryptography.hazmat.primitives.kdf.pbkdf2 import PBKDF2HMAC
from cryptography.hazmat.primitives import hashes

# --- Tunable security parameters -------------------------------------------
KEY_LEN = 32            # 32 bytes = 256-bit key -> AES-256
SALT_LEN = 16
NONCE_LEN = 12          # 96-bit nonce is the recommended size for GCM
PBKDF2_ITERS = 600_000  # OWASP-recommended floor for PBKDF2-HMAC-SHA256


# ---------------------------------------------------------------------------
# Core crypto — pure functions, no I/O, identical to the CLI build.
# ---------------------------------------------------------------------------
def _derive_key(passphrase: str, salt: bytes) -> bytes:
    """Stretch a passphrase into a 256-bit AES key with PBKDF2."""
    kdf = PBKDF2HMAC(
        algorithm=hashes.SHA256(),
        length=KEY_LEN,
        salt=salt,
        iterations=PBKDF2_ITERS,
    )
    return kdf.derive(passphrase.encode("utf-8"))


def lock(message: str, passphrase: str) -> str:
    """Encrypt `message` and return a self-contained base64 'agent code'."""
    salt = secrets.token_bytes(SALT_LEN)
    nonce = secrets.token_bytes(NONCE_LEN)
    key = _derive_key(passphrase, salt)
    ciphertext = AESGCM(key).encrypt(nonce, message.encode("utf-8"), None)
    blob = salt + nonce + ciphertext
    return base64.urlsafe_b64encode(blob).decode("ascii")


def unlock(token: str, passphrase: str) -> str:
    """Decrypt an agent code. Raises InvalidTag on wrong key/tampering."""
    blob = base64.urlsafe_b64decode(token.encode("ascii"))
    salt, nonce, ciphertext = (
        blob[:SALT_LEN],
        blob[SALT_LEN : SALT_LEN + NONCE_LEN],
        blob[SALT_LEN + NONCE_LEN :],
    )
    key = _derive_key(passphrase, salt)
    plaintext = AESGCM(key).decrypt(nonce, ciphertext, None)
    return plaintext.decode("utf-8")


@dataclass
class Secret:
    label: str
    token: str


# ---------------------------------------------------------------------------
# GUI
# ---------------------------------------------------------------------------
class CipherVaultGUI(tk.Tk):
    def __init__(self) -> None:
        super().__init__()
        self.title("🕵️  Cipher Vault — AES-256")
        self.geometry("640x560")
        self.minsize(560, 480)

        self.vault: list[Secret] = []

        notebook = ttk.Notebook(self)
        notebook.pack(fill="both", expand=True, padx=10, pady=10)

        self._build_lock_tab(notebook)
        self._build_unlock_tab(notebook)
        self._build_challenge_tab(notebook)
        self._build_about_tab(notebook)

    # -- Lock tab -----------------------------------------------------------
    def _build_lock_tab(self, nb: ttk.Notebook) -> None:
        tab = ttk.Frame(nb, padding=12)
        nb.add(tab, text="🔒 Lock")

        ttk.Label(tab, text="Codename:").grid(row=0, column=0, sticky="w", pady=4)
        self.lock_label = ttk.Entry(tab, width=40)
        self.lock_label.grid(row=0, column=1, sticky="we", pady=4)

        ttk.Label(tab, text="Message to classify:").grid(row=1, column=0, sticky="nw", pady=4)
        self.lock_message = tk.Text(tab, height=6, width=40, wrap="word")
        self.lock_message.grid(row=1, column=1, sticky="we", pady=4)

        ttk.Label(tab, text="Passphrase:").grid(row=2, column=0, sticky="w", pady=4)
        self.lock_pw = ttk.Entry(tab, width=40, show="•")
        self.lock_pw.grid(row=2, column=1, sticky="we", pady=4)

        ttk.Label(tab, text="Confirm:").grid(row=3, column=0, sticky="w", pady=4)
        self.lock_pw2 = ttk.Entry(tab, width=40, show="•")
        self.lock_pw2.grid(row=3, column=1, sticky="we", pady=4)

        self.show_pw = tk.BooleanVar(value=False)
        ttk.Checkbutton(
            tab, text="Show passphrase", variable=self.show_pw,
            command=self._toggle_lock_pw,
        ).grid(row=4, column=1, sticky="w", pady=2)

        ttk.Button(tab, text="🔒  Seal with AES-256", command=self._do_lock).grid(
            row=5, column=1, sticky="w", pady=8
        )

        ttk.Label(tab, text="Agent code:").grid(row=6, column=0, sticky="nw", pady=4)
        self.lock_output = tk.Text(tab, height=5, width=40, wrap="char", state="disabled")
        self.lock_output.grid(row=6, column=1, sticky="we", pady=4)

        ttk.Button(tab, text="📋 Copy code", command=self._copy_code).grid(
            row=7, column=1, sticky="w", pady=2
        )

        tab.columnconfigure(1, weight=1)

    def _toggle_lock_pw(self) -> None:
        char = "" if self.show_pw.get() else "•"
        self.lock_pw.config(show=char)
        self.lock_pw2.config(show=char)

    def _do_lock(self) -> None:
        message = self.lock_message.get("1.0", "end-1c")
        pw = self.lock_pw.get()
        pw2 = self.lock_pw2.get()
        if not message:
            messagebox.showwarning("Nothing to lock", "Enter a message to classify.")
            return
        if not pw:
            messagebox.showwarning("Weak", "Empty passphrase is no protection at all.")
            return
        if pw != pw2:
            messagebox.showerror("Mismatch", "Passphrases don't match.")
            return

        label = self.lock_label.get().strip() or "untitled"
        token = lock(message, pw)
        self.vault.append(Secret(label, token))
        self._refresh_vault_list()

        self.lock_output.config(state="normal")
        self.lock_output.delete("1.0", "end")
        self.lock_output.insert("1.0", token)
        self.lock_output.config(state="disabled")

        # Clear sensitive fields; the code is useless without the passphrase.
        self.lock_pw.delete(0, "end")
        self.lock_pw2.delete(0, "end")
        messagebox.showinfo("Sealed", f"'{label}' sealed with AES-256-GCM.")

    def _copy_code(self) -> None:
        code = self.lock_output.get("1.0", "end-1c").strip()
        if code:
            self.clipboard_clear()
            self.clipboard_append(code)
            messagebox.showinfo("Copied", "Agent code copied to clipboard.")

    # -- Unlock tab ---------------------------------------------------------
    def _build_unlock_tab(self, nb: ttk.Notebook) -> None:
        tab = ttk.Frame(nb, padding=12)
        nb.add(tab, text="🔓 Unlock")

        ttk.Label(tab, text="This session's secrets:").grid(row=0, column=0, sticky="w")
        self.vault_list = tk.Listbox(tab, height=4)
        self.vault_list.grid(row=1, column=0, columnspan=2, sticky="we", pady=4)
        self.vault_list.bind("<<ListboxSelect>>", self._load_from_vault)

        ttk.Label(tab, text="Agent code:").grid(row=2, column=0, sticky="nw", pady=4)
        self.unlock_token = tk.Text(tab, height=5, width=40, wrap="char")
        self.unlock_token.grid(row=2, column=1, sticky="we", pady=4)

        ttk.Label(tab, text="Passphrase:").grid(row=3, column=0, sticky="w", pady=4)
        self.unlock_pw = ttk.Entry(tab, width=40, show="•")
        self.unlock_pw.grid(row=3, column=1, sticky="we", pady=4)

        ttk.Button(tab, text="🔓  Decrypt", command=self._do_unlock).grid(
            row=4, column=1, sticky="w", pady=8
        )

        ttk.Label(tab, text="Result:").grid(row=5, column=0, sticky="nw", pady=4)
        self.unlock_output = tk.Text(tab, height=6, width=40, wrap="word", state="disabled")
        self.unlock_output.grid(row=5, column=1, sticky="we", pady=4)

        tab.columnconfigure(1, weight=1)

    def _refresh_vault_list(self) -> None:
        self.vault_list.delete(0, "end")
        for s in self.vault:
            self.vault_list.insert("end", s.label)

    def _load_from_vault(self, _event: object) -> None:
        sel = self.vault_list.curselection()
        if not sel:
            return
        token = self.vault[sel[0]].token
        self.unlock_token.delete("1.0", "end")
        self.unlock_token.insert("1.0", token)

    def _do_unlock(self) -> None:
        token = self.unlock_token.get("1.0", "end-1c").strip()
        pw = self.unlock_pw.get()
        if not token:
            messagebox.showwarning("No code", "Paste or pick an agent code first.")
            return

        self.unlock_output.config(state="normal")
        self.unlock_output.delete("1.0", "end")
        try:
            message = unlock(token, pw)
        except InvalidTag:
            self.unlock_output.insert(
                "1.0", "🚨 ACCESS DENIED — wrong passphrase or the code was tampered with."
            )
        except Exception as exc:  # noqa: BLE001 - malformed token from a text field
            self.unlock_output.insert("1.0", f"❌ Not a valid agent code. ({exc})")
        else:
            self.unlock_output.insert("1.0", f"✅ DECLASSIFIED:\n\n{message}")
        self.unlock_output.config(state="disabled")
        self.unlock_pw.delete(0, "end")

    # -- Challenge tab ------------------------------------------------------
    def _build_challenge_tab(self, nb: ttk.Notebook) -> None:
        tab = ttk.Frame(nb, padding=12)
        nb.add(tab, text="🎯 Challenge")

        self._challenge_pin: str | None = None
        self._challenge_token: str | None = None
        self._challenge_left = 0

        ttk.Label(
            tab,
            text="HQ locked a secret with a 4-digit PIN. You get 5 tries.",
            wraplength=560,
        ).pack(anchor="w", pady=6)

        ttk.Button(tab, text="🎯  New challenge", command=self._new_challenge).pack(
            anchor="w", pady=4
        )

        row = ttk.Frame(tab)
        row.pack(anchor="w", pady=4)
        ttk.Label(row, text="Guess:").pack(side="left")
        self.challenge_entry = ttk.Entry(row, width=8)
        self.challenge_entry.pack(side="left", padx=6)
        self.challenge_entry.bind("<Return>", lambda _e: self._guess_pin())
        ttk.Button(row, text="Try", command=self._guess_pin).pack(side="left")

        self.challenge_status = tk.Text(tab, height=10, width=60, wrap="word", state="disabled")
        self.challenge_status.pack(fill="both", expand=True, pady=8)

    def _challenge_log(self, line: str) -> None:
        self.challenge_status.config(state="normal")
        self.challenge_status.insert("end", line + "\n")
        self.challenge_status.see("end")
        self.challenge_status.config(state="disabled")

    def _new_challenge(self) -> None:
        self._challenge_pin = f"{secrets.randbelow(10_000):04d}"
        self._challenge_token = lock("LAUNCH CODES: 7-ALPHA-NINER", self._challenge_pin)
        self._challenge_left = 5
        self.challenge_status.config(state="normal")
        self.challenge_status.delete("1.0", "end")
        self.challenge_status.config(state="disabled")
        self._challenge_log("🔐 A new secret is locked. 5 tries left.")

    def _guess_pin(self) -> None:
        if not self._challenge_token:
            self._challenge_log("Press 'New challenge' to begin.")
            return
        if self._challenge_left <= 0:
            return
        guess = self.challenge_entry.get().strip()
        self.challenge_entry.delete(0, "end")
        try:
            message = unlock(self._challenge_token, guess)
        except Exception:  # noqa: BLE001 - any failure is just a wrong guess
            self._challenge_left -= 1
            if self._challenge_left > 0:
                self._challenge_log(f"🔴 Nope. {self._challenge_left} tries left.")
            else:
                self._challenge_log(f"💥 Out of tries. The PIN was {self._challenge_pin}.")
                self._challenge_log(
                    "Now imagine a 256-bit key instead of 4 digits — ~10^77 "
                    "combinations. Brute force isn't a strategy. 🛡️"
                )
                self._challenge_token = None
            return
        self._challenge_log(f"🏆 CRACKED IT! The secret was: {message}")
        self._challenge_token = None

    # -- About tab ----------------------------------------------------------
    def _build_about_tab(self, nb: ttk.Notebook) -> None:
        tab = ttk.Frame(nb, padding=12)
        nb.add(tab, text="ℹ️ How it works")
        about = (
            "How the vault works\n"
            "───────────────────\n\n"
            f"• Your passphrase is stretched into a 256-bit key with\n"
            f"  PBKDF2-HMAC-SHA256 ({PBKDF2_ITERS:,} iterations + random salt).\n\n"
            "• The message is sealed with AES-256 in GCM mode, which also\n"
            "  stamps an authentication tag — change one byte of the code\n"
            "  and decryption refuses outright (the 🚨 ACCESS DENIED).\n\n"
            "• A fresh random salt + nonce per lock means encrypting the\n"
            "  same message twice yields two totally different agent codes.\n\n"
            "• The agent code is self-contained: it packs salt + nonce +\n"
            "  ciphertext together, so unlocking needs only the code and\n"
            "  the passphrase.\n"
        )
        widget = tk.Text(tab, wrap="word", height=18)
        widget.insert("1.0", about)
        widget.config(state="disabled")
        widget.pack(fill="both", expand=True)


def main() -> None:
    CipherVaultGUI().mainloop()


if __name__ == "__main__":
    main()
