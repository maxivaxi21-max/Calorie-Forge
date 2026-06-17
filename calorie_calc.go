// calorie_calc.go - Калькулятор калорий на Go (CLI)
package main

import (
	"flag"
	"fmt"
	"os"
	"strconv"
	"strings"
)

var activityLevels = map[string]float64{
	"sedentary":  1.2,
	"light":      1.375,
	"moderate":   1.55,
	"active":     1.725,
	"very_active": 1.9,
}
var activityNames = map[string]string{
	"sedentary":   "Сидячий образ жизни",
	"light":       "Лёгкая активность (1-3 дня/нед)",
	"moderate":    "Умеренная активность (3-5 дней/нед)",
	"active":      "Высокая активность (6-7 дней/нед)",
	"very_active": "Очень высокая активность (тяжёлая работа/спорт)",
}
var goalNames = map[string]string{
	"maintain": "Поддержание веса",
	"lose":     "Похудение (дефицит 15%)",
	"gain":     "Набор массы (профицит 15%)",
}
var genderNames = map[string]string{
	"male":   "Мужской",
	"female": "Женский",
}

func bmrMifflin(gender string, age, height, weight float64) float64 {
	if gender == "male" {
		return 10*weight + 6.25*height - 5*age + 5
	}
	return 10*weight + 6.25*height - 5*age - 161
}

func bmrHarris(gender string, age, height, weight float64) float64 {
	if gender == "male" {
		return 88.362 + 13.397*weight + 4.799*height - 5.677*age
	}
	return 447.593 + 9.247*weight + 3.098*height - 4.330*age
}

func calculateTdee(bmr float64, activity string) float64 {
	return bmr * activityLevels[activity]
}

func calculateGoalCalories(tdee float64, goal string) float64 {
	switch goal {
	case "maintain":
		return tdee
	case "lose":
		return tdee * 0.85
	case "gain":
		return tdee * 1.15
	default:
		return tdee
	}
}

func calculateMacros(calories float64, goal string) (protein, fat, carbs float64) {
	proteinRatio, fatRatio, carbRatio := 0.30, 0.25, 0.45
	switch goal {
	case "lose":
		proteinRatio, fatRatio, carbRatio = 0.35, 0.25, 0.40
	case "gain":
		proteinRatio, fatRatio, carbRatio = 0.30, 0.20, 0.50
	}
	protein = (calories * proteinRatio) / 4
	fat = (calories * fatRatio) / 9
	carbs = (calories * carbRatio) / 4
	return
}

func generateReport(gender, age, height, weight, activity, goal, formula string, bmr, tdee, goalCal, protein, fat, carbs float64) string {
	lines := []string{}
	lines = append(lines, strings.Repeat("=", 50))
	lines = append(lines, "📋 ОТЧЁТ ПО КАЛОРИЯМ")
	lines = append(lines, strings.Repeat("=", 50))
	lines = append(lines, fmt.Sprintf("Пол: %s", genderNames[gender]))
	lines = append(lines, fmt.Sprintf("Возраст: %.0f лет", age))
	lines = append(lines, fmt.Sprintf("Рост: %.1f см", height))
	lines = append(lines, fmt.Sprintf("Вес: %.1f кг", weight))
	lines = append(lines, fmt.Sprintf("Уровень активности: %s", activityNames[activity]))
	lines = append(lines, fmt.Sprintf("Цель: %s", goalNames[goal]))
	lines = append(lines, strings.Repeat("-", 50))
	lines = append(lines, fmt.Sprintf("BMR (базовый метаболизм): %.1f ккал/день", bmr))
	lines = append(lines, fmt.Sprintf("TDEE (суточная норма): %.1f ккал/день", tdee))
	lines = append(lines, fmt.Sprintf("Рекомендуемая калорийность для цели: %.1f ккал/день", goalCal))
	lines = append(lines, strings.Repeat("-", 50))
	lines = append(lines, "🍽️ РАСПРЕДЕЛЕНИЕ ПО МАКРОНУТРИЕНТАМ:")
	lines = append(lines, fmt.Sprintf("Белки: %.1f г (%.0f%%)", protein, (protein*4/goalCal)*100))
	lines = append(lines, fmt.Sprintf("Жиры: %.1f г (%.0f%%)", fat, (fat*9/goalCal)*100))
	lines = append(lines, fmt.Sprintf("Углеводы: %.1f г (%.0f%%)", carbs, (carbs*4/goalCal)*100))
	lines = append(lines, strings.Repeat("=", 50))
	return strings.Join(lines, "\n")
}

func main() {
	var (
		gender    string
		age       float64
		height    float64
		heightFt  int
		heightIn  int
		weight    float64
		weightLb  float64
		activity  string
		goal      string
		formula   string
		output    string
		useMetric bool
	)
	flag.StringVar(&gender, "gender", "", "Пол (male/female)")
	flag.Float64Var(&age, "age", 0, "Возраст (лет)")
	flag.Float64Var(&height, "height", 0, "Рост (см)")
	flag.IntVar(&heightFt, "height-ft", 0, "Рост (футы)")
	flag.IntVar(&heightIn, "height-in", 0, "Рост (дюймы)")
	flag.Float64Var(&weight, "weight", 0, "Вес (кг)")
	flag.Float64Var(&weightLb, "weight-lb", 0, "Вес (фунты)")
	flag.StringVar(&activity, "activity", "", "Уровень активности (sedentary,light,moderate,active,very_active)")
	flag.StringVar(&goal, "goal", "maintain", "Цель (maintain,lose,gain)")
	flag.StringVar(&formula, "formula", "msj", "Формула (msj/hb)")
	flag.StringVar(&output, "output", "", "Сохранить отчёт в файл")
	flag.Parse()

	if gender == "" || age == 0 || activity == "" {
		fmt.Println("Укажите --gender, --age, --activity")
		return
	}
	if height == 0 && (heightFt == 0 && heightIn == 0) {
		fmt.Println("Укажите рост (--height или --height-ft/--height-in)")
		return
	}
	if weight == 0 && weightLb == 0 {
		fmt.Println("Укажите вес (--weight или --weight-lb)")
		return
	}

	if height == 0 {
		height = float64(heightFt)*30.48 + float64(heightIn)*2.54
	}
	if weight == 0 {
		weight = weightLb * 0.453592
	}

	var bmr float64
	if formula == "msj" {
		bmr = bmrMifflin(gender, age, height, weight)
	} else {
		bmr = bmrHarris(gender, age, height, weight)
	}
	tdee := calculateTdee(bmr, activity)
	goalCal := calculateGoalCalories(tdee, goal)
	protein, fat, carbs := calculateMacros(goalCal, goal)

	report := generateReport(gender, age, height, weight, activity, goal, formula, bmr, tdee, goalCal, protein, fat, carbs)
	fmt.Println(report)

	if output != "" {
		os.WriteFile(output, []byte(report), 0644)
		fmt.Printf("Отчёт сохранён в %s\n", output)
	}
}
