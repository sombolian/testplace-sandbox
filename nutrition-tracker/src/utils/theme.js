export const COLORS = {
  primary: '#6C5CE7',
  primaryLight: '#A29BFE',
  primaryDark: '#5A4BD1',
  secondary: '#00CEC9',
  secondaryLight: '#81ECEC',
  accent: '#FD79A8',
  accentLight: '#FDCB6E',

  success: '#00B894',
  successLight: '#55EFC4',
  warning: '#FDCB6E',
  warningLight: '#FFEAA7',
  danger: '#E17055',
  dangerLight: '#FAB1A0',

  background: '#0F0F1A',
  backgroundLight: '#1A1A2E',
  backgroundCard: '#16213E',
  backgroundInput: '#1E2A45',

  text: '#FFFFFF',
  textSecondary: '#B2BEC3',
  textMuted: '#636E72',
  textDark: '#2D3436',

  border: '#2D3460',
  borderLight: '#3D4470',

  water: '#74B9FF',
  waterLight: '#A3D8FF',
  protein: '#A29BFE',
  calories: '#FD79A8',
  streak: '#FDCB6E',
};

export const FONTS = {
  regular: { fontSize: 14, color: COLORS.text },
  medium: { fontSize: 16, color: COLORS.text, fontWeight: '500' },
  bold: { fontSize: 16, color: COLORS.text, fontWeight: '700' },
  title: { fontSize: 24, color: COLORS.text, fontWeight: '700' },
  subtitle: { fontSize: 18, color: COLORS.text, fontWeight: '600' },
  small: { fontSize: 12, color: COLORS.textSecondary },
  tiny: { fontSize: 10, color: COLORS.textMuted },
};

export const SHADOWS = {
  small: {
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.25,
    shadowRadius: 3.84,
    elevation: 3,
  },
  medium: {
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.3,
    shadowRadius: 4.65,
    elevation: 6,
  },
};

export const SPACING = {
  xs: 4,
  sm: 8,
  md: 16,
  lg: 24,
  xl: 32,
  xxl: 48,
};

export const RADIUS = {
  sm: 8,
  md: 12,
  lg: 16,
  xl: 24,
  full: 999,
};

export const FOOD_EMOJIS = {
  chicken: '🍗',
  rice: '🍚',
  salad: '🥗',
  tuna: '🐟',
  egg: '🥚',
  bread: '🍞',
  pasta: '🍝',
  steak: '🥩',
  fish: '🐟',
  milk: '🥛',
  cheese: '🧀',
  fruit: '🍎',
  vegetable: '🥦',
  water: '💧',
  coffee: '☕',
  shake: '🥤',
  protein: '💪',
  default: '🍽️',
};

export function getFoodEmoji(name) {
  if (!name) return FOOD_EMOJIS.default;
  const lower = name.toLowerCase();
  for (const [key, emoji] of Object.entries(FOOD_EMOJIS)) {
    if (lower.includes(key)) return emoji;
  }
  return FOOD_EMOJIS.default;
}
