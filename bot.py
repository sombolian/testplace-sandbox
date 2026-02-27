"""
🎮 AI Dungeon RPG — Telegram Bot
Powered by Claude AI as the Dungeon Master.
"""
import os
import json
import random
import logging
import asyncio
from pathlib import Path

from telegram import Update, InlineKeyboardButton, InlineKeyboardMarkup
from telegram.ext import (
    Application, CommandHandler, CallbackQueryHandler,
    MessageHandler, filters, ContextTypes
)

from game_state import GameState, CharacterClass, MONSTERS, ITEMS, LOCATIONS
import ai_master

logging.basicConfig(format="%(asctime)s - %(levelname)s - %(message)s", level=logging.INFO)
log = logging.getLogger(__name__)

TOKEN = "8599833483:AAEJ7mmH-rNQQUvXBrIbatVgmNbE2C7_4OU"
OWNER_CHAT_ID = 1625078551
SAVE_DIR = Path("saves")
SAVE_DIR.mkdir(exist_ok=True)

# ─── Save / Load ──────────────────────────────────────────────────────────────

def save_game(user_id: int, state: GameState):
    path = SAVE_DIR / f"{user_id}.json"
    with open(path, "w") as f:
        json.dump(state.to_dict(), f)

def load_game(user_id: int) -> GameState | None:
    path = SAVE_DIR / f"{user_id}.json"
    if path.exists():
        with open(path) as f:
            return GameState.from_dict(json.load(f))
    return None

def get_state(user_id: int) -> GameState:
    state = load_game(user_id)
    if state is None:
        state = GameState()
    return state

# ─── Keyboards ────────────────────────────────────────────────────────────────

def class_keyboard():
    return InlineKeyboardMarkup([
        [InlineKeyboardButton("⚔️ Warrior — Tank & bruiser", callback_data="class_WARRIOR")],
        [InlineKeyboardButton("🧙 Mage — High magic damage", callback_data="class_MAGE")],
        [InlineKeyboardButton("🗡️ Rogue — Fast & deadly",   callback_data="class_ROGUE")],
    ])

def action_keyboard(choices: list[str]):
    rows = [[InlineKeyboardButton(f"{i+1}. {c}", callback_data=f"choice_{i}")] for i, c in enumerate(choices)]
    rows.append([
        InlineKeyboardButton("📊 Status", callback_data="quick_status"),
        InlineKeyboardButton("🎒 Bag",    callback_data="quick_inventory"),
    ])
    return InlineKeyboardMarkup(rows)

def combat_keyboard(has_potion: bool):
    rows = [
        [InlineKeyboardButton("⚔️ Attack",        callback_data="combat_attack")],
        [InlineKeyboardButton("🛡️ Defend (+DEF)", callback_data="combat_defend")],
        [InlineKeyboardButton("💨 Dodge (50% chance)", callback_data="combat_dodge")],
    ]
    if has_potion:
        rows.append([InlineKeyboardButton("🧪 Use Health Potion", callback_data="combat_potion")])
    rows.append([InlineKeyboardButton("🏃 Flee (50% chance)", callback_data="combat_flee")])
    return InlineKeyboardMarkup(rows)

def explore_keyboard():
    return InlineKeyboardMarkup([[
        InlineKeyboardButton("🗺️ Explore",   callback_data="action_explore"),
        InlineKeyboardButton("🏪 Shop",      callback_data="action_shop"),
        InlineKeyboardButton("💤 Rest",      callback_data="action_rest"),
    ]])

# ─── Helpers ──────────────────────────────────────────────────────────────────

async def send(update: Update, text: str, keyboard=None, parse_mode="Markdown"):
    kwargs = {"parse_mode": parse_mode}
    if keyboard:
        kwargs["reply_markup"] = keyboard
    if update.callback_query:
        try:
            await update.callback_query.answer()
        except Exception:
            pass
        await update.callback_query.message.reply_text(text, **kwargs)
    else:
        await update.message.reply_text(text, **kwargs)

# ─── /start ───────────────────────────────────────────────────────────────────

async def cmd_start(update: Update, ctx: ContextTypes.DEFAULT_TYPE):
    user = update.effective_user
    state = get_state(user.id)

    if state.phase == "playing":
        await send(update,
            f"⚔️ Welcome back, *{state.name}*!\n\n"
            f"Level {state.level} {state.char_class.value} — HP {state._hp}/{state.max_hp}\n\n"
            "What will you do?",
            explore_keyboard()
        )
        return

    await send(update,
        "🏰 *DUNGEON REALM*\n\n"
        "An ancient evil stirs in the depths.\n"
        "Heroes are called. Legends are forged.\n\n"
        "What is your name, adventurer?",
    )
    state.phase = "name_input"
    save_game(user.id, state)

async def cmd_new(update: Update, ctx: ContextTypes.DEFAULT_TYPE):
    user = update.effective_user
    state = GameState()
    state.phase = "name_input"
    save_game(user.id, state)
    await send(update,
        "🔄 *New Adventure Started!*\n\n"
        "What is your name, adventurer?"
    )

async def cmd_status(update: Update, ctx: ContextTypes.DEFAULT_TYPE):
    state = get_state(update.effective_user.id)
    if state.phase != "playing":
        await send(update, "❌ No active game. Use /start to begin!")
        return
    await send(update, state.status_text(), explore_keyboard())

async def cmd_inventory(update: Update, ctx: ContextTypes.DEFAULT_TYPE):
    state = get_state(update.effective_user.id)
    if state.phase != "playing":
        await send(update, "❌ No active game. Use /start to begin!")
        return
    await send(update, state.inventory_text(), explore_keyboard())

async def cmd_help(update: Update, ctx: ContextTypes.DEFAULT_TYPE):
    await send(update,
        "⚔️ *DUNGEON REALM — Commands*\n\n"
        "/start — Start or resume your adventure\n"
        "/new — Start a fresh adventure\n"
        "/status — View your character stats\n"
        "/inventory — View your items\n"
        "/help — Show this menu\n\n"
        "*How to play:*\n"
        "• Use the buttons to explore, fight, and survive\n"
        "• Beat monsters to earn XP and Gold\n"
        "• Level up to grow stronger\n"
        "• Visit the Shop to buy items\n"
        "• Rest to recover HP\n\n"
        "🧙 *AI Dungeon Master* narrates your journey!"
    )

# ─── Text input handler ───────────────────────────────────────────────────────

async def handle_text(update: Update, ctx: ContextTypes.DEFAULT_TYPE):
    user = update.effective_user
    state = get_state(user.id)
    text = update.message.text.strip()

    if state.phase == "name_input":
        if len(text) < 2 or len(text) > 20:
            await send(update, "⚠️ Name must be 2-20 characters. Try again:")
            return
        state.name = text
        state.phase = "class_select"
        save_game(user.id, state)
        await send(update,
            f"✨ *{text}* — a name that will be remembered!\n\n"
            "Choose your class:",
            class_keyboard()
        )
    else:
        await send(update, "Use the buttons below to play!", explore_keyboard())

# ─── Callback handler ─────────────────────────────────────────────────────────

async def handle_callback(update: Update, ctx: ContextTypes.DEFAULT_TYPE):
    user = update.effective_user
    data = update.callback_query.data
    state = get_state(user.id)

    # ── Class selection ──
    if data.startswith("class_"):
        cls_name = data[6:]
        char_class = CharacterClass[cls_name]
        state.apply_class(char_class)
        state.phase = "playing"
        state.turn = 1
        save_game(user.id, state)
        emoji = {"WARRIOR": "⚔️", "MAGE": "🧙", "ROGUE": "🗡️"}[cls_name]
        await send(update,
            f"{emoji} *{state.name} the {char_class.value}* awakens!\n\n"
            f"❤️ HP: {state._hp}  ⚔️ ATK: {state.attack}  🛡️ DEF: {state.defense}\n\n"
            f"You stand at the entrance of {state.current_location}.\n"
            f"The air smells of danger and treasure...\n\n"
            "What will you do?",
            explore_keyboard()
        )
        return

    # ── Quick status / inventory ──
    if data == "quick_status":
        await send(update, state.status_text(), explore_keyboard())
        return
    if data == "quick_inventory":
        await send(update, state.inventory_text(), explore_keyboard())
        return

    # ── Main actions ──
    if data == "action_explore":
        await do_explore(update, state, user.id)
        return
    if data == "action_rest":
        await do_rest(update, state, user.id)
        return
    if data == "action_shop":
        await do_shop(update, state, user.id)
        return

    # ── Story choices ──
    if data.startswith("choice_"):
        idx = int(data[7:])
        if idx < len(state.pending_choices):
            choice = state.pending_choices[idx]
            await do_choice(update, state, user.id, choice)
        return

    # ── Shop ──
    if data.startswith("buy_"):
        item_name = data[4:]
        await do_buy(update, state, user.id, item_name)
        return
    if data == "shop_leave":
        await send(update, "🚪 You leave the shop.", explore_keyboard())
        return

    # ── Combat ──
    if data.startswith("combat_"):
        action = data[7:]
        await do_combat_action(update, state, user.id, action)
        return

    # ── Use item ──
    if data.startswith("use_"):
        item_name = data[4:]
        await do_use_item(update, state, user.id, item_name)
        return

    # ── Equip ──
    if data.startswith("equip_"):
        item_name = data[6:]
        await do_equip(update, state, user.id, item_name)
        return

# ─── Game actions ─────────────────────────────────────────────────────────────

async def do_explore(update: Update, state: GameState, user_id: int):
    state.turn += 1
    state.current_location = random.choice(LOCATIONS)

    # Random encounter chance (40%)
    if random.random() < 0.40:
        await start_combat(update, state, user_id)
        return

    # Loot find chance (20%)
    if random.random() < 0.20:
        loot_items = [k for k, v in ITEMS.items() if v["type"] != "currency"]
        found = random.choice(loot_items)
        state.inventory.append(found)
        info = ITEMS[found]
        save_game(user_id, state)
        narrative, choices = ai_master.generate_exploration(state)
        state.pending_choices = choices
        save_game(user_id, state)
        await send(update,
            f"🔍 *Exploring {state.current_location}...*\n\n"
            f"✨ You found: {info['emoji']} **{found}**!\n\n"
            f"{narrative}",
            action_keyboard(choices)
        )
        return

    narrative, choices = ai_master.generate_exploration(state)
    state.pending_choices = choices
    save_game(user_id, state)
    await send(update,
        f"🔍 *Exploring {state.current_location}...*\n\n{narrative}",
        action_keyboard(choices)
    )

async def do_choice(update: Update, state: GameState, user_id: int, choice: str):
    state.story_history.append(choice)
    if len(state.story_history) > 6:
        state.story_history = state.story_history[-6:]

    outcome = ai_master.generate_choice_outcome(state, choice)

    # Small random reward for engaging with story
    reward = ""
    roll = random.random()
    if roll < 0.15:
        gold = random.randint(5, 20)
        state.gold += gold
        reward = f"\n💰 You found {gold} gold!"
    elif roll < 0.25:
        hp_heal = random.randint(5, 15)
        state.hp = min(state.max_hp, state._hp + hp_heal)
        reward = f"\n💚 You recovered {hp_heal} HP!"

    save_game(user_id, state)
    await send(update,
        f"📖 *{choice}*\n\n{outcome}{reward}\n\n"
        "What next?",
        explore_keyboard()
    )

async def do_rest(update: Update, state: GameState, user_id: int):
    cost = 10
    if state.gold < cost:
        heal = int(state.max_hp * 0.3)
        state.hp = min(state.max_hp, state._hp + heal)
        save_game(user_id, state)
        await send(update,
            f"💤 *You rest briefly...*\n\n"
            f"Not enough gold for a proper rest, but you recover {heal} HP.\n"
            f"❤️ HP: {state._hp}/{state.max_hp}",
            explore_keyboard()
        )
    else:
        state.gold -= cost
        state.hp = state.max_hp
        save_game(user_id, state)
        await send(update,
            f"🏕️ *Full Rest* (Cost: {cost}🪙)\n\n"
            f"You sleep safely and wake fully restored.\n"
            f"❤️ HP: {state._hp}/{state.max_hp} | 🪙 Gold: {state.gold}",
            explore_keyboard()
        )

async def do_shop(update: Update, state: GameState, user_id: int):
    shop_items = {
        "Health Potion":   15,
        "Elixir":          35,
        "Iron Sword":      40,
        "Steel Shield":    35,
        "Enchanted Staff": 55,
        "Magic Amulet":    30,
    }
    rows = []
    for item, price in shop_items.items():
        info = ITEMS[item]
        affordable = "✅" if state.gold >= price else "❌"
        rows.append([InlineKeyboardButton(
            f"{affordable} {info['emoji']} {item} — {price}🪙",
            callback_data=f"buy_{item}"
        )])
    rows.append([InlineKeyboardButton("🚪 Leave Shop", callback_data="shop_leave")])

    await send(update,
        f"🏪 *THE MARKET*\n\n"
        f"🪙 Your gold: {state.gold}\n\n"
        "What would you like to buy?",
        InlineKeyboardMarkup(rows)
    )

async def do_buy(update: Update, state: GameState, user_id: int, item_name: str):
    prices = {
        "Health Potion": 15, "Elixir": 35, "Iron Sword": 40,
        "Steel Shield": 35, "Enchanted Staff": 55, "Magic Amulet": 30,
    }
    price = prices.get(item_name, 999)
    if state.gold < price:
        await send(update, f"❌ Not enough gold! You need {price}🪙 but have {state.gold}🪙.", explore_keyboard())
        return
    state.gold -= price
    state.inventory.append(item_name)
    info = ITEMS[item_name]
    save_game(user_id, state)
    await send(update,
        f"✅ Purchased {info['emoji']} *{item_name}*!\n"
        f"🪙 Gold remaining: {state.gold}\n\n"
        "Use /inventory to equip or use items.",
        explore_keyboard()
    )

async def do_use_item(update: Update, state: GameState, user_id: int, item_name: str):
    if item_name not in state.inventory:
        await send(update, "❌ You don't have that item!", explore_keyboard())
        return
    info = ITEMS.get(item_name, {})
    if info.get("type") == "consumable" and info.get("effect") == "heal":
        heal = info["value"]
        actual = min(heal, state.max_hp - state._hp)
        state.hp = state._hp + actual
        state.inventory.remove(item_name)
        save_game(user_id, state)
        await send(update,
            f"🧪 Used *{item_name}*!\n"
            f"❤️ Restored {actual} HP → {state._hp}/{state.max_hp}",
            explore_keyboard()
        )
    else:
        await send(update, f"⚠️ You can't use *{item_name}* directly. Try equipping it!", explore_keyboard())

async def do_equip(update: Update, state: GameState, user_id: int, item_name: str):
    if item_name not in state.inventory:
        await send(update, "❌ You don't have that item!", explore_keyboard())
        return
    info = ITEMS.get(item_name, {})
    if info.get("type") == "weapon":
        state.equipped_weapon = item_name
        save_game(user_id, state)
        await send(update, f"⚔️ Equipped *{item_name}*! ATK is now {state.effective_attack()}.", explore_keyboard())
    elif info.get("type") == "armor":
        state.equipped_armor = item_name
        save_game(user_id, state)
        await send(update, f"🛡️ Equipped *{item_name}*! DEF is now {state.effective_defense()}.", explore_keyboard())
    else:
        await send(update, "⚠️ This item can't be equipped.", explore_keyboard())

# ─── Combat ───────────────────────────────────────────────────────────────────

async def start_combat(update: Update, state: GameState, user_id: int):
    # Scale monster to player level
    eligible = [m for m in MONSTERS if abs(m["xp"] / 15 - state.level) <= 2] or MONSTERS
    template = random.choice(eligible)
    enemy = dict(template)
    # Scale HP and attack with level
    scale = 1 + (state.level - 1) * 0.15
    enemy["hp"] = int(enemy["hp"] * scale)
    enemy["attack"] = int(enemy["attack"] * scale)
    state.enemy = enemy
    state.in_combat = True
    save_game(user_id, state)

    has_potion = "Health Potion" in state.inventory or "Elixir" in state.inventory
    await send(update,
        f"⚠️ *ENCOUNTER!*\n\n"
        f"{enemy['emoji']} A **{enemy['name']}** appears!\n\n"
        f"💀 Enemy HP: {enemy['hp']}\n"
        f"⚔️ Enemy ATK: {enemy['attack']}\n\n"
        f"Your HP: {state._hp}/{state.max_hp}\n\n"
        "Choose your action:",
        combat_keyboard(has_potion)
    )

async def do_combat_action(update: Update, state: GameState, user_id: int, action: str):
    if not state.in_combat or not state.enemy:
        await send(update, "No active combat.", explore_keyboard())
        return

    enemy = state.enemy
    lines = []
    defending = False

    # ── Player action ──
    if action == "attack":
        base_dmg = state.effective_attack()
        variance = random.randint(-3, 5)
        dmg = max(1, base_dmg + variance)
        crit = random.random() < 0.15
        if crit:
            dmg = int(dmg * 1.8)
            lines.append(f"💥 *CRITICAL HIT!* {ai_master.generate_combat_hit(state.name, enemy['name'], dmg)}")
        else:
            lines.append(f"⚔️ {ai_master.generate_combat_hit(state.name, enemy['name'], dmg)}")
        enemy["hp"] -= dmg
        lines.append(f"  → Dealt **{dmg}** damage! Enemy HP: {max(0, enemy['hp'])}")

    elif action == "defend":
        defending = True
        lines.append("🛡️ *You brace yourself, raising your guard!*")
        lines.append("  → Defense doubled this turn!")

    elif action == "dodge":
        if random.random() < 0.5:
            lines.append("💨 *Perfect dodge!* You slip past the enemy's attack entirely!")
            state.enemy = enemy
            save_game(user_id, state)
            # Skip enemy turn
            if enemy["hp"] <= 0:
                await end_combat_victory(update, state, user_id, lines)
            else:
                has_potion = "Health Potion" in state.inventory or "Elixir" in state.inventory
                await send(update, "\n".join(lines), combat_keyboard(has_potion))
            return
        else:
            lines.append("💨 *Dodge failed!* You stumble forward...")

    elif action == "potion":
        potion = "Elixir" if "Elixir" in state.inventory else "Health Potion"
        info = ITEMS[potion]
        heal = info["value"]
        actual = min(heal, state.max_hp - state._hp)
        state.hp = state._hp + actual
        state.inventory.remove(potion)
        lines.append(f"🧪 Drank *{potion}*! Recovered **{actual}** HP → {state._hp}/{state.max_hp}")

    elif action == "flee":
        if random.random() < 0.5:
            state.in_combat = False
            state.enemy = None
            save_game(user_id, state)
            await send(update, "🏃 *You flee into the shadows!*\n\nYou escape safely.", explore_keyboard())
            return
        else:
            lines.append("🏃 *Flee failed!* The enemy blocks your path!")

    # ── Check if enemy dead ──
    if enemy["hp"] <= 0:
        state.enemy = enemy
        await end_combat_victory(update, state, user_id, lines)
        return

    # ── Enemy turn ──
    enemy_atk = enemy["attack"]
    defense_mod = 2 if defending else 1
    dodge_chance = 0.15
    if random.random() < dodge_chance:
        narr = ai_master.generate_enemy_attack(enemy["name"], 0, dodged=True)
        lines.append(f"\n{enemy['emoji']} {narr}")
        lines.append(f"  → Dodged! No damage taken.")
    else:
        variance = random.randint(-2, 4)
        raw = max(1, enemy_atk + variance)
        actual_dmg = max(1, raw - state.effective_defense() // defense_mod)
        narr = ai_master.generate_enemy_attack(enemy["name"], actual_dmg, dodged=False)
        lines.append(f"\n{enemy['emoji']} {narr}")
        state.hp = state._hp - actual_dmg
        lines.append(f"  → You take **{actual_dmg}** damage! Your HP: {state._hp}/{state.max_hp}")

    state.enemy = enemy
    save_game(user_id, state)

    # ── Check player death ──
    if state._hp <= 0:
        await end_combat_death(update, state, user_id, lines)
        return

    lines.append(f"\n📊 *Battle Status*")
    lines.append(f"You: ❤️ {state._hp}/{state.max_hp}  |  {enemy['emoji']} {enemy['name']}: 💀 {enemy['hp']}")

    has_potion = "Health Potion" in state.inventory or "Elixir" in state.inventory
    await send(update, "\n".join(lines), combat_keyboard(has_potion))

async def end_combat_victory(update: Update, state: GameState, user_id: int, lines: list):
    enemy = state.enemy
    xp_gain = enemy["xp"]
    gold_gain = random.randint(enemy["gold"] // 2, enemy["gold"])

    state.kills += 1
    state.gold += gold_gain
    leveled = state.gain_xp(xp_gain)
    state.in_combat = False
    state.enemy = None

    # Loot drop (30%)
    loot_msg = ""
    if random.random() < 0.30:
        loot_items = [k for k, v in ITEMS.items() if v["type"] != "currency"]
        loot = random.choice(loot_items)
        state.inventory.append(loot)
        info = ITEMS[loot]
        loot_msg = f"\n{info['emoji']} Loot: **{loot}** found!"

    save_game(user_id, state)

    lines.append(f"\n🏆 *{ai_master.generate_victory(enemy['name'])}*")
    lines.append(f"\n⭐ +{xp_gain} XP  |  🪙 +{gold_gain} Gold{loot_msg}")

    if leveled:
        lines.append(f"\n{ai_master.generate_level_up_message()}")
        lines.append(f"🎉 *LEVEL UP! You are now Level {state.level}!*")
        lines.append(f"HP fully restored! New HP: {state._hp}/{state.max_hp}")

    await send(update, "\n".join(lines), explore_keyboard())

async def end_combat_death(update: Update, state: GameState, user_id: int, lines: list):
    death_msg = ai_master.generate_death_message(state.name, state.kills)
    lines.append(f"\n💀 *YOU HAVE FALLEN...*\n\n{death_msg}")
    lines.append(f"\n📜 *Final Stats:* Level {state.level} | {state.kills} kills | {state.gold} gold")

    # Reset game
    new_state = GameState()
    new_state.phase = "idle"
    save_game(user_id, new_state)

    lines.append("\n\nUse /start to begin a new adventure!")
    await send(update, "\n".join(lines))

# ─── Startup notification ─────────────────────────────────────────────────────

async def notify_owner(app: Application):
    """Send a message to the owner when the bot is ready."""
    try:
        await app.bot.send_message(
            chat_id=OWNER_CHAT_ID,
            text=(
                "🎮 *Dungeon Realm is READY!*\n\n"
                "Your AI-powered RPG adventure bot is online and waiting for you!\n\n"
                "⚔️ Type /start to begin your quest!\n"
                "📜 Type /help for all commands\n\n"
                "_Powered by Claude AI as your Dungeon Master_ 🧙"
            ),
            parse_mode="Markdown"
        )
        log.info(f"Startup notification sent to {OWNER_CHAT_ID}")
    except Exception as e:
        log.error(f"Failed to send startup notification: {e}")

# ─── Main ─────────────────────────────────────────────────────────────────────

def main():
    app = Application.builder().token(TOKEN).build()

    app.add_handler(CommandHandler("start",     cmd_start))
    app.add_handler(CommandHandler("new",       cmd_new))
    app.add_handler(CommandHandler("status",    cmd_status))
    app.add_handler(CommandHandler("inventory", cmd_inventory))
    app.add_handler(CommandHandler("help",      cmd_help))
    app.add_handler(CallbackQueryHandler(handle_callback))
    app.add_handler(MessageHandler(filters.TEXT & ~filters.COMMAND, handle_text))

    async def post_init(application: Application):
        await notify_owner(application)

    app.post_init = post_init

    log.info("🎮 Dungeon Realm bot starting...")
    app.run_polling(drop_pending_updates=True)

if __name__ == "__main__":
    main()
