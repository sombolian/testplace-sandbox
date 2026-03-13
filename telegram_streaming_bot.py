#!/usr/bin/env python3
"""
Telegram Bot that tests the new sendMessageDraft streaming feature (Bot API 9.5).

When someone DMs the bot, it responds by streaming the reply word-by-word
using sendMessageDraft, then finalizes with sendMessage.
"""

import asyncio
import aiohttp
import time
import json

BOT_TOKEN = "8646687353:AAFHVJVtyj4zCWrPlYJoOj4EppmBaOJp8dg"
API_BASE = f"https://api.telegram.org/bot{BOT_TOKEN}"

# Sample responses the bot will stream
RESPONSES = [
    "Hey there! This message is being streamed to you word by word using Telegram's brand new sendMessageDraft API. Pretty cool, right? The future of bot messaging is here!",
    "Welcome! You're witnessing Telegram's new streaming response feature in action. Each word appears progressively, just like talking to an AI assistant. This is powered by the sendMessageDraft method from Bot API 9.5!",
    "Hello! I'm a test bot demonstrating Telegram's streaming capabilities. Instead of waiting for the entire message, you see it being typed out in real time. This uses the new sendMessageDraft endpoint that was made available to all bots in March 2026!",
    "Hi! This is a live demo of Telegram's streaming bot responses. The text flows in progressively using sendMessageDraft, which replaces the old hack of sending a message then rapidly editing it. Much smoother experience!",
]

response_index = 0


async def api_call(session: aiohttp.ClientSession, method: str, **params) -> dict:
    """Make a Telegram Bot API call."""
    url = f"{API_BASE}/{method}"
    async with session.post(url, json=params) as resp:
        return await resp.json()


async def stream_response(session: aiohttp.ClientSession, chat_id: int, text: str):
    """Stream a response word-by-word using sendMessageDraft, then finalize with sendMessage."""
    words = text.split()
    partial = ""

    for i, word in enumerate(words):
        if partial:
            partial += " " + word
        else:
            partial = word

        # Send draft (partial message) — skip the very last chunk, we'll sendMessage for that
        if i < len(words) - 1:
            result = await api_call(
                session,
                "sendMessageDraft",
                chat_id=chat_id,
                text=partial,
            )
            if not result.get("ok"):
                print(f"  sendMessageDraft error: {result}")
                # Fallback: just send the full message directly
                await api_call(session, "sendMessage", chat_id=chat_id, text=text)
                return

            # Small delay between chunks to make the streaming visible
            await asyncio.sleep(0.12)

    # Finalize: send the complete message
    result = await api_call(
        session,
        "sendMessage",
        chat_id=chat_id,
        text=partial,
    )
    if not result.get("ok"):
        print(f"  sendMessage error: {result}")


async def handle_message(session: aiohttp.ClientSession, message: dict):
    """Handle an incoming message."""
    global response_index

    chat_id = message["chat"]["id"]
    chat_type = message["chat"]["type"]
    text = message.get("text", "")
    user = message.get("from", {})
    first_name = user.get("first_name", "Unknown")

    print(f"[MSG] From {first_name} (chat {chat_id}, type {chat_type}): {text}")

    if text == "/start":
        welcome = (
            f"Hi {first_name}! I'm a streaming bot demo.\n\n"
            "Send me any message and I'll reply using Telegram's new "
            "sendMessageDraft streaming feature (Bot API 9.5).\n\n"
            "Each response streams word-by-word in real time!"
        )
        await stream_response(session, chat_id, welcome)
    else:
        # Pick a response and stream it
        resp_text = RESPONSES[response_index % len(RESPONSES)]
        response_index += 1
        await stream_response(session, chat_id, resp_text)


async def main():
    """Main polling loop."""
    print("Starting Telegram Streaming Bot...")
    print("=" * 50)

    async with aiohttp.ClientSession() as session:
        # Verify bot identity
        me = await api_call(session, "getMe")
        if not me.get("ok"):
            print(f"Failed to connect to Telegram: {me}")
            return

        bot_info = me["result"]
        print(f"Bot: @{bot_info.get('username', 'unknown')} ({bot_info['first_name']})")
        print(f"Bot ID: {bot_info['id']}")
        print("Listening for messages... (Ctrl+C to stop)")
        print("=" * 50)

        offset = 0
        while True:
            try:
                updates = await api_call(
                    session,
                    "getUpdates",
                    offset=offset,
                    timeout=30,
                )

                if updates.get("ok") and updates.get("result"):
                    for update in updates["result"]:
                        offset = update["update_id"] + 1
                        if "message" in update:
                            await handle_message(session, update["message"])

            except aiohttp.ClientError as e:
                print(f"Network error: {e}, retrying in 3s...")
                await asyncio.sleep(3)
            except Exception as e:
                print(f"Error: {e}")
                await asyncio.sleep(1)


if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        print("\nBot stopped.")
