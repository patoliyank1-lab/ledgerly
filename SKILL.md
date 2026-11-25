---
name: finledger-android-builder
description: >
  Build skill for FinLedger — a Kotlin Android personal expense management app.
  Offline-first, Jetpack Compose UI, Room database, Hilt DI.
  Built exclusively via GitHub Actions. No local Android SDK or emulator required.
---

# FinLedger Android Builder Skill

## Context Files (Always Read First)
- [Product Spec](../../../App/Product.md) — Full feature requirements
- [Design System](../../../App/DESIGN.md) — Apple-inspired design tokens
- [Master Plan](PLAN.md) — All ~200 tasks, phased delivery
- [Financial Rules](FINANCIAL_RULES.md) — Critical accounting constraints
- [DB Schema](DB_SCHEMA.md) — Complete Room database schema
- [Design Tokens](DESIGN_TOKENS.md) — DESIGN.md → Android XML mapping

## Project Root
All Android project files live at: `/home/picpu-11/Applications/App/android/`

## Build Method
**GitHub Actions only.** Never try to run `./gradlew` locally.
- Workflow: `.github/workflows/build.yml`
- APK download: GitHub Actions → Artifacts
- Release APK: GitHub Releases (on tag push)

## Architecture Rules

### MVVM + Clean Architecture
```
data/local/     → Room entities, DAOs, AppDatabase
data/repository/ → Repository implementations
domain/usecase/  → One use case class per atomic operation
domain/model/    → Domain-layer data classes
ui/theme/        → Color.kt, Type.kt, Shape.kt (from DESIGN.md)
ui/screens/      → One package per screen, ViewModel co-located
ui/components/   → Shared Compose composables
worker/          → WorkManager workers (reminders, backup)
```

### Naming Conventions
- Entities: `AccountEntity`, `TransactionEntity` (suffix `Entity`)
- DAOs: `AccountDao`, `TransactionDao` (suffix `Dao`)
- ViewModels: `AccountsViewModel` (suffix `ViewModel`)
- Use Cases: `AddExpenseUseCase` (prefix verb + suffix `UseCase`)
- Screens: `AccountsScreen.kt` + `AccountsViewModel.kt` in same package
- Composables: PascalCase function names

### Dependency Injection
- All repositories and use cases: `@Inject constructor` + `@HiltViewModel`
- Room database: `@Singleton` in `DatabaseModule`
- Never pass context into ViewModels directly

## Design System Rules (from DESIGN.md)

### Colors — NEVER use hardcoded hex in Compose code
```kotlin
// CORRECT
MaterialTheme.colorScheme.primary       // #0066cc
MaterialTheme.colorScheme.surface       // #ffffff
MaterialTheme.colorScheme.background    // #f5f5f7

// WRONG
Color(0xFF0066CC)  // ❌ never hardcode in screen files
```

### Typography
- Display/headline: 34sp–56sp, fontWeight 600, letterSpacing negative
- Body: 17sp, fontWeight 400
- Caption: 14sp
- Use `MaterialTheme.typography.*` tokens only

### Shapes
- Pill buttons: `shape = MaterialTheme.shapes.extraLarge` (fully rounded)
- Cards: `shape = MaterialTheme.shapes.large` (18dp corners)
- Utility buttons: `shape = MaterialTheme.shapes.small` (8dp)

### Button Rules
- Primary action: `Button` with blue background + white text + pill shape
- Secondary: `OutlinedButton` with blue border + pill shape
- FAB: round, blue, "+" icon
- Press animation: `scale(0.95f)` on click via `interactionSource`

### Elevation
- Cards: `elevation = 0.dp` (flat — use color contrast for hierarchy)
- FAB: standard Material3 elevation
- Bottom nav: surface tonal elevation

### Alternating Section Pattern
```kotlin
// Light section
Surface(color = MaterialTheme.colorScheme.surface) { ... }
// Dark section  
Surface(color = MaterialTheme.colorScheme.inverseSurface) { ... }
```

## Critical Financial Rules (NEVER VIOLATE)

1. **Shared expense:** If user pays ₹2400 for 4 people, user's expense = ₹600 (their share only), receivables = ₹1800
2. **Transfer:** Account A → Account B = NOT income, NOT expense. Balance changes only.
3. **Settlement received:** Reduces receivable, NOT income.
4. **Settlement paid:** Reduces payable, NOT expense.
5. **Lending:** Creates receivable, NOT an expense.
6. **Borrowing:** Creates liability, NOT income.
7. **Credit card purchase:** Records expense + increases card liability.
8. **Credit card payment:** Reduces liability only, does NOT re-record expense.
9. **Refund:** Links to original expense, reduces spending total.

## Task Execution Pattern

For each task from PLAN.md:
1. Read the relevant section of Product.md for requirements
2. Check DB_SCHEMA.md for entity structure
3. Write entity/DAO → repository → use case → ViewModel → Screen in that order
4. Never skip layers (UI must not query DAOs directly)
5. Add a `// TODO(P2):` comment for Phase 2+ features referenced in Phase 1 screens

## Room Database Rules
- All entities use `@PrimaryKey(autoGenerate = true) val id: Long = 0`
- Timestamps stored as `Long` (epoch milliseconds)
- Soft deletes via `val isDeleted: Boolean = false` column
- Amounts stored as `Long` (paise/smallest unit, never Float/Double)
- `Flow<List<T>>` for all list queries (live updates)
- `suspend fun` for all write operations

## Testing Convention
- Unit test every use case in `test/` directory
- Test financial calculation rules specifically
- No UI tests in Phase 1 (time-box)

