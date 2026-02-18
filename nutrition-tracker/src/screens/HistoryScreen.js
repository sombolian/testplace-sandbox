import React, { useState, useCallback } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
} from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { COLORS, FONTS, RADIUS, SPACING, SHADOWS } from '../utils/theme';
import { getAllMeals, getAllWater, getSettings, getEffectiveDate, getDayRating } from '../utils/storage';

const MONTHS = [
  'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December'
];

const DAYS = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];

function getStarEmoji(rating) {
  if (rating === 'full') return '⭐';
  if (rating === 'half') return '✨';
  return '💔';
}

function getStarColor(rating) {
  if (rating === 'full') return COLORS.streak;
  if (rating === 'half') return COLORS.textSecondary;
  return COLORS.danger;
}

export default function HistoryScreen({ navigation }) {
  const [currentMonth, setCurrentMonth] = useState(new Date());
  const [allMeals, setAllMeals] = useState({});
  const [allWater, setAllWater] = useState({});
  const [settings, setSettingsData] = useState({});
  const [selectedDay, setSelectedDay] = useState(null);

  useFocusEffect(
    useCallback(() => {
      loadData();
    }, [])
  );

  const loadData = async () => {
    const [meals, water, s] = await Promise.all([
      getAllMeals(),
      getAllWater(),
      getSettings(),
    ]);
    setAllMeals(meals);
    setAllWater(water);
    setSettingsData(s);
  };

  const year = currentMonth.getFullYear();
  const month = currentMonth.getMonth();

  const daysInMonth = new Date(year, month + 1, 0).getDate();
  const firstDayOfWeek = new Date(year, month, 1).getDay();
  const today = getEffectiveDate();

  const prevMonth = () => {
    setCurrentMonth(new Date(year, month - 1, 1));
    setSelectedDay(null);
  };

  const nextMonth = () => {
    const next = new Date(year, month + 1, 1);
    if (next <= new Date()) {
      setCurrentMonth(next);
      setSelectedDay(null);
    }
  };

  const getDayData = (day) => {
    const dateStr = `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
    const meals = allMeals[dateStr] || [];
    const water = allWater[dateStr] || 0;
    const totalCal = meals.reduce((sum, m) => sum + (m.calories || 0), 0);
    const totalPro = meals.reduce((sum, m) => sum + (m.protein || 0), 0);

    const calRating = getDayRating(totalCal, settings.calorieGoal || 2000);
    const proRating = getDayRating(totalPro, settings.proteinGoal || 150);

    // Combined rating: both need to pass
    let rating = 'none';
    if (meals.length > 0) {
      if (calRating === 'full' && proRating === 'full') rating = 'full';
      else if (calRating === 'broken' || proRating === 'broken') rating = 'broken';
      else rating = 'half';
    }

    return { dateStr, meals, water, totalCal, totalPro, rating };
  };

  const calendarDays = [];
  for (let i = 0; i < firstDayOfWeek; i++) {
    calendarDays.push(null);
  }
  for (let i = 1; i <= daysInMonth; i++) {
    calendarDays.push(i);
  }

  const selectedDayData = selectedDay ? getDayData(selectedDay) : null;

  return (
    <View style={styles.container}>
      {/* Header */}
      <View style={styles.header}>
        <Text style={styles.headerTitle}>📅 History</Text>
        <Text style={styles.headerSubtitle}>Track your progress</Text>
      </View>

      <ScrollView showsVerticalScrollIndicator={false}>
        {/* Month Navigation */}
        <View style={styles.monthNav}>
          <TouchableOpacity onPress={prevMonth} style={styles.monthNavBtn}>
            <Text style={styles.monthNavArrow}>‹</Text>
          </TouchableOpacity>
          <Text style={styles.monthTitle}>
            {MONTHS[month]} {year}
          </Text>
          <TouchableOpacity onPress={nextMonth} style={styles.monthNavBtn}>
            <Text style={styles.monthNavArrow}>›</Text>
          </TouchableOpacity>
        </View>

        {/* Legend */}
        <View style={styles.legend}>
          <View style={styles.legendItem}>
            <Text style={styles.legendStar}>⭐</Text>
            <Text style={styles.legendText}>Goal met</Text>
          </View>
          <View style={styles.legendItem}>
            <Text style={styles.legendStar}>✨</Text>
            <Text style={styles.legendText}>Close</Text>
          </View>
          <View style={styles.legendItem}>
            <Text style={styles.legendStar}>💔</Text>
            <Text style={styles.legendText}>Missed</Text>
          </View>
        </View>

        {/* Calendar Grid */}
        <View style={styles.calendar}>
          {/* Day headers */}
          <View style={styles.dayHeaders}>
            {DAYS.map((d) => (
              <Text key={d} style={styles.dayHeader}>{d}</Text>
            ))}
          </View>

          {/* Calendar cells */}
          <View style={styles.calendarGrid}>
            {calendarDays.map((day, index) => {
              if (!day) {
                return <View key={`empty-${index}`} style={styles.calendarCell} />;
              }

              const data = getDayData(day);
              const isToday = data.dateStr === today;
              const isFuture = data.dateStr > today;
              const isSelected = selectedDay === day;

              return (
                <TouchableOpacity
                  key={day}
                  style={[
                    styles.calendarCell,
                    isToday && styles.todayCell,
                    isSelected && styles.selectedCell,
                    isFuture && styles.futureCell,
                  ]}
                  onPress={() => !isFuture && setSelectedDay(day)}
                  disabled={isFuture}>
                  <Text style={[
                    styles.calendarDay,
                    isToday && styles.todayText,
                    isFuture && styles.futureText,
                  ]}>
                    {day}
                  </Text>
                  {data.rating !== 'none' && !isFuture && (
                    <Text style={styles.cellStar}>{getStarEmoji(data.rating)}</Text>
                  )}
                </TouchableOpacity>
              );
            })}
          </View>
        </View>

        {/* Selected Day Details */}
        {selectedDayData && (
          <View style={styles.dayDetail}>
            <View style={styles.dayDetailHeader}>
              <Text style={styles.dayDetailTitle}>
                {selectedDayData.dateStr === today ? 'Today' : selectedDayData.dateStr}
              </Text>
              {selectedDayData.rating !== 'none' && (
                <Text style={styles.dayDetailStar}>
                  {getStarEmoji(selectedDayData.rating)}
                </Text>
              )}
            </View>

            <View style={styles.dayDetailStats}>
              <View style={styles.dayDetailStat}>
                <Text style={styles.dayDetailValue}>{selectedDayData.totalCal}</Text>
                <Text style={styles.dayDetailLabel}>/ {settings.calorieGoal || 2000} cal</Text>
              </View>
              <View style={styles.dayDetailStat}>
                <Text style={[styles.dayDetailValue, { color: COLORS.protein }]}>
                  {selectedDayData.totalPro}g
                </Text>
                <Text style={styles.dayDetailLabel}>/ {settings.proteinGoal || 150}g protein</Text>
              </View>
              <View style={styles.dayDetailStat}>
                <Text style={[styles.dayDetailValue, { color: COLORS.water }]}>
                  {selectedDayData.water}
                </Text>
                <Text style={styles.dayDetailLabel}>/ {settings.waterGoal || 8} water</Text>
              </View>
            </View>

            <Text style={styles.dayDetailMealsTitle}>
              Meals ({selectedDayData.meals.length})
            </Text>
            {selectedDayData.meals.map((meal) => (
              <View key={meal.id} style={styles.dayMealItem}>
                <Text style={styles.dayMealName}>{meal.name}</Text>
                <Text style={styles.dayMealStats}>
                  {meal.calories} cal • {meal.protein}g protein
                </Text>
              </View>
            ))}
            {selectedDayData.meals.length === 0 && (
              <Text style={styles.noMeals}>No meals logged</Text>
            )}

            {/* Navigate to this day */}
            <TouchableOpacity
              style={styles.viewDayBtn}
              onPress={() => {
                navigation.navigate('Home', { date: selectedDayData.dateStr });
              }}>
              <Text style={styles.viewDayBtnText}>View Full Day →</Text>
            </TouchableOpacity>
          </View>
        )}

        {/* Monthly Stats */}
        <View style={styles.monthlyStats}>
          <Text style={styles.monthlyTitle}>Monthly Summary</Text>
          {(() => {
            let fullDays = 0, halfDays = 0, brokenDays = 0, totalDays = 0;
            for (let i = 1; i <= daysInMonth; i++) {
              const data = getDayData(i);
              if (data.rating !== 'none') {
                totalDays++;
                if (data.rating === 'full') fullDays++;
                else if (data.rating === 'half') halfDays++;
                else brokenDays++;
              }
            }
            return (
              <View style={styles.monthlyStatsRow}>
                <View style={styles.monthlyStat}>
                  <Text style={styles.monthlyStatEmoji}>⭐</Text>
                  <Text style={styles.monthlyStatValue}>{fullDays}</Text>
                  <Text style={styles.monthlyStatLabel}>Perfect</Text>
                </View>
                <View style={styles.monthlyStat}>
                  <Text style={styles.monthlyStatEmoji}>✨</Text>
                  <Text style={styles.monthlyStatValue}>{halfDays}</Text>
                  <Text style={styles.monthlyStatLabel}>Close</Text>
                </View>
                <View style={styles.monthlyStat}>
                  <Text style={styles.monthlyStatEmoji}>💔</Text>
                  <Text style={styles.monthlyStatValue}>{brokenDays}</Text>
                  <Text style={styles.monthlyStatLabel}>Missed</Text>
                </View>
                <View style={styles.monthlyStat}>
                  <Text style={styles.monthlyStatEmoji}>📊</Text>
                  <Text style={styles.monthlyStatValue}>{totalDays}</Text>
                  <Text style={styles.monthlyStatLabel}>Logged</Text>
                </View>
              </View>
            );
          })()}
        </View>

        <View style={{ height: 100 }} />
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: COLORS.background,
  },
  header: {
    paddingTop: SPACING.xxl + 10,
    paddingHorizontal: SPACING.lg,
    paddingBottom: SPACING.md,
    backgroundColor: COLORS.background,
  },
  headerTitle: {
    ...FONTS.title,
    fontSize: 22,
  },
  headerSubtitle: {
    ...FONTS.small,
    color: COLORS.textSecondary,
    marginTop: 2,
  },
  monthNav: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: SPACING.lg,
    marginVertical: SPACING.md,
  },
  monthNavBtn: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: COLORS.backgroundCard,
    alignItems: 'center',
    justifyContent: 'center',
  },
  monthNavArrow: {
    fontSize: 24,
    color: COLORS.text,
    fontWeight: '700',
  },
  monthTitle: {
    ...FONTS.subtitle,
    fontSize: 20,
  },
  legend: {
    flexDirection: 'row',
    justifyContent: 'center',
    gap: SPACING.lg,
    marginBottom: SPACING.md,
  },
  legendItem: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
  },
  legendStar: {
    fontSize: 14,
  },
  legendText: {
    ...FONTS.tiny,
    color: COLORS.textSecondary,
  },
  calendar: {
    marginHorizontal: SPACING.md,
    backgroundColor: COLORS.backgroundCard,
    borderRadius: RADIUS.lg,
    padding: SPACING.md,
    ...SHADOWS.small,
  },
  dayHeaders: {
    flexDirection: 'row',
    marginBottom: SPACING.sm,
  },
  dayHeader: {
    flex: 1,
    textAlign: 'center',
    ...FONTS.tiny,
    color: COLORS.textMuted,
    fontWeight: '600',
  },
  calendarGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
  },
  calendarCell: {
    width: `${100 / 7}%`,
    aspectRatio: 1,
    alignItems: 'center',
    justifyContent: 'center',
    padding: 2,
  },
  todayCell: {
    backgroundColor: COLORS.primary + '30',
    borderRadius: RADIUS.sm,
  },
  selectedCell: {
    backgroundColor: COLORS.primary + '50',
    borderRadius: RADIUS.sm,
    borderWidth: 1,
    borderColor: COLORS.primary,
  },
  futureCell: {
    opacity: 0.3,
  },
  calendarDay: {
    ...FONTS.small,
    color: COLORS.text,
    fontWeight: '500',
    fontSize: 13,
  },
  todayText: {
    color: COLORS.primaryLight,
    fontWeight: '700',
  },
  futureText: {
    color: COLORS.textMuted,
  },
  cellStar: {
    fontSize: 10,
    marginTop: -1,
  },
  dayDetail: {
    marginHorizontal: SPACING.md,
    marginTop: SPACING.md,
    backgroundColor: COLORS.backgroundCard,
    borderRadius: RADIUS.lg,
    padding: SPACING.lg,
    ...SHADOWS.small,
  },
  dayDetailHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: SPACING.md,
  },
  dayDetailTitle: {
    ...FONTS.subtitle,
  },
  dayDetailStar: {
    fontSize: 24,
  },
  dayDetailStats: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    backgroundColor: COLORS.backgroundInput,
    borderRadius: RADIUS.md,
    padding: SPACING.md,
    marginBottom: SPACING.md,
  },
  dayDetailStat: {
    alignItems: 'center',
  },
  dayDetailValue: {
    ...FONTS.bold,
    fontSize: 18,
    color: COLORS.calories,
  },
  dayDetailLabel: {
    ...FONTS.tiny,
    marginTop: 2,
  },
  dayDetailMealsTitle: {
    ...FONTS.medium,
    marginBottom: SPACING.sm,
    color: COLORS.textSecondary,
  },
  dayMealItem: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: SPACING.sm,
    borderBottomWidth: 1,
    borderBottomColor: COLORS.border,
  },
  dayMealName: {
    ...FONTS.regular,
    flex: 1,
  },
  dayMealStats: {
    ...FONTS.small,
    color: COLORS.textSecondary,
  },
  noMeals: {
    ...FONTS.small,
    color: COLORS.textMuted,
    textAlign: 'center',
    paddingVertical: SPACING.md,
  },
  viewDayBtn: {
    marginTop: SPACING.md,
    backgroundColor: COLORS.primary + '30',
    borderRadius: RADIUS.md,
    padding: SPACING.md,
    alignItems: 'center',
  },
  viewDayBtnText: {
    ...FONTS.medium,
    color: COLORS.primaryLight,
  },
  monthlyStats: {
    marginHorizontal: SPACING.md,
    marginTop: SPACING.md,
    backgroundColor: COLORS.backgroundCard,
    borderRadius: RADIUS.lg,
    padding: SPACING.lg,
    ...SHADOWS.small,
  },
  monthlyTitle: {
    ...FONTS.subtitle,
    marginBottom: SPACING.md,
    textAlign: 'center',
  },
  monthlyStatsRow: {
    flexDirection: 'row',
    justifyContent: 'space-around',
  },
  monthlyStat: {
    alignItems: 'center',
  },
  monthlyStatEmoji: {
    fontSize: 24,
    marginBottom: 4,
  },
  monthlyStatValue: {
    ...FONTS.bold,
    fontSize: 20,
  },
  monthlyStatLabel: {
    ...FONTS.tiny,
    marginTop: 2,
  },
});
