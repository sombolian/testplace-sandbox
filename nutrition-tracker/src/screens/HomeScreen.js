import React, { useState, useCallback } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  RefreshControl,
  StatusBar,
} from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { COLORS, FONTS, RADIUS, SPACING, SHADOWS } from '../utils/theme';
import {
  getMeals,
  getSettings,
  getStreak,
  updateStreak,
  getWater,
  updateMeal,
  deleteMeal,
  getEffectiveDate,
  isGoalMet,
} from '../utils/storage';
import ProgressRing from '../components/ProgressRing';
import MealCard from '../components/MealCard';

export default function HomeScreen({ navigation, route }) {
  const [meals, setMeals] = useState([]);
  const [settings, setSettings] = useState({});
  const [streak, setStreakData] = useState({ current: 0, best: 0 });
  const [water, setWaterData] = useState(0);
  const [refreshing, setRefreshing] = useState(false);
  const [selectedDate, setSelectedDate] = useState(null);

  const viewingDate = selectedDate || route?.params?.date || null;
  const isToday = !viewingDate || viewingDate === getEffectiveDate();

  const loadData = useCallback(async () => {
    const [m, s, st, w] = await Promise.all([
      getMeals(viewingDate),
      getSettings(),
      isToday ? updateStreak() : getStreak(),
      getWater(viewingDate),
    ]);
    setMeals(m);
    setSettings(s);
    setStreakData(st);
    setWaterData(w);
  }, [viewingDate, isToday]);

  useFocusEffect(
    useCallback(() => {
      loadData();
    }, [loadData])
  );

  const onRefresh = async () => {
    setRefreshing(true);
    await loadData();
    setRefreshing(false);
  };

  const totalCalories = meals.reduce((sum, m) => sum + (m.calories || 0), 0);
  const totalProtein = meals.reduce((sum, m) => sum + (m.protein || 0), 0);

  const calGoal = settings.calorieGoal || 2000;
  const proGoal = settings.proteinGoal || 150;
  const waterGoal = settings.waterGoal || 8;

  const calProgress = calGoal > 0 ? totalCalories / calGoal : 0;
  const proProgress = proGoal > 0 ? totalProtein / proGoal : 0;
  const waterProgress = waterGoal > 0 ? water / waterGoal : 0;

  const calMet = isGoalMet(totalCalories, calGoal);
  const proMet = isGoalMet(totalProtein, proGoal);
  const waterMet = isGoalMet(water, waterGoal);

  const handleUpdateMeal = async (mealId, updates) => {
    const updated = await updateMeal(mealId, updates, viewingDate);
    setMeals(updated);
  };

  const handleDeleteMeal = async (mealId) => {
    const updated = await deleteMeal(mealId, viewingDate);
    setMeals(updated);
  };

  const goToPreviousDay = () => {
    const current = viewingDate || getEffectiveDate();
    const d = new Date(current);
    d.setDate(d.getDate() - 1);
    const newDate = d.toISOString().split('T')[0];
    setSelectedDate(newDate);
  };

  const goToNextDay = () => {
    const current = viewingDate || getEffectiveDate();
    const d = new Date(current);
    d.setDate(d.getDate() + 1);
    const newDate = d.toISOString().split('T')[0];
    const today = getEffectiveDate();
    if (newDate <= today) {
      setSelectedDate(newDate === today ? null : newDate);
    }
  };

  const goToToday = () => {
    setSelectedDate(null);
  };

  const formatDate = (dateStr) => {
    if (!dateStr) return 'Today';
    const today = getEffectiveDate();
    if (dateStr === today) return 'Today';
    const yesterday = new Date();
    yesterday.setDate(yesterday.getDate() - 1);
    if (dateStr === yesterday.toISOString().split('T')[0]) return 'Yesterday';
    const d = new Date(dateStr + 'T12:00:00');
    return d.toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric' });
  };

  return (
    <View style={styles.container}>
      <StatusBar barStyle="light-content" backgroundColor={COLORS.background} />

      {/* Header */}
      <View style={styles.header}>
        <View>
          <Text style={styles.headerTitle}>NutriTrack</Text>
          <Text style={styles.headerSubtitle}>
            {isToday ? "Let's hit those goals!" : formatDate(viewingDate)}
          </Text>
        </View>
        <View style={styles.streakBadge}>
          <Text style={styles.streakEmoji}>🔥</Text>
          <Text style={styles.streakCount}>{streak.current}</Text>
          <Text style={styles.streakLabel}>day streak</Text>
        </View>
      </View>

      <ScrollView
        style={styles.scrollView}
        refreshControl={
          <RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={COLORS.primary} />
        }
        showsVerticalScrollIndicator={false}>

        {/* Date Navigator */}
        <View style={styles.dateNav}>
          <TouchableOpacity style={styles.dateNavBtn} onPress={goToPreviousDay}>
            <Text style={styles.dateNavArrow}>‹</Text>
          </TouchableOpacity>
          <TouchableOpacity onPress={goToToday}>
            <Text style={styles.dateNavText}>{formatDate(viewingDate)}</Text>
            {!isToday && <Text style={styles.tapToday}>Tap for today</Text>}
          </TouchableOpacity>
          <TouchableOpacity
            style={[styles.dateNavBtn, isToday && styles.dateNavBtnDisabled]}
            onPress={goToNextDay}
            disabled={isToday}>
            <Text style={[styles.dateNavArrow, isToday && { color: COLORS.textMuted }]}>›</Text>
          </TouchableOpacity>
        </View>

        {/* Progress Rings */}
        <View style={styles.progressSection}>
          <ProgressRing
            progress={calProgress}
            size={130}
            strokeWidth={12}
            color={COLORS.calories}
            label="Calories"
            value={totalCalories}
            unit={`/ ${calGoal}`}
            goal={calGoal}
            showCheck={calMet}
          />
          <ProgressRing
            progress={proProgress}
            size={130}
            strokeWidth={12}
            color={COLORS.protein}
            label="Protein"
            value={`${totalProtein}g`}
            unit={`/ ${proGoal}g`}
            goal={proGoal}
            showCheck={proMet}
          />
        </View>

        {/* Water Progress */}
        <View style={styles.waterSection}>
          <View style={styles.waterHeader}>
            <Text style={styles.sectionTitle}>💧 Water</Text>
            <Text style={styles.waterCount}>
              {water} / {waterGoal} cups {waterMet ? '✓' : ''}
            </Text>
          </View>
          <View style={styles.waterBar}>
            <View
              style={[
                styles.waterFill,
                {
                  width: `${Math.min(waterProgress * 100, 100)}%`,
                  backgroundColor: waterProgress > 1.2 ? COLORS.danger : COLORS.water,
                },
              ]}
            />
          </View>
          <View style={styles.waterDrops}>
            {Array.from({ length: waterGoal }, (_, i) => (
              <Text key={i} style={[styles.waterDrop, i < water && styles.waterDropFilled]}>
                💧
              </Text>
            ))}
          </View>
        </View>

        {/* Quick Stats Bar */}
        <View style={styles.statsBar}>
          <View style={styles.statBox}>
            <Text style={styles.statBoxValue}>{calGoal - totalCalories}</Text>
            <Text style={styles.statBoxLabel}>cal remaining</Text>
          </View>
          <View style={[styles.statBox, styles.statBoxMiddle]}>
            <Text style={styles.statBoxValue}>{proGoal - totalProtein}g</Text>
            <Text style={styles.statBoxLabel}>protein left</Text>
          </View>
          <View style={styles.statBox}>
            <Text style={styles.statBoxValue}>{meals.length}</Text>
            <Text style={styles.statBoxLabel}>meals today</Text>
          </View>
        </View>

        {/* Meals List */}
        <View style={styles.mealsSection}>
          <Text style={styles.sectionTitle}>Today's Meals</Text>
          {meals.length === 0 ? (
            <View style={styles.emptyState}>
              <Text style={styles.emptyEmoji}>🍽️</Text>
              <Text style={styles.emptyText}>No meals logged yet</Text>
              <Text style={styles.emptySubtext}>
                Tap the + button to add your first meal
              </Text>
            </View>
          ) : (
            meals.map((meal) => (
              <MealCard
                key={meal.id}
                meal={meal}
                onUpdate={handleUpdateMeal}
                onDelete={handleDeleteMeal}
              />
            ))
          )}
        </View>

        <View style={{ height: 100 }} />
      </ScrollView>

      {/* FAB */}
      {isToday && (
        <TouchableOpacity
          style={styles.fab}
          onPress={() => navigation.navigate('AddMeal')}>
          <Text style={styles.fabText}>+</Text>
        </TouchableOpacity>
      )}

      {!isToday && (
        <TouchableOpacity
          style={[styles.fab, { backgroundColor: COLORS.secondary }]}
          onPress={() => navigation.navigate('AddMeal', { date: viewingDate })}>
          <Text style={styles.fabText}>+</Text>
        </TouchableOpacity>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: COLORS.background,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: SPACING.lg,
    paddingTop: SPACING.xxl + 10,
    paddingBottom: SPACING.md,
    backgroundColor: COLORS.background,
  },
  headerTitle: {
    ...FONTS.title,
    fontSize: 28,
    color: COLORS.primary,
  },
  headerSubtitle: {
    ...FONTS.small,
    marginTop: 2,
  },
  streakBadge: {
    alignItems: 'center',
    backgroundColor: COLORS.backgroundCard,
    borderRadius: RADIUS.lg,
    paddingHorizontal: SPACING.md,
    paddingVertical: SPACING.sm,
    ...SHADOWS.small,
  },
  streakEmoji: {
    fontSize: 24,
  },
  streakCount: {
    ...FONTS.bold,
    fontSize: 20,
    color: COLORS.streak,
  },
  streakLabel: {
    ...FONTS.tiny,
    fontSize: 8,
  },
  scrollView: {
    flex: 1,
  },
  dateNav: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: SPACING.lg,
    paddingVertical: SPACING.sm,
    marginHorizontal: SPACING.md,
    marginBottom: SPACING.sm,
    backgroundColor: COLORS.backgroundCard,
    borderRadius: RADIUS.md,
  },
  dateNavBtn: {
    width: 40,
    height: 40,
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: RADIUS.full,
    backgroundColor: COLORS.backgroundInput,
  },
  dateNavBtnDisabled: {
    opacity: 0.3,
  },
  dateNavArrow: {
    fontSize: 24,
    color: COLORS.text,
    fontWeight: '700',
  },
  dateNavText: {
    ...FONTS.medium,
    textAlign: 'center',
  },
  tapToday: {
    ...FONTS.tiny,
    textAlign: 'center',
    color: COLORS.primaryLight,
  },
  progressSection: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    paddingHorizontal: SPACING.lg,
    paddingVertical: SPACING.lg,
  },
  waterSection: {
    marginHorizontal: SPACING.md,
    backgroundColor: COLORS.backgroundCard,
    borderRadius: RADIUS.lg,
    padding: SPACING.md,
    marginBottom: SPACING.md,
  },
  waterHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: SPACING.sm,
  },
  waterCount: {
    ...FONTS.medium,
    color: COLORS.water,
  },
  waterBar: {
    height: 8,
    backgroundColor: COLORS.backgroundInput,
    borderRadius: RADIUS.full,
    overflow: 'hidden',
    marginBottom: SPACING.sm,
  },
  waterFill: {
    height: '100%',
    borderRadius: RADIUS.full,
  },
  waterDrops: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 4,
  },
  waterDrop: {
    fontSize: 16,
    opacity: 0.3,
  },
  waterDropFilled: {
    opacity: 1,
  },
  statsBar: {
    flexDirection: 'row',
    marginHorizontal: SPACING.md,
    marginBottom: SPACING.lg,
    backgroundColor: COLORS.backgroundCard,
    borderRadius: RADIUS.lg,
    padding: SPACING.md,
    ...SHADOWS.small,
  },
  statBox: {
    flex: 1,
    alignItems: 'center',
  },
  statBoxMiddle: {
    borderLeftWidth: 1,
    borderRightWidth: 1,
    borderColor: COLORS.border,
  },
  statBoxValue: {
    ...FONTS.bold,
    fontSize: 18,
    color: COLORS.primaryLight,
  },
  statBoxLabel: {
    ...FONTS.tiny,
    marginTop: 2,
  },
  mealsSection: {
    paddingHorizontal: SPACING.md,
  },
  sectionTitle: {
    ...FONTS.subtitle,
    marginBottom: SPACING.md,
    marginLeft: SPACING.xs,
  },
  emptyState: {
    alignItems: 'center',
    paddingVertical: SPACING.xxl,
  },
  emptyEmoji: {
    fontSize: 48,
    marginBottom: SPACING.md,
  },
  emptyText: {
    ...FONTS.medium,
    color: COLORS.textSecondary,
  },
  emptySubtext: {
    ...FONTS.small,
    color: COLORS.textMuted,
    marginTop: SPACING.xs,
  },
  fab: {
    position: 'absolute',
    bottom: 24,
    right: 24,
    width: 60,
    height: 60,
    borderRadius: 30,
    backgroundColor: COLORS.primary,
    alignItems: 'center',
    justifyContent: 'center',
    ...SHADOWS.medium,
    zIndex: 999,
  },
  fabText: {
    fontSize: 30,
    color: COLORS.text,
    fontWeight: '300',
    marginTop: -2,
  },
});
