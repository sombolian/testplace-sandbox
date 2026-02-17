"""
Roblox Player Online Check
Based on: https://devforum.roblox.com/t/checking-last-time-a-player-was-online/2218556

This script demonstrates two Roblox presence API approaches:
1. /v1/presence/users - Returns current online status (no auth required)
2. /v1/presence/last-online - Returns last online timestamp (requires .ROBLOSECURITY cookie)
"""

import requests
import sys
from datetime import datetime


ROBLOX_USERS_API = "https://users.roblox.com/v1/usernames/users"
ROBLOX_PRESENCE_API = "https://presence.roblox.com/v1/presence/users"
ROBLOX_LAST_ONLINE_API = "https://presence.roblox.com/v1/presence/last-online"

PRESENCE_TYPES = {
    0: "Offline",
    1: "Online (Website)",
    2: "In-Game",
    3: "In Studio",
}


def get_user_id(username):
    """Look up a Roblox user ID by username."""
    resp = requests.post(
        ROBLOX_USERS_API,
        json={"usernames": [username], "excludeBannedUsers": False},
    )
    resp.raise_for_status()
    data = resp.json().get("data", [])
    if not data:
        return None
    return data[0]


def get_presence(user_id):
    """Get current presence status (no auth required)."""
    resp = requests.post(
        ROBLOX_PRESENCE_API,
        json={"userIds": [user_id]},
    )
    resp.raise_for_status()
    presences = resp.json().get("userPresences", [])
    if not presences:
        return None
    return presences[0]


def get_last_online(user_id, cookie=None):
    """
    Get last online timestamp (requires .ROBLOSECURITY cookie).
    Returns ISO 8601 timestamp or error info.
    """
    headers = {}
    if cookie:
        headers["Cookie"] = f".ROBLOSECURITY={cookie}"
    resp = requests.post(
        ROBLOX_LAST_ONLINE_API,
        json={"userIds": [user_id]},
        headers=headers,
    )
    if resp.status_code == 401 or resp.status_code == 404:
        return {"errors": [{"code": resp.status_code, "message": "Authentication required"}]}
    resp.raise_for_status()
    return resp.json()


def main():
    username = sys.argv[1] if len(sys.argv) > 1 else "Y04H"

    print(f"Looking up Roblox user: {username}")
    user = get_user_id(username)
    if not user:
        print(f"User '{username}' not found.")
        return

    user_id = user["id"]
    display_name = user["displayName"]
    print(f"  Username:     {user['name']}")
    print(f"  Display Name: {display_name}")
    print(f"  User ID:      {user_id}")
    print()

    # Method 1: Current presence (public, no auth)
    print("--- Method 1: Current Presence (no auth required) ---")
    presence = get_presence(user_id)
    if presence:
        ptype = presence.get("userPresenceType", -1)
        status = PRESENCE_TYPES.get(ptype, f"Unknown ({ptype})")
        last_location = presence.get("lastLocation", "N/A")
        print(f"  Status:        {status}")
        print(f"  Last Location: {last_location}")
        if presence.get("placeId"):
            print(f"  Place ID:      {presence['placeId']}")
        if presence.get("gameId"):
            print(f"  Game ID:       {presence['gameId']}")
    else:
        print("  Could not retrieve presence info.")
    print()

    # Method 2: Last online timestamp (requires auth)
    print("--- Method 2: Last Online Timestamp (auth required) ---")
    last_online_data = get_last_online(user_id)
    errors = last_online_data.get("errors", [])
    timestamps = last_online_data.get("lastOnlineTimestamps", [])

    if timestamps:
        ts = timestamps[0].get("lastOnline", "")
        if ts:
            dt = datetime.fromisoformat(ts.replace("Z", "+00:00"))
            print(f"  Last Online:   {dt.strftime('%Y-%m-%d %H:%M:%S UTC')}")
            print(f"  ISO Timestamp: {ts}")
        else:
            print("  No timestamp returned.")
    elif errors:
        print("  Auth required - pass a .ROBLOSECURITY cookie to use this endpoint.")
        print(f"  API response: {last_online_data}")
    else:
        print(f"  Unexpected response: {last_online_data}")


if __name__ == "__main__":
    main()
