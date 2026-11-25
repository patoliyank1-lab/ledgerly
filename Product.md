# Product.md --- Personal Expense Management App

## 1. Product Overview

### Product vision

A personal finance management application that helps a person
understand, organize, and manage all money-related activities in one
place. It covers personal income and expenses, bank and cash accounts,
friends and shared expenses, groups and events, person-wise balances,
borrowing and lending, investments, bills, budgets, analytics, and data
portability.

The product should feel like a reliable personal financial manager and a
clear financial ledger---not merely an expense-entry tool.

### Main goals

-   Record where money comes from, where it goes, and where it is held.
-   Understand personal spending across categories, merchants,
    locations, purchases, and events.
-   Manage shared expenses with friends without losing track of who paid
    and who owes whom.
-   Show one consolidated financial balance for each person across all
    groups, events, loans, and repayments.
-   Track multiple accounts, including cash, bank accounts, wallets,
    cards, and investments.
-   Track money lent to others and money borrowed from others or
    financial platforms.
-   Understand assets, liabilities, cash flow, savings, and net worth.
-   Analyze financial activity and create useful reports.
-   Import, export, back up, and restore data with clear validation and
    error reporting.

### Product principles

1.  **One reliable financial record:** A transaction should be recorded
    once and reflected consistently in all relevant views.
2.  **Clear personal impact:** Shared payments must not be mistaken for
    the user's personal expense in full.
3.  **One balance per person:** A person's overall balance must include
    all relevant groups, events, loans, and repayments.
4.  **No double counting:** Transfers, repayments, refunds, and
    investment movements must be classified correctly.
5.  **Transparent calculations:** Users should be able to understand why
    a balance or report has a particular value.
6.  **Privacy and control:** Personal financial information remains
    private unless the user deliberately shares selected information.
7.  **Flexible data management:** Users can correct records and move
    their data in and out of the application.

## 2. Users and Main Use Cases

### Primary user

An individual who wants to manage personal finances and shared financial
activities with friends.

### Main use cases

-   Record a personal purchase, bill, income, or cash transaction.
-   Track balances across several bank accounts, wallets, cards, and
    cash.
-   Create a group for friends and create events within that group.
-   Record an expense paid by one or more people and split it using
    different rules.
-   Find out how much each person owes the user and how much the user
    owes each person.
-   Record lending, borrowing, partial repayments, and full repayments.
-   Track investments and other assets.
-   Set budgets and financial goals.
-   Review financial performance and export statements or reports.
-   Import transactions from supported files and resolve invalid or
    duplicate records.

## 3. Main Navigation and Modules

1.  Dashboard
2.  Transactions
3.  Accounts & Wallets
4.  Friends & Contacts
5.  Groups
6.  Events & Shared Expenses
7.  Person-wise Balances & Settlements
8.  Borrowing & Lending
9.  Investments & Assets
10. Bills & Subscriptions
11. Budgets & Financial Goals
12. Analytics & Reports
13. Import, Export & Backup
14. Notifications & Reminders
15. Financial Calendar
16. Document Vault
17. Settings & Personalization

## 4. Dashboard & Financial Overview

### Core features

-   Total available money.
-   Cash balance.
-   Bank account balances.
-   Digital wallet balances.
-   Credit card outstanding amount and available credit, where tracked.
-   Monthly income.
-   Monthly expenses.
-   Monthly savings.
-   Money people owe the user.
-   Money the user owes others.
-   Total lending and borrowing.
-   Investment value.
-   Assets and liabilities.
-   Net worth.
-   Recent transactions.
-   Upcoming bills and repayments.
-   Pending group and event settlements.
-   Budget progress.
-   Quick actions to add an expense, income, transfer, lending,
    borrowing, or settlement.

### Additional features

-   Financial health overview.
-   Monthly spending comparison.
-   Savings progress.
-   Financial goals progress.
-   Unusual spending alerts.
-   Customizable dashboard sections.

Dashboard totals must clearly distinguish available cash, other assets,
receivables, and liabilities. Do not present them as one interchangeable
balance.

## 5. Transaction Management

### Transaction types

-   Expense
-   Income
-   Transfer between the user's own accounts
-   Refund or returned purchase
-   Lending
-   Borrowing
-   Repayment received
-   Repayment made
-   Investment purchase or sale
-   Account adjustment, with an audit trail

### Transaction details

-   Amount and currency.
-   Transaction type.
-   Date and time.
-   Category and subcategory.
-   Account used or affected.
-   Merchant, payee, or source.
-   Location.
-   Payment method.
-   Description and notes.
-   Tags.
-   Receipt, invoice, or other attachments.
-   Personal or shared classification.
-   Optional group and event association.
-   Person or people involved.
-   Split details, where applicable.
-   Status, such as pending, completed, or cancelled.

### Transaction actions

-   Add, view, edit, and delete.
-   Search and filter.
-   Sort by date, amount, category, account, person, or type.
-   Bulk update and bulk delete, with confirmation.
-   Duplicate transaction detection.
-   Transaction history and change history.
-   Attach multiple files.
-   Link related transactions.

### Additional features

-   Receipt scanning and automatic detail extraction.
-   Suggested categories.
-   Item-level purchase details.
-   Separate discounts, taxes, tips, and fees.
-   Warranty and return-deadline tracking.
-   Recurring transaction creation.

## 6. Income Management

### Income sources

-   Salary.
-   Freelance or contract work.
-   Business income.
-   Rental income.
-   Interest and dividends.
-   Gifts.
-   Refunds.
-   Other income.

### Features

-   Add, edit, and delete income.
-   Income categories and sources.
-   Deposit account.
-   Date, amount, notes, and attachments.
-   Recurring income.
-   Expected versus received income.
-   Salary payment tracking.
-   Monthly and yearly income reports.
-   Tax-related income classification.

A refund should normally be linked to the original expense or treated as
an expense reduction, rather than incorrectly counted as ordinary earned
income.

## 7. Accounts & Wallet Management

### Supported account types

-   Cash and physical wallets.
-   Savings bank accounts.
-   Current bank accounts.
-   Digital wallets.
-   UPI transaction tracking.
-   Debit cards.
-   Credit cards.
-   Fixed deposits.
-   Investment and brokerage accounts.
-   Other assets or manually tracked accounts.

### Account information

-   Account name and type.
-   Bank, provider, or institution.
-   Masked account identifier.
-   Currency.
-   Opening balance and opening date.
-   Current calculated balance.
-   Account status.
-   Notes and attachments.

### Features

-   Add, edit, archive, and view accounts.
-   Account-wise transaction history.
-   Transfers between own accounts.
-   Account balance history.
-   Account reconciliation.
-   Statement import.
-   Low-balance alerts.
-   Cash withdrawal tracking.
-   Credit card billing cycles.
-   Credit limit and outstanding amount.
-   Minimum payment and due-date tracking.
-   Account-wise reports.

### Account rules

-   A transfer between two accounts owned by the user is not income or
    expense.
-   A credit card purchase creates an expense and a card liability.
-   Paying a credit card bill settles the liability; it must not count
    the purchase expense a second time.
-   Investment accounts and other assets must be shown separately from
    spendable cash.
-   Account balances should be explainable from the opening balance and
    recorded activity.

## 8. Friends & Contact Management

### Contact profile

-   Name and profile image.
-   Contact details, if provided.
-   Relationship or label.
-   Notes.
-   Active or archived status.

### Financial features

-   Complete transaction history with the person.
-   Total amount the person owes the user.
-   Total amount the user owes the person.
-   Net balance.
-   Group-wise and event-wise activity.
-   Lending and borrowing history.
-   Repayment history.
-   Settlement history.
-   Payment reminders.
-   Contact-wise financial statement.
-   Export an individual person's statement.

### Important rule

The same person must be represented consistently across personal
transactions, groups, events, loans, and repayments. Avoid creating
separate unrelated balances for the same person.

## 9. Group Management

### Group features

-   Create, edit, archive, and close groups.
-   Group name, image, and description.
-   Add, invite, or remove members.
-   Member roles and permissions.
-   Group activity history.
-   Group expenses and contributions.
-   Group balances.
-   Group settlements.
-   Group reports.
-   Group transaction history.
-   Member-wise spending summary.
-   Group documents and attachments.
-   Group spending limits.
-   Group announcements.
-   Leave group and group closure.

### Privacy and participation

The product should support personal-only tracking as well as
collaborative groups. In personal-only mode, the user records other
people's shares without requiring them to join. In collaborative mode,
invited members can participate according to their permissions. Personal
accounts and private transactions must not become visible to group
members.

## 10. Event Management & Shared Expenses

An event is a specific occasion, such as a trip, dinner, birthday party,
shopping trip, or celebration. A group can contain multiple events. An
event may also be created independently when needed.

### Event details

-   Event name and description.
-   Date and duration.
-   Location.
-   Linked group, if any.
-   Participants.
-   Event budget.
-   Event categories.
-   Attachments and notes.
-   Open, completed, or archived status.

### Shared expense features

-   Record who paid.
-   Support multiple contributors for one expense.
-   Add item-level details.
-   Split equally.
-   Split by custom amount.
-   Split by percentage.
-   Split by shares or weights.
-   Split by item.
-   Exclude selected participants.
-   Record a participant's share separately from the amount paid.
-   Track event-wise balances.
-   Record partial and full settlements.
-   Generate an event summary.
-   Close an event and calculate remaining balances.

### Critical calculation rule

If the user pays ₹2,400 for four people and each person's share is
₹600: - The user's account decreases by ₹2,400. - The user's personal
expense is ₹600. - The other participants collectively owe the user
₹1,800. - The full ₹2,400 must not be counted as the user's personal
spending.

The event's total spending can still be shown as ₹2,400 in event
reports.

## 11. Person-wise Balances & Settlements

This is a core module and must provide one consolidated view of the
user's financial relationship with every person.

### Main summary

-   Total money to receive.
-   Total money to pay.
-   Net receivable or payable.
-   Person-wise balances.
-   Pending and overdue amounts.
-   Balances by group and event.
-   Balances from personal lending and borrowing.
-   Settlement history.

### Individual person ledger

For each person, show: - Opening balance, if applicable. - Shared
expenses and the person's share. - Amount paid by the person. - Amount
paid by the user on the person's behalf. - Money lent and borrowed. -
Repayments received and made. - Adjustments, refunds, and corrections. -
Settlements. - Current outstanding balance. - Transaction dates and
descriptions. - Supporting payment evidence.

### Settlement features

-   Record money received.
-   Record money paid.
-   Partial or full settlement.
-   Settlement date and payment account.
-   Payment method and reference.
-   Receipt or proof attachment.
-   Notes.
-   Pending, confirmed, disputed, or cancelled status where applicable.
-   Settlement correction with an audit trail.
-   Reminders for pending amounts.
-   Aging report showing how long balances have been outstanding.
-   Export a person-wise statement.

### Netting and settlement simplification

If the person owes the user ₹500 and the user owes that person ₹200,
show the net position as that person owing the user ₹300. Preserve the
original transactions so the calculation remains transparent.

For groups, optionally suggest a settlement plan that reduces the number
of payments while preserving each member's final balance. The user must
review and confirm suggested payments.

### Balance sign convention

Use one consistent convention throughout the application: - Positive
balance: the person owes the user. - Negative balance: the user owes the
person. - Zero balance: nothing is outstanding.

Clearly label the direction in the interface; do not rely on color or
plus/minus signs alone.

## 12. Borrowing & Lending

### Lending

-   Record money lent to a person or organization.
-   Principal amount.
-   Date given.
-   Expected repayment date.
-   Interest terms, if applicable.
-   Repayment schedule.
-   Partial repayments.
-   Outstanding principal and interest.
-   Attach agreements and proof.
-   Loan status and closure.

### Borrowing

-   Record money borrowed from a friend, bank, or platform.
-   Lender or platform.
-   Principal amount.
-   Interest and fees.
-   Start date and due date.
-   Duration.
-   EMI or repayment schedule.
-   Partial repayments.
-   Outstanding balance.
-   Agreements and payment evidence.
-   Loan status and closure.

### Additional features

-   Interest calculation.
-   EMI schedule.
-   Late payment tracking.
-   Maturity and due-date reminders.
-   Loan summary and history.
-   Guarantor details, where relevant.
-   Loan agreement storage.

### Accounting rules

-   Lending creates a receivable; it is not an expense by itself.
-   Borrowing creates a liability; it is not income.
-   Repayments reduce the receivable or liability.
-   Interest and fees should be classified separately.
-   A repayment must not be counted as new income or a new expense for
    the principal amount.

## 13. Investments & Assets

### Trackable investment types

-   Stocks.
-   Mutual funds.
-   SIPs.
-   Fixed deposits.
-   Gold.
-   Real estate.
-   Bonds and other instruments.
-   Cryptocurrency records, if the user chooses to track them.
-   Other manually tracked assets.

### Features

-   Investment account and platform.
-   Asset or instrument name.
-   Buy and sell transactions.
-   Quantity and price.
-   Transaction date.
-   Fees, brokerage, and taxes.
-   Average purchase price.
-   Current valuation.
-   Realized profit or loss.
-   Unrealized profit or loss.
-   Dividends and interest.
-   Portfolio allocation.
-   Investment history.
-   Maturity reminders.
-   Manual valuation updates.

Live market prices and external financial integrations are optional
future enhancements. Manual entry and file import should remain
available.

## 14. Bills & Subscriptions

### Bill types

-   Electricity.
-   Water.
-   Internet.
-   Mobile.
-   Rent.
-   Insurance.
-   Credit card bills.
-   EMIs.
-   OTT subscriptions.
-   Software subscriptions.
-   Other recurring payments.

### Features

-   Bill name and provider.
-   Expected amount.
-   Due date.
-   Frequency.
-   Payment account.
-   Paid or unpaid status.
-   Payment history.
-   Attach bill or receipt.
-   Reminders.
-   Recurring expected transactions.
-   Late payment alerts.

### Additional features

-   Subscription cancellation reminders.
-   Price increase tracking.
-   Annual recurring expense summary.
-   Upcoming payment calendar.

## 15. Budgets & Financial Goals

### Budget features

-   Monthly and yearly budgets.
-   Custom date-range budgets.
-   Category and subcategory budgets.
-   Account, group, and event budgets.
-   Spending limits.
-   Amount spent and amount remaining.
-   Budget progress.
-   Alerts when approaching or exceeding a limit.
-   Compare budget with actual spending.
-   Rollover unused budget, optionally.

### Goal features

-   Savings goals.
-   Emergency fund.
-   Travel goals.
-   Purchase goals.
-   Education or other personal goals.
-   Target amount and target date.
-   Contributions and withdrawals.
-   Goal progress.
-   Goal history.
-   Completion status.
-   Forecasted completion date.

## 16. Analytics & Reports

### Expense analytics

-   Daily, weekly, monthly, and yearly spending.
-   Category and subcategory analysis.
-   Merchant and location analysis.
-   Account-wise spending.
-   Payment-method analysis.
-   Personal versus shared expenses.
-   Group and event spending.
-   Top merchants and categories.
-   Spending frequency.
-   Period-over-period comparison.

### Income and savings analytics

-   Income by source.
-   Income trends.
-   Savings rate.
-   Income versus expenses.
-   Expected versus received income.

### People and group analytics

-   Person-wise receivables and payables.
-   Group-wise balances.
-   Event-wise spending.
-   Settlement history.
-   Outstanding balance aging.
-   Amount lent and borrowed by person.

### Overall financial reports

-   Cash flow statement.
-   Assets and liabilities.
-   Net worth history.
-   Investment performance.
-   Budget versus actual.
-   Recurring payment summary.
-   Tax-year summary.
-   Custom date-range reports.

### Report controls

-   Date range.
-   Account.
-   Category.
-   Person.
-   Group.
-   Event.
-   Transaction type.
-   Currency.
-   Export selected report.

## 17. Import, Export & Backup

### Import formats

-   CSV.
-   Excel.
-   Supported bank statements.
-   Other supported transaction files.

### Export formats

-   CSV.
-   Excel.
-   PDF reports.
-   Full JSON data backup.

### Import workflow

1.  Select and upload a file.
2.  Identify the file format.
3.  Preview the file.
4.  Map file columns to application fields.
5.  Validate dates, amounts, currencies, accounts, and required fields.
6.  Detect exact and possible duplicate transactions.
7.  Show valid, invalid, and duplicate rows.
8.  Let the user review the import.
9.  Import according to the selected error policy.
10. Show a final import report.

### Error handling

-   Identify the row and field causing an error.
-   Explain the error in simple language.
-   Allow correction before import.
-   Support valid-row import when partial import is selected.
-   Allow failed rows to be downloaded.
-   Allow failed rows to be corrected and retried.
-   Keep an import history.
-   Provide rollback or reversal for an import batch where safe.
-   Prevent accidental duplicate imports.

### Backup and restore

-   Manual backup.
-   Scheduled backup.
-   Full data export.
-   Restore from backup.
-   Backup history.
-   Restore preview and validation.

## 18. Notifications & Reminders

-   Upcoming bill reminders.
-   Loan repayment reminders.
-   Lending collection reminders.
-   Group settlement reminders.
-   Event reminders.
-   Budget alerts.
-   Low-balance alerts.
-   Credit card due-date reminders.
-   Investment maturity reminders.
-   Recurring payment reminders.
-   Goal progress notifications.
-   Unusual spending alerts.
-   Daily spending summary.
-   Weekly and monthly financial summaries.
-   Custom schedules and notification preferences.

Notifications should be configurable so users can control which
reminders they receive and how often.

## 19. Financial Calendar

A combined calendar showing: - Expenses. - Income. - Bills and
subscriptions. - Credit card due dates. - Loan EMIs and repayments. -
Money expected from others. - Money owed to others. - Group and event
dates. - Investment maturity dates. - Savings goal milestones.

Allow users to switch between calendar and agenda views and filter by
activity type.

## 20. Financial Document Vault

Store and organize: - Receipts. - Invoices. - Bank statements. - Loan
agreements. - Repayment proofs. - Insurance documents. - Investment
statements. - Tax documents. - Warranty documents. - Other financial
files.

Documents may be linked to transactions, accounts, people, groups,
events, or loans. Users should be able to search and filter documents.

## 21. Settings & Personalization

-   Personal profile.
-   Profile picture.
-   Primary currency.
-   Multiple currencies.
-   Date and number formats.
-   Language.
-   Theme.
-   Personal financial year.
-   Custom categories.
-   Custom tags.
-   Custom payment methods.
-   Notification preferences.
-   Privacy settings.
-   Security settings.
-   PIN or biometric lock where supported.
-   Data backup settings.
-   Archived records.
-   Trash and recovery.
-   Activity history.
-   Custom dashboard sections.
-   Custom transaction fields.

## 22. Additional Recommended Features

### Smart receipt scanner

Extract merchant, date, line items, taxes, and total from a receipt. Let
the user review and correct extracted information before saving.

### Financial assistant

Allow natural-language questions such as: - How much did I spend on food
this month? - Who owes me money? - How much do I owe Rahul? - What were
my biggest expenses last month? - How much did I save this year?

Answers must be based on recorded data, show the relevant date range,
and allow the user to inspect supporting transactions.

### Net worth tracker

Calculate net worth using tracked assets and liabilities. Show
historical changes and explain included values. Avoid treating
receivables as available cash.

### Unusual expense detection

Identify spending that differs significantly from the user's history and
let the user review the transactions behind the alert.

### Multi-currency travel expenses

Record transactions in the original currency, retain the exchange rate
and converted amount, and use the user's selected primary currency for
consolidated reports.

### Family finance

Optionally support shared household expenses while keeping each person's
private accounts and transactions private.

### Financial health overview

Show useful indicators such as savings rate, budget adherence, debt
obligations, emergency fund progress, and overdue receivables. Explain
how each indicator is calculated.

## 23. Important Financial Rules

These rules apply across the whole product.

### Expense and shared expense

For an expense paid entirely by the user on behalf of several people: -
Record the full amount leaving the user's payment account. - Record only
the user's own share as the user's personal expense. - Record other
participants' shares as receivables from those participants. - Show the
full amount in the relevant group or event spending report.

### Transfers

A transfer between the user's own accounts changes account balances but
is neither income nor expense.

### Settlements

Receiving money from a person reduces that person's outstanding balance.
It is not new income when it is a repayment of an existing amount owed.
Paying someone reduces the user's outstanding payable. It is not a new
expense when it settles an existing recorded amount.

### Lending and borrowing

Lending creates a receivable. Borrowing creates a liability. Principal
repayments reduce those balances. Interest and fees are classified
separately.

### Credit cards

A purchase is recorded as an expense and increases the credit card
liability. Paying the card bill reduces the liability and must not
duplicate the original expense.

### Investments

Buying an investment moves value from a cash account into an investment
asset. It is not automatically an expense. Investment fees and taxes
should be recorded separately. Realized and unrealized gains must be
distinguished.

### Refunds

A refund should be linked to the original expense where possible and
reduce the relevant spending total.

### Person-wise balances

Maintain one consolidated balance per person across groups, events,
loans, and other recorded financial relationships. Preserve the
underlying transactions so the balance can always be explained.

### Corrections and auditability

Edits, deletions, reversals, and settlement corrections should preserve
an activity history. Financial totals should be recalculated
consistently after a correction.

## 24. Suggested Product Delivery Priorities

### Phase 1 --- Personal finance foundation

-   Personal profile and settings.
-   Dashboard.
-   Categories and tags.
-   Accounts and wallets.
-   Income and expense transactions.
-   Account transfers.
-   Basic transaction search and reports.

### Phase 2 --- Friends and shared expenses

-   Friends and contacts.
-   Groups and members.
-   Events and participants.
-   Shared expense entry.
-   Flexible splitting.
-   Person-wise consolidated balances.
-   Settlements and payment history.

### Phase 3 --- Debt and recurring commitments

-   Borrowing and lending.
-   Partial repayments.
-   Bills and subscriptions.
-   Reminders.
-   Budgets and financial goals.

### Phase 4 --- Financial insights

-   Detailed analytics.
-   Cash flow.
-   Assets and liabilities.
-   Net worth history.
-   Investment tracking.
-   Financial calendar.

### Phase 5 --- Data management

-   CSV and Excel import.
-   Bank statement import.
-   Validation and duplicate detection.
-   Export reports.
-   Full backup and restore.
-   Import error reports and retry.

### Phase 6 --- Advanced capabilities

-   Receipt scanning.
-   Smart categorization.
-   Financial assistant.
-   Unusual expense detection.
-   Forecasting.
-   Family finance.
-   External account integrations where supported.

## 25. Product Acceptance Checklist

The product should not be considered ready until these key user journeys
work correctly.

### Personal transactions

-   [ ] Add, edit, view, and delete an expense.
-   [ ] Add and edit income.
-   [ ] Transfer money between own accounts without changing income or
    expense totals.
-   [ ] Attach and view receipts.
-   [ ] Search and filter transaction history.

### Accounts

-   [ ] Create cash, bank, wallet, and card accounts.
-   [ ] Set opening balances.
-   [ ] Verify balances after income, expenses, and transfers.
-   [ ] Reconcile an account with a statement.
-   [ ] Correctly handle credit card purchases and payments.

### Groups and events

-   [ ] Create a group and add members.
-   [ ] Create an event within a group.
-   [ ] Add an expense paid by one person.
-   [ ] Add an expense paid by multiple people.
-   [ ] Split equally, by custom amount, percentage, shares, and items.
-   [ ] Exclude a participant.
-   [ ] Verify event totals and individual shares.

### Person-wise balances

-   [ ] Consolidate a person's balances across multiple groups and
    events.
-   [ ] Include personal lending and borrowing.
-   [ ] Show both money to receive and money to pay.
-   [ ] Apply partial and full settlements.
-   [ ] Verify net balances after settlements.
-   [ ] Preserve the original transaction history.
-   [ ] Export a person's statement.

### Borrowing and lending

-   [ ] Record money lent and borrowed.
-   [ ] Record partial repayments.
-   [ ] Calculate outstanding principal.
-   [ ] Handle interest and fees separately.
-   [ ] Close a fully repaid loan.

### Budgets, bills, and investments

-   [ ] Create a budget and track progress.
-   [ ] Create recurring bills and reminders.
-   [ ] Track investment purchases and sales.
-   [ ] Distinguish investment value from spendable cash.
-   [ ] Calculate assets, liabilities, and net worth consistently.

### Analytics and data management

-   [ ] Verify category, account, person, group, and event reports.
-   [ ] Compare date ranges.
-   [ ] Export reports.
-   [ ] Import valid records.
-   [ ] Identify invalid and duplicate records.
-   [ ] Download error details and retry failed records.
-   [ ] Back up and restore data.

## 26. Open Product Decisions

These decisions should be confirmed before detailed screen design and
final workflows:

1.  **Group participation:** Should the product support both
    personal-only tracking and collaborative groups where friends can
    join?
2.  **Account tracking:** Should accounts be managed manually, through
    statement imports, or through supported automatic connections?
3.  **Platforms:** Should the first release be web, mobile, or both?
4.  **Currency:** Should the product start with INR and support multiple
    currencies later, or support multiple currencies from the beginning?
5.  **Family finance:** Should family or household finance be included
    in the first release or kept for a later phase?
6.  **Investment scope:** Should investments initially be manually
    tracked, with live market data considered later?
7.  **Sharing and privacy:** Which group details can members see, and
    which information must always remain private?

------------------------------------------------------------------------

## Product Success Criteria

A user should be able to open the application and confidently answer
these questions:

-   How much money do I have in each account?
-   How much did I earn and spend?
-   Where did my money go?
-   How much money do people owe me?
-   How much money do I owe each person?
-   Why does each person have that balance?
-   Which group or event created the balance?
-   What bills and repayments are coming up?
-   How much have I lent or borrowed?
-   What are my assets, liabilities, and net worth?
-   Am I staying within my budgets?
-   Can I export or restore all my financial records?

**The central promise of the product:** Every amount should be
traceable, every person's balance should be explainable, and the user
should always know where their money is and what they owe or are owed.
