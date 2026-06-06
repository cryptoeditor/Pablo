"""
🕵️  CIPHER VAULT  🔐  — an AES-256 encryption game.

A small, spy-themed playground for experimenting with real authenticated
encryption. Lock a secret message in "the vault", share the resulting
agent code, and only someone with the right passphrase can crack it open.

Crypto under the hood (no hand-rolled ciphers here):
  * AES-256 in GCM mode  -> confidentiality + tamper detection
  * PBKDF2-HMAC-SHA256    -> turns a human passphrase into a 256-bit key
  * random salt + nonce   -> every lock is unique, even for the same message

Run it:  python test1.py
Requires: cryptography  (pip install cryptography)
"""

from __future__ import annotations

import base64
import getpass
import os
import secrets
import sys
from dataclasses import dataclass

from cryptography.exceptions import InvalidTag
from cryptography.hazmat.primitives.ciphers.aead import AESGCM
from cryptography.hazmat.primitives.kdf.pbkdf2 import PBKDF2HMAC
from cryptography.hazmat.primitives import hashes

# --- Tunable security parameters -------------------------------------------
KEY_LEN = 32          # 32 bytes = 256-bit key -> AES-256
SALT_LEN = 16
NONCE_LEN = 12        # 96-bit nonce is the recommended size for GCM
PBKDF2_ITERS = 600_000  # OWASP-recommended floor for PBKDF2-HMAC-SHA256


# ---------------------------------------------------------------------------
# Core crypto — pure functions, no I/O, easy to test in isolation.
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
    """Encrypt `message` and return a self-contained base64 'agent code'.

    The token packs salt + nonce + ciphertext(+tag) together, so the
    matching `unlock` only needs the token and the passphrase.
    """
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


# ---------------------------------------------------------------------------
# Game state + presentation
# ---------------------------------------------------------------------------
@dataclass
class Secret:
    label: str
    token: str


BANNER = r"""
   ____ _       _                 __     __         _ _
  / ___(_)_ __ | |__   ___ _ __   \ \   / /_ _ _   _| | |_
 | |   | | '_ \| '_ \ / _ \ '__|   \ \ / / _` | | | | | __|
 | |___| | |_) | | | |  __/ |       \ V / (_| | |_| | | |_
  \____|_| .__/|_| |_|\___|_|        \_/ \__,_|\__,_|_|\__|
         |_|         A E S - 2 5 6   c l a s s i f i e d
"""


def _input(prompt: str) -> str:
    try:
        return input(prompt)
    except (EOFError, KeyboardInterrupt):
        print("\n[ABORT] Exiting the vault. Stay sharp, Agent. 🫡")
        sys.exit(0)


def _passphrase(prompt: str) -> str:
    """Read a passphrase without echoing it (falls back if no TTY)."""
    try:
        value = getpass.getpass(prompt)
    except (EOFError, KeyboardInterrupt):
        print("\n[ABORT] Exiting the vault. 🫡")
        sys.exit(0)
    if not value:
        print("   ⚠️  Empty passphrase — that's no protection at all.")
    return value


def do_lock(vault: list[Secret]) -> None:
    print("\n--- 🔒  LOCK A SECRET ---")
    label = _input("   Codename for this secret: ").strip() or "untitled"
    message = _input("   Message to classify: ")
    pw = _passphrase("   Passphrase (hidden): ")
    confirm = _passphrase("   Confirm passphrase: ")
    if pw != confirm:
        print("   ❌ Passphrases don't match. Aborting lock.")
        return
    token = lock(message, pw)
    vault.append(Secret(label, token))
    print("\n   ✅ Secret sealed with AES-256-GCM. Your agent code:\n")
    print(f"   {token}\n")
    print("   Share the code freely — it's useless without the passphrase.")


def do_unlock(vault: list[Secret]) -> None:
    print("\n--- 🔓  CRACK OPEN A SECRET ---")
    if vault:
        print("   Stored this session:")
        for i, s in enumerate(vault, 1):
            print(f"     [{i}] {s.label}")
        print("     [P] Paste an external agent code")
        choice = _input("   Choose a number or P: ").strip().lower()
    else:
        choice = "p"

    if choice == "p":
        token = _input("   Paste agent code: ").strip()
    else:
        try:
            token = vault[int(choice) - 1].token
        except (ValueError, IndexError):
            print("   ❌ No such secret.")
            return

    pw = _passphrase("   Passphrase (hidden): ")
    try:
        message = unlock(token, pw)
    except InvalidTag:
        print("   🚨 ACCESS DENIED — wrong passphrase or the code was tampered with.")
        return
    except (ValueError, Exception) as exc:  # noqa: BLE001 - malformed token input
        print(f"   ❌ That doesn't look like a valid agent code. ({exc})")
        return
    print(f"\n   ✅ DECLASSIFIED:  {message}\n")


def do_crack_challenge() -> None:
    """A mini-game that shows *why* brute force is hopeless against AES-256."""
    print("\n--- 🎯  CRACK-THE-CODE CHALLENGE ---")
    print("   HQ locked a secret with a 4-digit PIN. You have 5 tries.")
    pin = f"{secrets.randbelow(10_000):04d}"
    token = lock("LAUNCH CODES: 7-ALPHA-NINER", pin)

    for attempt in range(1, 6):
        guess = _input(f"   Attempt {attempt}/5 — guess the 4-digit PIN: ").strip()
        try:
            message = unlock(token, guess)
        except (InvalidTag, ValueError, Exception):  # noqa: BLE001
            print("      🔴 Nope.")
            continue
        print(f"\n   🏆 CRACKED IT! The secret was: {message}")
        return

    print(f"\n   💥 Out of tries. The PIN was {pin}.")
    print("      Now imagine the key is 256 bits instead of 4 digits —")
    print("      that's ~10^77 combinations. Brute force isn't a strategy. 🛡️")


def do_about() -> None:
    print(
        "\n--- ℹ️  HOW THE VAULT WORKS ---\n"
        "   • Your passphrase is stretched into a 256-bit key with\n"
        f"     PBKDF2-HMAC-SHA256 ({PBKDF2_ITERS:,} iterations + random salt).\n"
        "   • The message is sealed with AES-256 in GCM mode, which also\n"
        "     stamps an authentication tag — change one byte of the code and\n"
        "     decryption refuses outright (that's the 🚨 ACCESS DENIED).\n"
        "   • A fresh random salt + nonce per lock means encrypting the same\n"
        "     message twice yields two totally different agent codes.\n"
    )


def main() -> None:
    print(BANNER)
    vault: list[Secret] = []
    menu = (
        "\n   [1] Lock a secret\n"
        "   [2] Crack open a secret\n"
        "   [3] Crack-the-code challenge\n"
        "   [4] How it works\n"
        "   [Q] Quit\n"
    )
    actions = {
        "1": lambda: do_lock(vault),
        "2": lambda: do_unlock(vault),
        "3": do_crack_challenge,
        "4": do_about,
    }
    while True:
        print(menu)
        choice = _input("   > ").strip().lower()
        if choice in ("q", "quit", "exit"):
            print("\n   🔒 Vault sealed. Goodbye, Agent.\n")
            return
        action = actions.get(choice)
        if action:
            action()
        else:
            print("   ❓ Unknown command.")


if __name__ == "__main__":
    main()
