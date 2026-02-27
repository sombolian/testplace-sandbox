import json
import random
from enum import Enum

class CharacterClass(Enum):
    WARRIOR = "Warrior"
    MAGE = "Mage"
    ROGUE = "Rogue"

CLASS_STATS = {
    CharacterClass.WARRIOR: {"hp": 120, "max_hp": 120, "attack": 18, "defense": 12, "magic": 3, "emoji": "⚔️"},
    CharacterClass.MAGE:    {"hp": 70,  "max_hp": 70,  "attack": 8,  "defense": 5,  "magic": 20, "emoji": "🧙"},
    CharacterClass.ROGUE:   {"hp": 90,  "max_hp": 90,  "attack": 15, "defense": 8,  "magic": 6,  "emoji": "🗡️"},
}

ITEMS = {
    "Health Potion":    {"type": "consumable", "effect": "heal",    "value": 40,  "emoji": "🧪"},
    "Elixir":           {"type": "consumable", "effect": "heal",    "value": 80,  "emoji": "✨"},
    "Iron Sword":       {"type": "weapon",     "effect": "attack",  "value": 8,   "emoji": "🗡️"},
    "Enchanted Staff":  {"type": "weapon",     "effect": "magic",   "value": 10,  "emoji": "🪄"},
    "Shadow Dagger":    {"type": "weapon",     "effect": "attack",  "value": 12,  "emoji": "🔪"},
    "Steel Shield":     {"type": "armor",      "effect": "defense", "value": 6,   "emoji": "🛡️"},
    "Magic Amulet":     {"type": "accessory",  "effect": "magic",   "value": 5,   "emoji": "📿"},
    "Gold Coin":        {"type": "currency",   "effect": "gold",    "value": 20,  "emoji": "🪙"},
}

MONSTERS = [
    {"name": "Goblin",        "hp": 30,  "attack": 8,  "defense": 2,  "xp": 15, "gold": 10, "emoji": "👺"},
    {"name": "Skeleton",      "hp": 40,  "attack": 12, "defense": 4,  "xp": 20, "gold": 15, "emoji": "💀"},
    {"name": "Wolf",          "hp": 45,  "attack": 14, "defense": 3,  "xp": 22, "gold": 8,  "emoji": "🐺"},
    {"name": "Dark Mage",     "hp": 55,  "attack": 18, "defense": 5,  "xp": 35, "gold": 25, "emoji": "🧟"},
    {"name": "Troll",         "hp": 80,  "attack": 20, "defense": 8,  "xp": 45, "gold": 30, "emoji": "👹"},
    {"name": "Dragon",        "hp": 150, "attack": 35, "defense": 15, "xp": 100,"gold": 100,"emoji": "🐉"},
    {"name": "Shadow Demon",  "hp": 110, "attack": 28, "defense": 10, "xp": 75, "gold": 60, "emoji": "👿"},
]

LOCATIONS = [
    "🌲 Dark Forest",
    "🏔️ Mountain Pass",
    "🏚️ Abandoned Village",
    "⛩️ Ancient Temple",
    "🌋 Volcanic Cavern",
    "🌊 Sunken Ruins",
    "🏰 Haunted Castle",
    "🕸️ Spider Den",
]

class GameState:
    def __init__(self):
        self.phase = "idle"  # idle, class_select, playing, combat, shop
        self.name = ""
        self.char_class = None
        self.level = 1
        self.xp = 0
        self.xp_next = 50
        self.gold = 30
        self.inventory = ["Health Potion"]
        self.equipped_weapon = None
        self.equipped_armor = None
        self.story_history = []  # last few narrative lines
        self.current_location = random.choice(LOCATIONS)
        self.turn = 0
        self.kills = 0
        self.dungeons_cleared = 0
        # combat state
        self.in_combat = False
        self.enemy = None
        self.pending_choices = []

    @property
    def hp(self):
        return self._hp

    @hp.setter
    def hp(self, v):
        self._hp = max(0, v)

    def apply_class(self, char_class: CharacterClass):
        self.char_class = char_class
        stats = CLASS_STATS[char_class]
        self._hp = stats["hp"]
        self.max_hp = stats["max_hp"]
        self.attack = stats["attack"]
        self.defense = stats["defense"]
        self.magic = stats["magic"]

    def effective_attack(self):
        bonus = 0
        if self.equipped_weapon:
            item = ITEMS.get(self.equipped_weapon, {})
            if item.get("effect") in ("attack", "magic"):
                bonus = item["value"]
        return self.attack + bonus

    def effective_defense(self):
        bonus = 0
        if self.equipped_armor:
            item = ITEMS.get(self.equipped_armor, {})
            if item.get("effect") == "defense":
                bonus = item["value"]
        return self.defense + bonus

    def xp_to_level(self):
        return self.level * 50

    def gain_xp(self, amount):
        self.xp += amount
        leveled = False
        while self.xp >= self.xp_to_level():
            self.xp -= self.xp_to_level()
            self.level += 1
            # Stat boost on level up
            self.max_hp += 15
            self._hp = self.max_hp
            self.attack += 3
            self.defense += 2
            leveled = True
        return leveled

    def status_text(self):
        cls = self.char_class
        emoji = CLASS_STATS[cls]["emoji"] if cls else "👤"
        hp_bar = self._hp_bar()
        weapon_str = f"🗡️ {self.equipped_weapon}" if self.equipped_weapon else "👊 Bare hands"
        armor_str = f"🛡️ {self.equipped_armor}" if self.equipped_armor else "👕 No armor"
        return (
            f"═══ 📜 CHARACTER STATUS ═══\n"
            f"{emoji} **{self.name}** — Level {self.level} {cls.value if cls else ''}\n"
            f"❤️ HP: {self._hp}/{self.max_hp}  {hp_bar}\n"
            f"⚔️ ATK: {self.effective_attack()}  🛡️ DEF: {self.effective_defense()}  ✨ MAG: {self.magic}\n"
            f"⭐ XP: {self.xp}/{self.xp_to_level()}\n"
            f"🪙 Gold: {self.gold}\n"
            f"📍 Location: {self.current_location}\n"
            f"🎒 Weapon: {weapon_str}\n"
            f"🎒 Armor: {armor_str}\n"
            f"💀 Kills: {self.kills} | 🏰 Dungeons: {self.dungeons_cleared}\n"
        )

    def _hp_bar(self):
        pct = self._hp / self.max_hp
        filled = int(pct * 10)
        return "[" + "█" * filled + "░" * (10 - filled) + "]"

    def inventory_text(self):
        if not self.inventory:
            return "🎒 Your inventory is empty."
        counts = {}
        for item in self.inventory:
            counts[item] = counts.get(item, 0) + 1
        lines = ["🎒 **Inventory:**"]
        for item, count in counts.items():
            info = ITEMS.get(item, {})
            em = info.get("emoji", "📦")
            qty = f" x{count}" if count > 1 else ""
            lines.append(f"  {em} {item}{qty}")
        return "\n".join(lines)

    def to_dict(self):
        return {
            "phase": self.phase,
            "name": self.name,
            "char_class": self.char_class.value if self.char_class else None,
            "level": self.level,
            "xp": self.xp,
            "gold": self.gold,
            "inventory": self.inventory,
            "equipped_weapon": self.equipped_weapon,
            "equipped_armor": self.equipped_armor,
            "story_history": self.story_history,
            "current_location": self.current_location,
            "turn": self.turn,
            "kills": self.kills,
            "dungeons_cleared": self.dungeons_cleared,
            "in_combat": self.in_combat,
            "enemy": self.enemy,
            "_hp": self._hp,
            "max_hp": self.max_hp,
            "attack": self.attack,
            "defense": self.defense,
            "magic": self.magic,
        }

    @classmethod
    def from_dict(cls, d):
        g = cls.__new__(cls)
        g.phase = d["phase"]
        g.name = d["name"]
        g.char_class = CharacterClass(d["char_class"]) if d["char_class"] else None
        g.level = d["level"]
        g.xp = d["xp"]
        g.gold = d["gold"]
        g.inventory = d["inventory"]
        g.equipped_weapon = d["equipped_weapon"]
        g.equipped_armor = d["equipped_armor"]
        g.story_history = d.get("story_history", [])
        g.current_location = d["current_location"]
        g.turn = d["turn"]
        g.kills = d.get("kills", 0)
        g.dungeons_cleared = d.get("dungeons_cleared", 0)
        g.in_combat = d["in_combat"]
        g.enemy = d["enemy"]
        g._hp = d["_hp"]
        g.max_hp = d["max_hp"]
        g.attack = d["attack"]
        g.defense = d["defense"]
        g.magic = d["magic"]
        g.pending_choices = []
        return g
