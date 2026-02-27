"""
Simple Telegram bot using only urllib (no dependencies).
Tests 24/7 uptime in this environment.
"""

import json
import time
import urllib.request
import urllib.error
from datetime import datetime, timezone

TOKEN = "8599833483:AAEJ7mmH-rNQQUvXBrIbatVgmNbE2C7_4OU"
API = f"https://api.telegram.org/bot{TOKEN}"
START_TIME = datetime.now(timezone.utc)
POLL_COUNT = 0


def api_call(method, data=None):
    url = f"{API}/{method}"
    if data:
        payload = json.dumps(data).encode("utf-8")
        req = urllib.request.Request(url, data=payload, headers={"Content-Type": "application/json"})
    else:
        req = urllib.request.Request(url)
    with urllib.request.urlopen(req, timeout=35) as resp:
        return json.loads(resp.read().decode())


def get_uptime():
    delta = datetime.now(timezone.utc) - START_TIME
    minutes, seconds = divmod(int(delta.total_seconds()), 60)
    return f"{minutes}m {seconds}s"


def handle_message(msg):
    chat_id = msg["chat"]["id"]
    text = msg.get("text", "")

    if text == "/start":
        reply = (
            f"I'm alive! Uptime: {get_uptime()}\n"
            f"Started: {START_TIME.strftime('%H:%M:%S UTC')}\n\n"
            "Commands:\n/ping - check uptime\n"
            "Or just send any message!"
        )
    elif text == "/ping":
        reply = f"Pong! Uptime: {get_uptime()}"
    else:
        reply = f"Echo: {text}\n(Uptime: {get_uptime()})"

    api_call("sendMessage", {"chat_id": chat_id, "text": reply})


def main():
    global POLL_COUNT
    print(f"[{datetime.now(timezone.utc).strftime('%H:%M:%S')}] Bot starting...")

    # Delete webhook to enable polling
    api_call("deleteWebhook")

    me = api_call("getMe")
    print(f"[{datetime.now(timezone.utc).strftime('%H:%M:%S')}] Bot @{me['result']['username']} is online!")

    offset = None
    while True:
        POLL_COUNT += 1
        try:
            params = {"timeout": 30, "allowed_updates": ["message"]}
            if offset:
                params["offset"] = offset

            result = api_call("getUpdates", params)
            updates = result.get("result", [])

            for update in updates:
                offset = update["update_id"] + 1
                if "message" in update:
                    handle_message(update["message"])

            if POLL_COUNT % 10 == 0:
                print(f"[{datetime.now(timezone.utc).strftime('%H:%M:%S')}] "
                      f"Poll #{POLL_COUNT} | Uptime: {get_uptime()}")

        except Exception as e:
            print(f"[{datetime.now(timezone.utc).strftime('%H:%M:%S')}] Error: {e}")
            time.sleep(3)


if __name__ == "__main__":
    main()
