#!/usr/bin/env node
/**
 * calorie_calc.js - Калькулятор калорий на JavaScript (Node.js CLI)
 */
const fs = require('fs');
const { program } = require('commander');

const ACTIVITY_LEVELS = {
    sedentary: 1.2,
    light: 1.375,
    moderate: 1.55,
    active: 1.725,
    very_active: 1.9,
};
const ACTIVITY_NAMES = {
    sedentary: 'Сидячий образ жизни',
    light: 'Лёгкая активность (1-3 дня/нед)',
    moderate: 'Умеренная активность (3-5 дней/нед)',
    active: 'Высокая активность (6-7 дней/нед)',
    very_active: 'Очень высокая активность (тяжёлая работа/спорт)',
};
const GOALS = ['maintain', 'lose', 'gain'];
const GOAL_NAMES = {
    maintain: 'Поддержание веса',
    lose: 'Похудение (дефицит 15%)',
    gain: 'Набор массы (профицит 15%)',
};
const GENDER_NAMES = { male: 'Мужской', female: 'Женский' };

function bmrMifflin(gender, age, height, weight) {
    if (gender === 'male') {
        return 10 * weight + 6.25 * height - 5 * age + 5;
    } else {
        return 10 * weight + 6.25 * height - 5 * age - 161;
    }
}

function bmrHarris(gender, age, height, weight) {
    if (gender === 'male') {
        return 88.362 + 13.397 * weight + 4.799 * height - 5.677 * age;
    } else {
        return 447.593 + 9.247 * weight + 3.098 * height - 4.330 * age;
    }
}

function calculateTdee(bmr, activity) {
    return bmr * ACTIVITY_LEVELS[activity];
}

function calculateGoalCalories(tdee, goal) {
    if (goal === 'maintain') return tdee;
    if (goal === 'lose') return tdee * 0.85;
    if (goal === 'gain') return tdee * 1.15;
    return tdee;
}

function calculateMacros(calories, goal) {
    let proteinRatio = 0.30, fatRatio = 0.25, carbRatio = 0.45;
    if (goal === 'lose') {
        proteinRatio = 0.35; fatRatio = 0.25; carbRatio = 0.40;
    } else if (goal === 'gain') {
        proteinRatio = 0.30; fatRatio = 0.20; carbRatio = 0.50;
    }
    return {
        protein: (calories * proteinRatio) / 4,
        fat: (calories * fatRatio) / 9,
        carbs: (calories * carbRatio) / 4,
    };
}

function generateReport(params, bmr, tdee, goalCal, macros) {
    const lines = [];
    lines.push('='.repeat(50));
    lines.push('📋 ОТЧЁТ ПО КАЛОРИЯМ');
    lines.push('='.repeat(50));
    lines.push(`Пол: ${GENDER_NAMES[params.gender]}`);
    lines.push(`Возраст: ${params.age} лет`);
    lines.push(`Рост: ${params.height} см`);
    lines.push(`Вес: ${params.weight} кг`);
    lines.push(`Уровень активности: ${ACTIVITY_NAMES[params.activity]}`);
    lines.push(`Цель: ${GOAL_NAMES[params.goal]}`);
    lines.push('-'.repeat(50));
    lines.push(`BMR (базовый метаболизм): ${bmr.toFixed(1)} ккал/день`);
    lines.push(`TDEE (суточная норма): ${tdee.toFixed(1)} ккал/день`);
    lines.push(`Рекомендуемая калорийность для цели: ${goalCal.toFixed(1)} ккал/день`);
    lines.push('-'.repeat(50));
    lines.push('🍽️ РАСПРЕДЕЛЕНИЕ ПО МАКРОНУТРИЕНТАМ:');
    lines.push(`Белки: ${macros.protein.toFixed(1)} г (${((macros.protein*4/goalCal)*100).toFixed(0)}%)`);
    lines.push(`Жиры: ${macros.fat.toFixed(1)} г (${((macros.fat*9/goalCal)*100).toFixed(0)}%)`);
    lines.push(`Углеводы: ${macros.carbs.toFixed(1)} г (${((macros.carbs*4/goalCal)*100).toFixed(0)}%)`);
    lines.push('='.repeat(50));
    return lines.join('\n');
}

program
    .option('-g, --gender <gender>', 'Пол (male/female)')
    .option('-a, --age <age>', 'Возраст (лет)', parseFloat)
    .option('--height <height>', 'Рост (см)')
    .option('--height-ft <feet>', 'Рост (футы)', parseInt)
    .option('--height-in <inches>', 'Рост (дюймы)', parseInt, 0)
    .option('--weight <weight>', 'Вес (кг)')
    .option('--weight-lb <weight>', 'Вес (фунты)')
    .option('--activity <activity>', 'Уровень активности', /^(sedentary|light|moderate|active|very_active)$/i)
    .option('--goal <goal>', 'Цель (maintain/lose/gain)', 'maintain')
    .option('--formula <formula>', 'Формула (msj/hb)', 'msj')
    .option('--output <file>', 'Сохранить отчёт в файл')
    .parse(process.argv);

const opts = program.opts();

if (!opts.gender || !opts.age || !opts.activity) {
    console.error('Укажите --gender, --age, --activity');
    process.exit(1);
}

let height = opts.height;
let weight = opts.weight;
if (opts.heightFt !== undefined) {
    height = opts.heightFt * 30.48 + (opts.heightIn || 0) * 2.54;
}
if (opts.weightLb !== undefined) {
    weight = opts.weightLb * 0.453592;
}
if (height === undefined || weight === undefined) {
    console.error('Укажите рост и вес (метрические или имперские)');
    process.exit(1);
}

const params = {
    gender: opts.gender.toLowerCase(),
    age: opts.age,
    height: height,
    weight: weight,
    activity: opts.activity.toLowerCase(),
    goal: opts.goal.toLowerCase(),
    formula: opts.formula.toLowerCase(),
};

let bmr;
if (params.formula === 'msj') {
    bmr = bmrMifflin(params.gender, params.age, params.height, params.weight);
} else {
    bmr = bmrHarris(params.gender, params.age, params.height, params.weight);
}
const tdee = calculateTdee(bmr, params.activity);
const goalCal = calculateGoalCalories(tdee, params.goal);
const macros = calculateMacros(goalCal, params.goal);

const report = generateReport(params, bmr, tdee, goalCal, macros);
console.log(report);

if (opts.output) {
    fs.writeFileSync(opts.output, report, 'utf8');
    console.log(`Отчёт сохранён в ${opts.output}`);
}
