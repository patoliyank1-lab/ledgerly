# FinLedger — Financial Accounting Rules

> [!CAUTION]
> These rules apply across the entire application. Any code that violates these rules produces incorrect financial data for the user.

## 1. Expense vs Shared Expense

| Scenario | User pays | User's personal expense | Receivable created |
|---|---|---|---|
| Personal purchase | ₹500 | ₹500 | None |
| Splits equally with 1 friend | ₹1000 | ₹500 | ₹500 from friend |
| Pays for 4 people (equal) | ₹2400 | ₹600 | ₹1800 (3 × ₹600) |

**Rule:** `user_expense = user_split_share`, never `user_expense = total_paid`

## 2. Transfers

```
Account A balance: -₹500
Account B balance: +₹500
Income total:       unchanged
Expense total:      unchanged
```

A transfer between the user's own accounts is NEVER income or expense.

## 3. Settlements

**Receiving settlement from person:**
- Reduces receivable from that person
- Does NOT create income
- Does NOT affect expense/income totals

**Paying settlement to person:**
- Reduces payable to that person
- Does NOT create expense
- Does NOT affect expense/income totals

## 4. Lending

```
User lends ₹5000 to Rahul:
  Receivable (Rahul): +₹5000
  Cash account:       -₹5000
  Expense total:      unchanged
  Income total:       unchanged
```

Lending creates a **receivable asset**, not an expense.

## 5. Borrowing

```
User borrows ₹10000 from bank:
  Liability (bank):   +₹10000
  Cash account:       +₹10000
  Expense total:      unchanged
  Income total:       unchanged
```

Borrowing creates a **liability**, not income.

## 6. Loan Repayments

```
User repays ₹2000 to bank:
  Liability (bank):   -₹2000
  Cash account:       -₹2000
  Expense total:      unchanged (principal only)
```

Interest/fees on repayment = expense. Principal repayment = liability reduction only.

## 7. Credit Cards

**Purchase on credit card:**
```
  Expense:              +₹3000 (category: Shopping)
  Credit card liability: +₹3000
  Cash accounts:         unchanged
```

**Paying credit card bill:**
```
  Cash account:          -₹3000
  Credit card liability:  -₹3000
  Expense total:          unchanged (already recorded above)
```

**NEVER double-count:** The ₹3000 expense is recorded ONCE at purchase time only.

## 8. Investments

**Buying investment:**
```
  Cash account:         -₹10000
  Investment asset:     +₹10000
  Expense total:        unchanged
```

**Selling investment (realized gain):**
```
  Investment asset:     -₹10000 (cost basis)
  Cash account:         +₹12000
  Realized gain:        +₹2000 (income type: investment gain)
```

Investment fees/brokerage at time of purchase/sale = expense (category: Investment Fee).

## 9. Refunds

A refund reduces the original expense category spending.
- If linked to original transaction: reduces that transaction's amount
- If standalone: negative expense entry in same category
- A refund is NEVER ordinary income

## 10. Balance Sign Convention

| Value | Meaning |
|---|---|
| `personBalance > 0` | Person owes the USER |
| `personBalance < 0` | USER owes that person |
| `personBalance = 0` | Settled — nothing outstanding |

This convention must be consistent in ALL screens, labels, and calculations.
Never rely on color alone — always show explicit text ("owes you" / "you owe").

## 11. Net Worth Calculation

```
Net Worth = Total Assets - Total Liabilities

Assets:
  + Cash accounts
  + Bank savings accounts
  + Investment portfolio value
  + Fixed deposits (maturity value)
  + Receivables (money owed to user)
  + Other manually tracked assets

Liabilities:
  + Credit card outstanding balances
  + Outstanding loans (borrowed)
  + Payables (money user owes others)

Note: Receivables are NOT the same as available cash.
      Dashboard must clearly separate "cash available" from "total receivables".
```

## 12. Person-wise Balance (Consolidated)

For each person, the balance must include:
- Shared expense splits (from all groups + events)
- Personal lending to that person
- Personal borrowing from that person
- Settlements recorded (received or paid)
- Adjustments and corrections

One person = one consolidated balance. Never split the same person across multiple unrelated balances.

## 13. Auditability

- Every edit, deletion, reversal, or correction must create an audit log entry
- Financial totals must be recalculated from scratch from stored transactions
- The user must be able to drill down from any total to its supporting transactions
- Soft-delete only: never permanently erase financial records (use `isDeleted = true`)

