"""
🎮 AI Dungeon RPG — Telegram Bot
Powered by Claude AI as the Dungeon Master.
"""
import os
import json
import random
import logging
import threading
from pathlib import Path

import telebot
from telebot.types import InlineKeyboardMarkup, InlineKeyboardButton

from game_state import GameState, CharacterClass, MONSTERS, ITEMS, LOCATIONS
import ai_master

logging.basicConfig(format="%(asctime)s - %(levelname)s - %(message)s", level=logging.INFO)
log = logging.getLogger(__name__)

TOKEN = "8599833483:AAEJ7mmH-rNQQUvXBrIbatVgmNbE2C7_4OU"
OWNER_CHAT_ID = 1625078551
SAVE_DIR = Path("saves")
SAVE_DIR.mkdir(exist_ok=True)

bot = telebot.TeleBot(TOKEN, parse_mode="Markdown")

# ─── Save / Load ──────────────────────────────────────────────────────────────

def save_game(user_id: int, state: GameState):
    with open(SAVE_DIR / f"{user_id}.json", "w") as f:
        json.dump(state.to_dict(), f)

def load_game(user_id: int) -> GameState | None:
    path = SAVE_DIR / f"{user_id}.json"
    if path.exists():
        with open(path) as f:
            return GameState.from_dict(json.load(f))
    return None

def get_state(user_id: int) -> GameState:
    return load_game(user_id) or GameState()

# ─── Keyboards ────────────────────────────────────────────────────────────────

def class_keyboard():
    kb = InlineKeyboardMarkup(row_width=1)
    kb.add(
        InlineKeyboardButton("⚔️ Warrior — Tank & bruiser", callback_data="class_WARRIOR"),
        InlineKeyboardButton("🧙 Mage — High magic damage",  callback_data="class_MAGE"),
        InlineKeyboardButton("🗡️ Rogue — Fast & deadly",    callback_data="class_ROGUE"),
    )
    return kb

def action_keyboard(choices: list[str]):
    kb = InlineKeyboardMarkup(row_width=1)
    for i, c in enumerate(choices):
        kb.add(InlineKeyboardButton(f"{i+1}. {c}", callback_data=f"choice_{i}"))
    kb.row(
        InlineKeyboardButton("📊 Status",    callback_data="quick_status"),
        InlineKeyboardButton("🎒 Inventory", callback_data="quick_inventory"),
    )
    return kb

def combat_keyboard(has_potion: bool):
    kb = InlineKeyboardMarkup(row_width=1)
    kb.add(
        InlineKeyboardButton("⚔️ Attack",             callback_data="combat_attack"),
        InlineKeyboardButton("🛡️ Defend (+DEF)",      callback_data="combat_defend"),
        InlineKeyboardButton("💨 Dodge (50% chance)", callback_data="combat_dodge"),
    )
    if has_potion:
        kb.add(InlineKeyboardButton("🧪 Use Health Potion", callback_data="combat_potion"))
    kb.add(InlineKeyboardButton("🏃 Flee (50% chance)", callback_data="combat_flee"))
    return kb

def explore_keyboard():
    kb = InlineKeyboardMarkup(row_width=3)
    kb.row(
        InlineKeyboardButton("🗺️ Explore", callback_data="action_explore"),
        InlineKeyboardButton("🏪 Shop",    callback_data="action_shop"),
        InlineKeyboardButton("💤 Rest",    callback_data="action_rest"),
    )
    return kb

def shop_keyboard(state: GameState):
    shop_items = {
        "Health Potion":   15,
        "Elixir":          35,
        "Iron Sword":      40,
        "Steel Shield":    35,
        "Enchanted Staff": 55,
        "Magic Amulet":    30,
    }
    kb = InlineKeyboardMarkup(row_width=1)
    for item, price in shop_items.items():
        info = ITEMS[item]
        mark = "✅" if state.gold >= price else "❌"
        kb.add(InlineKeyboardButton(
            f"{mark} {info['emoji']} {item} — {price}🪙",
            callback_data=f"buy_{item}"
        ))
    kb.add(InlineKeyboardButton("🚪 Leave Shop", callback_data="shop_leave"))
    return kb

# ─── Reply helpers ────────────────────────────────────────────────────────────

def reply(chat_id, text, keyboard=None):
    try:
        bot.send_message(chat_id, text, reply_markup=keyboard, parse_mode="Markdown")
    except Exception as e:
        log.error(f"send_message error: {e}")
        try:
            bot.send_message(chat_id, text.replace("*", "").replace("_", ""), reply_markup=keyboard)
        except Exception as e2:
            log.error(f"plain send_message error: {e2}")

def edit_or_reply(call, text, keyboard=None):
    try:
        bot.answer_callback_query(call.id)
    except Exception:
        pass
    reply(call.message.chat.id, text, keyboard)

# ─── /start ───────────────────────────────────────────────────────────────────

@bot.message_handler(commands=["start"])
def cmd_start(message):
    uid = message.from_user.id
    state = get_state(uid)

    if state.phase == "playing":
        reply(uid,
            f"⚔️ Welcome back, *{state.name}*!\n\n"
            f"Level {state.level} {state.char_class.value} — ❤️ {state._hp}/{state.max_hp}\n\n"
            "What will you do?",
            explore_keyboard()
        )
        return

    state.phase = "name_input"
    save_game(uid, state)
    reply(uid,
        "🏰 *DUNGEON REALM*\n\n"
        "An ancient evil stirs in the depths.\n"
        "Heroes are called. Legends are forged.\n\n"
        "⚔️ What is your name, adventurer?"
    )

@bot.message_handler(commands=["new"])
def cmd_new(message):
    uid = message.from_user.id
    state = GameState()
    state.phase = "name_input"
    save_game(uid, state)
    reply(uid, "🔄 *New Adventure!*\n\nWhat is your name, adventurer?")

@bot.message_handler(commands=["status"])
def cmd_status(message):
    uid = message.from_user.id
    state = get_state(uid)
    if state.phase != "playing":
        reply(uid, "❌ No active game. Use /start to begin!")
        return
    reply(uid, state.status_text(), explore_keyboard())

@bot.message_handler(commands=["inventory"])
def cmd_inventory(message):
    uid = message.from_user.id
    state = get_state(uid)
    if state.phase != "playing":
        reply(uid, "❌ No active game. Use /start to begin!")
        return
    reply(uid, state.inventory_text(), explore_keyboard())

@bot.message_handler(commands=["help"])
def cmd_help(message):
    reply(message.from_user.id,
        "⚔️ *DUNGEON REALM — Commands*\n\n"
        "/start — Start or resume your adventure\n"
        "/new — Start a fresh adventure\n"
        "/status — View character stats\n"
        "/inventory — View your items\n"
        "/help — Show this menu\n\n"
        "*How to play:*\n"
        "• Tap buttons to explore, fight, and survive\n"
        "• Beat monsters → earn XP & Gold\n"
        "• Level up to grow stronger\n"
        "• Visit the Shop to buy gear\n"
        "• Rest to recover HP\n\n"
        "🧙 *Claude AI* is your Dungeon Master!"
    )

# ─── Text input ───────────────────────────────────────────────────────────────

@bot.message_handler(func=lambda m: True)
def handle_text(message):
    uid = message.from_user.id
    state = get_state(uid)
    text = message.text.strip()

    if state.phase == "name_input":
        if len(text) < 2 or len(text) > 20:
            reply(uid, "⚠️ Name must be 2-20 characters. Try again:")
            return
        state.name = text
        state.phase = "class_select"
        save_game(uid, state)
        reply(uid,
            f"✨ *{text}* — a name that will echo through the ages!\n\n"
            "Choose your class:",
            class_keyboard()
        )
    else:
        if state.phase == "playing":
            reply(uid, "Tap the buttons below to continue your adventure!", explore_keyboard())

# ─── Callbacks ────────────────────────────────────────────────────────────────

@bot.callback_query_handler(func=lambda c: True)
def handle_callback(call):
    uid = call.from_user.id
    data = call.data
    state = get_state(uid)

    try:
        bot.answer_callback_query(call.id)
    except Exception:
        pass

    # Class selection
    if data.startswith("class_"):
        cls_name = data[6:]
        char_class = CharacterClass[cls_name]
        state.apply_class(char_class)
        state.phase = "playing"
        state.turn = 1
        save_game(uid, state)
        emoji = {"WARRIOR": "⚔️", "MAGE": "🧙", "ROGUE": "🗡️"}[cls_name]
        reply(uid,
            f"{emoji} *{state.name} the {char_class.value}* awakens!\n\n"
            f"❤️ HP: {state._hp}  ⚔️ ATK: {state.attack}  🛡️ DEF: {state.defense}  ✨ MAG: {state.magic}\n\n"
            f"You stand at the entrance of {state.current_location}.\n"
            f"The air reeks of danger and forgotten treasure...\n\n"
            "What will you do?",
            explore_keyboard()
        )
        return

    # Quick panels
    if data == "quick_status":
        reply(uid, state.status_text(), explore_keyboard())
        return
    if data == "quick_inventory":
        reply(uid, state.inventory_text(), explore_keyboard())
        return

    # Main actions
    if data == "action_explore":
        do_explore(uid, state)
        return
    if data == "action_rest":
        do_rest(uid, state)
        return
    if data == "action_shop":
        reply(uid,
            f"🏪 *THE MARKET*\n\n🪙 Your gold: {state.gold}\n\nWhat would you like to buy?",
            shop_keyboard(state)
        )
        return

    # Story choices
    if data.startswith("choice_"):
        idx = int(data[7:])
        if idx < len(state.pending_choices):
            do_choice(uid, state, state.pending_choices[idx])
        return

    # Shop
    if data.startswith("buy_"):
        do_buy(uid, state, data[4:])
        return
    if data == "shop_leave":
        reply(uid, "🚪 You leave the market.", explore_keyboard())
        return

    # Combat
    if data.startswith("combat_"):
        do_combat_action(uid, state, data[7:])
        return

# ─── Game logic ───────────────────────────────────────────────────────────────

def do_explore(uid: int, state: GameState):
    state.turn += 1
    state.current_location = random.choice(LOCATIONS)

    # Combat encounter (40%)
    if random.random() < 0.40:
        start_combat(uid, state)
        return

    # Loot drop (20%)
    extra = ""
    if random.random() < 0.20:
        loot_items = [k for k, v in ITEMS.items() if v["type"] != "currency"]
        found = random.choice(loot_items)
        state.inventory.append(found)
        info = ITEMS[found]
        extra = f"\n\n{info['emoji']} *Found:* {found}!"

    narrative, choices = ai_master.generate_exploration(state)
    state.pending_choices = choices
    save_game(uid, state)

    reply(uid,
        f"🔍 *Exploring {state.current_location}...*\n\n{narrative}{extra}",
        action_keyboard(choices)
    )

def do_choice(uid: int, state: GameState, choice: str):
    state.story_history.append(choice)
    if len(state.story_history) > 6:
        state.story_history = state.story_history[-6:]

    outcome = ai_master.generate_choice_outcome(state, choice)
    reward = ""
    roll = random.random()
    if roll < 0.15:
        g = random.randint(5, 20)
        state.gold += g
        reward = f"\n💰 You found {g} gold!"
    elif roll < 0.25:
        h = random.randint(5, 15)
        state.hp = min(state.max_hp, state._hp + h)
        reward = f"\n💚 Recovered {h} HP!"

    save_game(uid, state)
    reply(uid,
        f"📖 *{choice}*\n\n{outcome}{reward}\n\nWhat next?",
        explore_keyboard()
    )

def do_rest(uid: int, state: GameState):
    cost = 10
    if state.gold < cost:
        heal = int(state.max_hp * 0.3)
        state.hp = min(state.max_hp, state._hp + heal)
        save_game(uid, state)
        reply(uid,
            f"💤 *Brief Rest* (free)\n\nNot enough gold for an inn, but you recover {heal} HP.\n"
            f"❤️ HP: {state._hp}/{state.max_hp}",
            explore_keyboard()
        )
    else:
        state.gold -= cost
        state.hp = state.max_hp
        save_game(uid, state)
        reply(uid,
            f"🏕️ *Full Rest* (-{cost}🪙)\n\nYou sleep soundly and awake fully healed.\n"
            f"❤️ HP: {state._hp}/{state.max_hp} | 🪙 Gold: {state.gold}",
            explore_keyboard()
        )

def do_buy(uid: int, state: GameState, item_name: str):
    prices = {
        "Health Potion": 15, "Elixir": 35, "Iron Sword": 40,
        "Steel Shield": 35, "Enchanted Staff": 55, "Magic Amulet": 30,
    }
    price = prices.get(item_name, 999)
    if state.gold < price:
        reply(uid, f"❌ Not enough gold! Need {price}🪙, have {state.gold}🪙.", explore_keyboard())
        return
    state.gold -= price
    state.inventory.append(item_name)
    info = ITEMS[item_name]

    # Auto-equip weapons/armor
    equip_msg = ""
    if info["type"] == "weapon" and not state.equipped_weapon:
        state.equipped_weapon = item_name
        equip_msg = f"\n⚔️ Auto-equipped! ATK is now {state.effective_attack()}."
    elif info["type"] == "armor" and not state.equipped_armor:
        state.equipped_armor = item_name
        equip_msg = f"\n🛡️ Auto-equipped! DEF is now {state.effective_defense()}."

    save_game(uid, state)
    reply(uid,
        f"✅ Bought {info['emoji']} *{item_name}*!{equip_msg}\n"
        f"🪙 Gold remaining: {state.gold}",
        explore_keyboard()
    )

# ─── Combat ───────────────────────────────────────────────────────────────────

def start_combat(uid: int, state: GameState):
    eligible = [m for m in MONSTERS if abs(m["xp"] / 15 - state.level) <= 2] or MONSTERS
    template = random.choice(eligible)
    enemy = dict(template)
    scale = 1 + (state.level - 1) * 0.15
    enemy["hp"] = int(enemy["hp"] * scale)
    enemy["attack"] = int(enemy["attack"] * scale)
    state.enemy = enemy
    state.in_combat = True
    save_game(uid, state)

    has_potion = "Health Potion" in state.inventory or "Elixir" in state.inventory
    reply(uid,
        f"⚠️ *ENCOUNTER!*\n\n"
        f"{enemy['emoji']} A wild **{enemy['name']}** appears!\n\n"
        f"💀 Enemy HP: {enemy['hp']} | ⚔️ ATK: {enemy['attack']}\n"
        f"Your ❤️ HP: {state._hp}/{state.max_hp}\n\n"
        "Choose your action:",
        combat_keyboard(has_potion)
    )

def do_combat_action(uid: int, state: GameState, action: str):
    if not state.in_combat or not state.enemy:
        reply(uid, "No active combat.", explore_keyboard())
        return

    enemy = state.enemy
    lines = []
    defending = False

    if action == "attack":
        dmg = max(1, state.effective_attack() + random.randint(-3, 5))
        crit = random.random() < 0.15
        if crit:
            dmg = int(dmg * 1.8)
            lines.append(f"💥 *CRITICAL HIT!* {ai_master.generate_combat_hit(state.name, enemy['name'], dmg)}")
        else:
            lines.append(f"⚔️ {ai_master.generate_combat_hit(state.name, enemy['name'], dmg)}")
        enemy["hp"] -= dmg
        lines.append(f"  → Dealt *{dmg}* damage! Enemy HP: {max(0, enemy['hp'])}")

    elif action == "defend":
        defending = True
        lines.append("🛡️ *You raise your guard — defense doubled this turn!*")

    elif action == "dodge":
        if random.random() < 0.5:
            lines.append("💨 *Perfect dodge!* You slip past the attack entirely!")
            state.enemy = enemy
            save_game(uid, state)
            if enemy["hp"] <= 0:
                end_combat_victory(uid, state, lines)
            else:
                has_potion = "Health Potion" in state.inventory or "Elixir" in state.inventory
                reply(uid, "\n".join(lines), combat_keyboard(has_potion))
            return
        else:
            lines.append("💨 *Dodge failed!* You stumble forward...")

    elif action == "potion":
        potion = "Elixir" if "Elixir" in state.inventory else "Health Potion"
        info = ITEMS[potion]
        actual = min(info["value"], state.max_hp - state._hp)
        state.hp = state._hp + actual
        state.inventory.remove(potion)
        lines.append(f"🧪 Drank *{potion}*! Recovered *{actual}* HP → {state._hp}/{state.max_hp}")

    elif action == "flee":
        if random.random() < 0.5:
            state.in_combat = False
            state.enemy = None
            save_game(uid, state)
            reply(uid, "🏃 *You flee into the shadows and escape safely!*", explore_keyboard())
            return
        else:
            lines.append("🏃 *Flee failed!* The enemy blocks your path!")

    # Victory check
    if enemy["hp"] <= 0:
        state.enemy = enemy
        end_combat_victory(uid, state, lines)
        return

    # Enemy attacks
    dodge_chance = 0.15
    if random.random() < dodge_chance:
        lines.append(f"\n{enemy['emoji']} {ai_master.generate_enemy_attack(enemy['name'], 0, True)}")
        lines.append("  → *Dodged!* No damage taken.")
    else:
        raw = max(1, enemy["attack"] + random.randint(-2, 4))
        dmg = max(1, raw - state.effective_defense() // (2 if defending else 1))
        lines.append(f"\n{enemy['emoji']} {ai_master.generate_enemy_attack(enemy['name'], dmg, False)}")
        state.hp = state._hp - dmg
        lines.append(f"  → You take *{dmg}* damage! ❤️ {state._hp}/{state.max_hp}")

    state.enemy = enemy
    save_game(uid, state)

    if state._hp <= 0:
        end_combat_death(uid, state, lines)
        return

    lines.append(f"\n📊 You: ❤️ {state._hp}/{state.max_hp}  |  {enemy['emoji']} {enemy['name']}: {enemy['hp']} HP")
    has_potion = "Health Potion" in state.inventory or "Elixir" in state.inventory
    reply(uid, "\n".join(lines), combat_keyboard(has_potion))

def end_combat_victory(uid: int, state: GameState, lines: list):
    enemy = state.enemy
    xp = enemy["xp"]
    gold = random.randint(enemy["gold"] // 2, enemy["gold"])
    state.kills += 1
    state.gold += gold
    leveled = state.gain_xp(xp)
    state.in_combat = False
    state.enemy = None

    loot_msg = ""
    if random.random() < 0.30:
        loot_items = [k for k, v in ITEMS.items() if v["type"] != "currency"]
        loot = random.choice(loot_items)
        state.inventory.append(loot)
        loot_msg = f"\n{ITEMS[loot]['emoji']} *Loot:* {loot}!"

    save_game(uid, state)
    lines.append(f"\n🏆 *{ai_master.generate_victory(enemy['name'])}*")
    lines.append(f"\n⭐ +{xp} XP  |  🪙 +{gold} Gold{loot_msg}")

    if leveled:
        lines.append(f"\n{ai_master.generate_level_up_message()}")
        lines.append(f"🎉 *LEVEL UP! You are now Level {state.level}!*")
        lines.append(f"HP fully restored! ❤️ {state._hp}/{state.max_hp}")

    reply(uid, "\n".join(lines), explore_keyboard())

def end_combat_death(uid: int, state: GameState, lines: list):
    death_msg = ai_master.generate_death_message(state.name, state.kills)
    lines.append(f"\n💀 *YOU HAVE FALLEN...*\n\n{death_msg}")
    lines.append(f"\n📜 *Final Stats:* Level {state.level} | ☠️ {state.kills} kills | 🪙 {state.gold} gold")
    new_state = GameState()
    new_state.phase = "idle"
    save_game(uid, new_state)
    lines.append("\n\nType /start to begin a new adventure!")
    reply(uid, "\n".join(lines))

# ─── Main ─────────────────────────────────────────────────────────────────────

if __name__ == "__main__":
    log.info("🎮 Dungeon Realm starting...")

    # Notify owner that bot is ready
    try:
        bot.send_message(
            OWNER_CHAT_ID,
            "🎮 *Dungeon Realm is READY!*\n\n"
            "Your AI-powered RPG adventure bot is online!\n\n"
            "⚔️ /start — Begin your quest\n"
            "📜 /help — All commands\n"
            "🗡️ /new — Fresh adventure\n\n"
            "_Powered by Claude AI as your Dungeon Master_ 🧙",
            parse_mode="Markdown"
        )
        log.info(f"✅ Startup notification sent to {OWNER_CHAT_ID}")
    except Exception as e:
        log.error(f"❌ Startup notification failed: {e}")

    log.info("Bot polling...")
    bot.infinity_polling(timeout=30, long_polling_timeout=20)
