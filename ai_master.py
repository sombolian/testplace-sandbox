"""
AI Dungeon Master powered by Claude.
Falls back to rich pre-written narratives if no API key is set.
"""
import os
import random
import anthropic

_client = None

def _get_client():
    global _client
    if _client is None:
        key = os.environ.get("ANTHROPIC_API_KEY", "")
        if key:
            _client = anthropic.Anthropic(api_key=key)
    return _client

# ─── System prompt ────────────────────────────────────────────────────────────
SYSTEM = """You are a legendary Dungeon Master narrating a dark fantasy RPG adventure.
Keep responses SHORT (2-4 sentences max). Be vivid, dramatic, and immersive.
Never break character. The player is the hero. Make every moment feel epic.
End each exploration message with exactly 3 numbered choices for the player.
Format choices like:
1. [action]
2. [action]
3. [action]"""

COMBAT_SYSTEM = """You are a Dungeon Master narrating a tense RPG combat encounter.
Keep responses to 1-2 sentences. Be dramatic and visceral. Describe the action cinematically."""

# ─── Fallback narratives ───────────────────────────────────────────────────────
EXPLORE_NARRATIVES = [
    ("The shadows shift as you venture deeper. Ancient runes glow faintly on the walls, whispering forgotten secrets.",
     ["Search the runes for clues", "Press deeper into the darkness", "Set up camp and rest"]),
    ("A cold wind carries the scent of blood and sulfur. Something powerful lurks nearby.",
     ["Track the scent carefully", "Ready your weapon and charge forward", "Find higher ground to scout"]),
    ("You discover a crumbling shrine. An offering bowl holds a single flickering flame that defies the wind.",
     ["Make an offering to the shrine", "Take the magical flame", "Pray for divine guidance"]),
    ("The path splits — left leads down a treacherous rocky slope, right through a dense fog.",
     ["Take the rocky slope", "Walk into the fog", "Climb above to see both paths"]),
    ("A wounded traveler slumps against a stone pillar, clutching a blood-soaked map.",
     ["Help the traveler", "Take the map and leave", "Question them about what attacked them"]),
    ("Bones litter the ground — a clear sign of past battles. A rusty chest sits untouched among the remains.",
     ["Open the chest carefully", "Search the bones for clues", "Bypass everything and move on"]),
    ("You hear the echoes of chanting. A ritual circle glows with eldritch energy ahead.",
     ["Interrupt the ritual", "Observe from the shadows", "Absorb the energy for yourself"]),
    ("A hidden trapdoor creaks open beneath your foot, revealing a ladder descending into pitch black.",
     ["Climb down into the darkness", "Toss a torch down first", "Seal the trapdoor and move on"]),
]

COMBAT_HITS = [
    "Your blade finds its mark — the creature staggers!",
    "A powerful strike connects with bone-crushing force!",
    "You unleash a devastating blow that echoes through the chamber!",
    "Swift and precise, your attack draws blood!",
    "With a battle cry, you slam into your enemy!",
]

COMBAT_MISSES = [
    "The creature twists away at the last second — you miss!",
    "Your strike glances off its thick hide!",
    "It parries your blow with surprising speed!",
    "You stumble and your weapon sweeps harmlessly past!",
]

COMBAT_ENEMY_HITS = [
    "The creature lunges and claws rake across your armor!",
    "A brutal strike sends you reeling backward!",
    "You take a vicious hit — pain explodes through your body!",
    "The monster's weapon smashes into you with terrible force!",
]

COMBAT_ENEMY_MISSES = [
    "The creature swings wildly — you dodge just in time!",
    "You sidestep the attack with practiced agility!",
    "Its strike glances off your armor!",
    "You raise your guard and deflect the blow!",
]

VICTORY_LINES = [
    "With a final, decisive blow, {enemy} crumples to the ground. Silence falls.",
    "The {enemy} lets out a dying screech and collapses — you stand victorious!",
    "{enemy} falls at your feet. Another foe defeated, another step toward legend.",
    "You defeat the {enemy} in a brilliant display of skill and courage!",
]

LEVEL_UP_LINES = [
    "⚡ Power surges through your veins — you've grown stronger!",
    "⚡ Battle-hardened and forged by fire — you level up!",
    "⚡ Your mastery deepens. New power awakens within you!",
]


# ─── Public API ───────────────────────────────────────────────────────────────

def generate_exploration(game_state) -> tuple[str, list[str]]:
    """Return (narrative_text, [choice1, choice2, choice3])."""
    client = _get_client()
    if client:
        try:
            prompt = (
                f"The hero {game_state.name} (Level {game_state.level} {game_state.char_class.value}) "
                f"is exploring {game_state.current_location}. Turn {game_state.turn}. "
                f"HP: {game_state._hp}/{game_state.max_hp}. Gold: {game_state.gold}. "
                f"Recent events: {'; '.join(game_state.story_history[-3:]) if game_state.story_history else 'just started the adventure'}. "
                f"Describe what they encounter and give 3 numbered choices."
            )
            resp = client.messages.create(
                model="claude-haiku-4-5-20251001",
                max_tokens=300,
                system=SYSTEM,
                messages=[{"role": "user", "content": prompt}]
            )
            text = resp.content[0].text.strip()
            # Parse choices from the response
            lines = text.split("\n")
            narrative_lines = []
            choices = []
            for line in lines:
                stripped = line.strip()
                if stripped and stripped[0].isdigit() and ". " in stripped[:4]:
                    choices.append(stripped[2:].strip().lstrip(". ").strip())
                elif stripped:
                    narrative_lines.append(stripped)
            narrative = " ".join(narrative_lines)
            if len(choices) >= 3:
                return narrative, choices[:3]
        except Exception:
            pass

    # Fallback
    narrative, choices = random.choice(EXPLORE_NARRATIVES)
    return narrative, choices


def generate_combat_hit(attacker, target, damage: int) -> str:
    """Narrate a hit in combat."""
    client = _get_client()
    if client:
        try:
            prompt = f"{attacker} hits {target} for {damage} damage. One dramatic sentence."
            resp = client.messages.create(
                model="claude-haiku-4-5-20251001",
                max_tokens=80,
                system=COMBAT_SYSTEM,
                messages=[{"role": "user", "content": prompt}]
            )
            return resp.content[0].text.strip()
        except Exception:
            pass
    return random.choice(COMBAT_HITS)


def generate_combat_miss(attacker, target) -> str:
    client = _get_client()
    if client:
        try:
            prompt = f"{attacker} attacks {target} but misses! One dramatic sentence."
            resp = client.messages.create(
                model="claude-haiku-4-5-20251001",
                max_tokens=80,
                system=COMBAT_SYSTEM,
                messages=[{"role": "user", "content": prompt}]
            )
            return resp.content[0].text.strip()
        except Exception:
            pass
    return random.choice(COMBAT_MISSES)


def generate_enemy_attack(enemy_name: str, damage: int, dodged: bool) -> str:
    if dodged:
        return random.choice(COMBAT_ENEMY_MISSES)
    return random.choice(COMBAT_ENEMY_HITS)


def generate_victory(enemy_name: str) -> str:
    return random.choice(VICTORY_LINES).format(enemy=enemy_name)


def generate_level_up_message() -> str:
    return random.choice(LEVEL_UP_LINES)


def generate_death_message(name: str, kills: int) -> str:
    client = _get_client()
    if client:
        try:
            prompt = f"Write a 2-sentence dramatic death scene for the hero {name} who defeated {kills} enemies."
            resp = client.messages.create(
                model="claude-haiku-4-5-20251001",
                max_tokens=100,
                system=COMBAT_SYSTEM,
                messages=[{"role": "user", "content": prompt}]
            )
            return resp.content[0].text.strip()
        except Exception:
            pass
    return (
        f"The darkness claims {name} at last. "
        f"But legends remember the {kills} foes that fell before them..."
    )


def generate_choice_outcome(game_state, choice: str) -> str:
    """Generate narrative outcome for a player's non-combat choice."""
    client = _get_client()
    if client:
        try:
            prompt = (
                f"Hero {game_state.name} (Lvl {game_state.level}) chose: '{choice}'. "
                f"Location: {game_state.current_location}. "
                f"Write 1-2 sentences describing what happens next (no new choices)."
            )
            resp = client.messages.create(
                model="claude-haiku-4-5-20251001",
                max_tokens=120,
                system=COMBAT_SYSTEM,
                messages=[{"role": "user", "content": prompt}]
            )
            return resp.content[0].text.strip()
        except Exception:
            pass
    outcomes = [
        "Your instincts prove sharp — the gamble pays off.",
        "A risky move, but fortune favors the bold today.",
        "The path ahead shifts in unexpected ways...",
        "Your choice echoes through the dungeon's ancient halls.",
        "The world responds to your decision with eerie silence.",
    ]
    return random.choice(outcomes)
