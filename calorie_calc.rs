// calorie_calc.rs - Калькулятор калорий на Rust (CLI)
use clap::{Arg, App};
use std::collections::HashMap;
use std::fs::File;
use std::io::Write;

lazy_static::lazy_static! {
    static ref ACTIVITY_LEVELS: HashMap<&'static str, f64> = {
        let mut m = HashMap::new();
        m.insert("sedentary", 1.2);
        m.insert("light", 1.375);
        m.insert("moderate", 1.55);
        m.insert("active", 1.725);
        m.insert("very_active", 1.9);
        m
    };
    static ref ACTIVITY_NAMES: HashMap<&'static str, &'static str> = {
        let mut m = HashMap::new();
        m.insert("sedentary", "Сидячий образ жизни");
        m.insert("light", "Лёгкая активность (1-3 дня/нед)");
        m.insert("moderate", "Умеренная активность (3-5 дней/нед)");
        m.insert("active", "Высокая активность (6-7 дней/нед)");
        m.insert("very_active", "Очень высокая активность (тяжёлая работа/спорт)");
        m
    };
    static ref GOAL_NAMES: HashMap<&'static str, &'static str> = {
        let mut m = HashMap::new();
        m.insert("maintain", "Поддержание веса");
        m.insert("lose", "Похудение (дефицит 15%)");
        m.insert("gain", "Набор массы (профицит 15%)");
        m
    };
    static ref GENDER_NAMES: HashMap<&'static str, &'static str> = {
        let mut m = HashMap::new();
        m.insert("male", "Мужской");
        m.insert("female", "Женский");
        m
    };
}

fn bmr_mifflin(gender: &str, age: f64, height: f64, weight: f64) -> f64 {
    if gender == "male" {
        10.0 * weight + 6.25 * height - 5.0 * age + 5.0
    } else {
        10.0 * weight + 6.25 * height - 5.0 * age - 161.0
    }
}

fn bmr_harris(gender: &str, age: f64, height: f64, weight: f64) -> f64 {
    if gender == "male" {
        88.362 + 13.397 * weight + 4.799 * height - 5.677 * age
    } else {
        447.593 + 9.247 * weight + 3.098 * height - 4.330 * age
    }
}

fn calculate_tdee(bmr: f64, activity: &str) -> f64 {
    bmr * ACTIVITY_LEVELS[activity]
}

fn calculate_goal_calories(tdee: f64, goal: &str) -> f64 {
    match goal {
        "maintain" => tdee,
        "lose" => tdee * 0.85,
        "gain" => tdee * 1.15,
        _ => tdee,
    }
}

fn calculate_macros(calories: f64, goal: &str) -> (f64, f64, f64) {
    let (protein_ratio, fat_ratio, carb_ratio) = match goal {
        "lose" => (0.35, 0.25, 0.40),
        "gain" => (0.30, 0.20, 0.50),
        _ => (0.30, 0.25, 0.45),
    };
    let protein = (calories * protein_ratio) / 4.0;
    let fat = (calories * fat_ratio) / 9.0;
    let carbs = (calories * carb_ratio) / 4.0;
    (protein, fat, carbs)
}

fn generate_report(params: &HashMap<String, String>, bmr: f64, tdee: f64, goal_cal: f64, protein: f64, fat: f64, carbs: f64) -> String {
    let mut lines = Vec::new();
    lines.push("=".repeat(50));
    lines.push("📋 ОТЧЁТ ПО КАЛОРИЯМ".to_string());
    lines.push("=".repeat(50));
    lines.push(format!("Пол: {}", GENDER_NAMES[params["gender"].as_str()]));
    lines.push(format!("Возраст: {} лет", params["age"]));
    lines.push(format!("Рост: {} см", params["height"]));
    lines.push(format!("Вес: {} кг", params["weight"]));
    lines.push(format!("Уровень активности: {}", ACTIVITY_NAMES[params["activity"].as_str()]));
    lines.push(format!("Цель: {}", GOAL_NAMES[params["goal"].as_str()]));
    lines.push("-".repeat(50));
    lines.push(format!("BMR (базовый метаболизм): {:.1} ккал/день", bmr));
    lines.push(format!("TDEE (суточная норма): {:.1} ккал/день", tdee));
    lines.push(format!("Рекомендуемая калорийность для цели: {:.1} ккал/день", goal_cal));
    lines.push("-".repeat(50));
    lines.push("🍽️ РАСПРЕДЕЛЕНИЕ ПО МАКРОНУТРИЕНТАМ:".to_string());
    lines.push(format!("Белки: {:.1} г ({:.0}%)", protein, (protein*4.0/goal_cal)*100.0));
    lines.push(format!("Жиры: {:.1} г ({:.0}%)", fat, (fat*9.0/goal_cal)*100.0));
    lines.push(format!("Углеводы: {:.1} г ({:.0}%)", carbs, (carbs*4.0/goal_cal)*100.0));
    lines.push("=".repeat(50));
    lines.join("\n")
}

fn main() {
    let matches = App::new("Calorie Calculator")
        .arg(Arg::with_name("gender").short("g").long("gender").takes_value(true).required(true).possible_values(&["male", "female"]))
        .arg(Arg::with_name("age").short("a").long("age").takes_value(true).required(true))
        .arg(Arg::with_name("height").long("height").takes_value(true).help("Рост (см)"))
        .arg(Arg::with_name("height-ft").long("height-ft").takes_value(true).help("Рост (футы)"))
        .arg(Arg::with_name("height-in").long("height-in").takes_value(true).default_value("0").help("Рост (дюймы)"))
        .arg(Arg::with_name("weight").long("weight").takes_value(true).help("Вес (кг)"))
        .arg(Arg::with_name("weight-lb").long("weight-lb").takes_value(true).help("Вес (фунты)"))
        .arg(Arg::with_name("activity").short("l").long("activity").takes_value(true).required(true).possible_values(&["sedentary", "light", "moderate", "active", "very_active"]))
        .arg(Arg::with_name("goal").short("o").long("goal").takes_value(true).default_value("maintain").possible_values(&["maintain", "lose", "gain"]))
        .arg(Arg::with_name("formula").short("f").long("formula").takes_value(true).default_value("msj").possible_values(&["msj", "hb"]))
        .arg(Arg::with_name("output").short("p").long("output").takes_value(true).help("Сохранить отчёт в файл"))
        .get_matches();

    let gender = matches.value_of("gender").unwrap();
    let age: f64 = matches.value_of("age").unwrap().parse().unwrap();
    let activity = matches.value_of("activity").unwrap();
    let goal = matches.value_of("goal").unwrap();
    let formula = matches.value_of("formula").unwrap();

    let height = if let Some(h) = matches.value_of("height") {
        h.parse().unwrap()
    } else {
        let ft: f64 = matches.value_of("height-ft").unwrap().parse().unwrap();
        let inc: f64 = matches.value_of("height-in").unwrap().parse().unwrap();
        ft * 30.48 + inc * 2.54
    };
    let weight = if let Some(w) = matches.value_of("weight") {
        w.parse().unwrap()
    } else {
        let lb: f64 = matches.value_of("weight-lb").unwrap().parse().unwrap();
        lb * 0.453592
    };

    let bmr = if formula == "msj" {
        bmr_mifflin(gender, age, height, weight)
    } else {
        bmr_harris(gender, age, height, weight)
    };
    let tdee = calculate_tdee(bmr, activity);
    let goal_cal = calculate_goal_calories(tdee, goal);
    let (protein, fat, carbs) = calculate_macros(goal_cal, goal);

    let mut params = HashMap::new();
    params.insert("gender".to_string(), gender.to_string());
    params.insert("age".to_string(), format!("{:.0}", age));
    params.insert("height".to_string(), format!("{:.1}", height));
    params.insert("weight".to_string(), format!("{:.1}", weight));
    params.insert("activity".to_string(), activity.to_string());
    params.insert("goal".to_string(), goal.to_string());

    let report = generate_report(&params, bmr, tdee, goal_cal, protein, fat, carbs);
    println!("{}", report);

    if let Some(output) = matches.value_of("output") {
        let mut file = File::create(output).unwrap();
        file.write_all(report.as_bytes()).unwrap();
        println!("Отчёт сохранён в {}", output);
    }
}
