import AsyncStorage from '@react-native-async-storage/async-storage';

const KEYS = {
  MEALS: 'nutrition_meals',
  SETTINGS: 'nutrition_settings',
  STREAK: 'nutrition_streak',
  WATER: 'nutrition_water',
};

// Day boundary: 5 AM - if before 5 AM, count as previous day
export function getEffectiveDate(date = new Date()) {
  const d = new Date(date);
  if (d.getHours() < 5) {
    d.setDate(d.getDate() - 1);
  }
  return d.toISOString().split('T')[0]; // YYYY-MM-DD
}

export function getEffectiveDateObj(date = new Date()) {
  const d = new Date(date);
  if (d.getHours() < 5) {
    d.setDate(d.getDate() - 1);
  }
  d.setHours(12, 0, 0, 0);
  return d;
}

// === MEALS ===
export async function getMeals(date) {
  try {
    const all = await getAllMeals();
    const dateKey = date || getEffectiveDate();
    return all[dateKey] || [];
  } catch (e) {
    console.error('getMeals error:', e);
    return [];
  }
}

export async function getAllMeals() {
  try {
    const data = await AsyncStorage.getItem(KEYS.MEALS);
    return data ? JSON.parse(data) : {};
  } catch (e) {
    console.error('getAllMeals error:', e);
    return {};
  }
}

export async function addMeal(meal, date) {
  try {
    const all = await getAllMeals();
    const dateKey = date || getEffectiveDate();
    if (!all[dateKey]) all[dateKey] = [];
    all[dateKey].push({
      id: Date.now().toString() + Math.random().toString(36).substr(2, 9),
      ...meal,
      timestamp: new Date().toISOString(),
    });
    await AsyncStorage.setItem(KEYS.MEALS, JSON.stringify(all));
    return all[dateKey];
  } catch (e) {
    console.error('addMeal error:', e);
    return [];
  }
}

export async function updateMeal(mealId, updates, date) {
  try {
    const all = await getAllMeals();
    const dateKey = date || getEffectiveDate();
    if (all[dateKey]) {
      all[dateKey] = all[dateKey].map((m) =>
        m.id === mealId ? { ...m, ...updates } : m
      );
      await AsyncStorage.setItem(KEYS.MEALS, JSON.stringify(all));
    }
    return all[dateKey] || [];
  } catch (e) {
    console.error('updateMeal error:', e);
    return [];
  }
}

export async function deleteMeal(mealId, date) {
  try {
    const all = await getAllMeals();
    const dateKey = date || getEffectiveDate();
    if (all[dateKey]) {
      all[dateKey] = all[dateKey].filter((m) => m.id !== mealId);
      await AsyncStorage.setItem(KEYS.MEALS, JSON.stringify(all));
    }
    return all[dateKey] || [];
  } catch (e) {
    console.error('deleteMeal error:', e);
    return [];
  }
}

// === WATER ===
export async function getWater(date) {
  try {
    const data = await AsyncStorage.getItem(KEYS.WATER);
    const all = data ? JSON.parse(data) : {};
    const dateKey = date || getEffectiveDate();
    return all[dateKey] || 0;
  } catch (e) {
    return 0;
  }
}

export async function addWater(cups, date) {
  try {
    const data = await AsyncStorage.getItem(KEYS.WATER);
    const all = data ? JSON.parse(data) : {};
    const dateKey = date || getEffectiveDate();
    all[dateKey] = (all[dateKey] || 0) + cups;
    await AsyncStorage.setItem(KEYS.WATER, JSON.stringify(all));
    return all[dateKey];
  } catch (e) {
    return 0;
  }
}

export async function setWater(cups, date) {
  try {
    const data = await AsyncStorage.getItem(KEYS.WATER);
    const all = data ? JSON.parse(data) : {};
    const dateKey = date || getEffectiveDate();
    all[dateKey] = cups;
    await AsyncStorage.setItem(KEYS.WATER, JSON.stringify(all));
    return all[dateKey];
  } catch (e) {
    return 0;
  }
}

export async function getAllWater() {
  try {
    const data = await AsyncStorage.getItem(KEYS.WATER);
    return data ? JSON.parse(data) : {};
  } catch (e) {
    return {};
  }
}

// === SETTINGS ===
const DEFAULT_SETTINGS = {
  geminiApiKey: '',
  coachModel: 'gemini-2.0-flash',
  analyzeModel: 'gemini-2.0-flash',
  calorieGoal: 2000,
  proteinGoal: 150,
  waterGoal: 8,
  overallGoal: '',
  gymFrequency: '',
};

export async function getSettings() {
  try {
    const data = await AsyncStorage.getItem(KEYS.SETTINGS);
    return data ? { ...DEFAULT_SETTINGS, ...JSON.parse(data) } : DEFAULT_SETTINGS;
  } catch (e) {
    return DEFAULT_SETTINGS;
  }
}

export async function saveSettings(settings) {
  try {
    const current = await getSettings();
    const updated = { ...current, ...settings };
    await AsyncStorage.setItem(KEYS.SETTINGS, JSON.stringify(updated));
    return updated;
  } catch (e) {
    return settings;
  }
}

// === STREAK ===
export async function getStreak() {
  try {
    const data = await AsyncStorage.getItem(KEYS.STREAK);
    return data ? JSON.parse(data) : { current: 0, lastDate: null, best: 0 };
  } catch (e) {
    return { current: 0, lastDate: null, best: 0 };
  }
}

export async function updateStreak() {
  try {
    const streak = await getStreak();
    const today = getEffectiveDate();

    if (streak.lastDate === today) return streak;

    const yesterday = new Date();
    yesterday.setDate(yesterday.getDate() - 1);
    const yesterdayKey = getEffectiveDate(yesterday);

    let newStreak;
    if (streak.lastDate === yesterdayKey) {
      newStreak = {
        current: streak.current + 1,
        lastDate: today,
        best: Math.max(streak.best, streak.current + 1),
      };
    } else if (!streak.lastDate) {
      newStreak = { current: 1, lastDate: today, best: Math.max(streak.best, 1) };
    } else {
      newStreak = { current: 1, lastDate: today, best: streak.best };
    }

    await AsyncStorage.setItem(KEYS.STREAK, JSON.stringify(newStreak));
    return newStreak;
  } catch (e) {
    return { current: 0, lastDate: null, best: 0 };
  }
}

// === DAY RATING ===
export function getDayRating(consumed, goal) {
  if (goal <= 0) return 'full';
  const ratio = consumed / goal;
  if (ratio >= 0.8 && ratio <= 1.2) return 'full';
  if (ratio >= 0.6 || (ratio > 1.2 && ratio <= 1.4)) return 'half';
  return 'broken';
}

// For the 90% completion check
export function isGoalMet(consumed, goal) {
  return consumed >= goal * 0.9;
}
