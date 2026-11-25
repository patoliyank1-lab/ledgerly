# FinLedger Android Project

Personal expense management app — offline-first Kotlin Android app.

## 📁 Project Structure
```
App/
├── Product.md           ← Full feature specification
├── DESIGN.md            ← Apple-inspired design token system
├── SKILL.md             ← Agent build instructions
├── FINANCIAL_RULES.md   ← Critical accounting rules
├── DESIGN_TOKENS.md     ← DESIGN.md → Android code mapping
├── DB_SCHEMA.md         ← Complete Room database schema
└── android/             ← Kotlin Android project root
    ├── .github/
    │   └── workflows/
    │       └── build.yml  ← GitHub Actions CI/CD
    ├── app/
    │   └── src/main/java/com/finledger/
    │       ├── data/
    │       ├── domain/
    │       ├── ui/
    │       └── worker/
    └── gradlew
```

## 🚀 How to Build
**No local Android Studio required!**

1. Push code to GitHub
2. GitHub Actions automatically builds the APK
3. Download APK from: GitHub → Actions → latest run → Artifacts
4. Install on Android phone

## 📋 Development Plan
See [PLAN.md](../brain/d3b977e4-b2ad-4c68-a821-c476d4904141/PLAN.md) for all ~200 tasks across 6 phases.

## 🏗️ Tech Stack
- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Database:** Room (SQLite, fully offline)
- **DI:** Hilt
- **Navigation:** Compose Navigation
- **Background:** WorkManager
- **Charts:** Vico
- **Build:** GitHub Actions

## 📱 Delivery Phases
| Phase | Scope |
|---|---|
| P1 | Personal finance foundation |
| P2 | Friends & shared expenses |
| P3 | Bills, loans, budgets |
| P4 | Analytics & investments |
| P5 | Import/export/backup |
| P6 | Smart features |

