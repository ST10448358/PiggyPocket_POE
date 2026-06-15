
---

# `README.md` – Wish List Planner

````markdown
# 🛍️ Wish List Planner

## Overview

The **Wish List Planner** feature allows users to save towards future purchases or experiences by setting personalised financial goals. Rather than making impulsive purchases, users can plan ahead and track their progress over time.

---

## Purpose

This feature is designed to:

- Encourage goal-oriented saving.
- Help users prioritise their financial goals.
- Reduce impulse spending.
- Increase motivation by connecting budgeting to meaningful rewards.

---

## How It Works

Users create a wish list item by specifying what they would like to save for. The application then tracks progress as contributions are made toward the goal.

---

## Creating a Wish List Item

Users can provide the following information:

- Item or goal name.
- Target amount.
- Target date *(optional)*.
- Description or notes *(optional)*.

### Examples

| Goal | Target Amount |
|--------|---------------:|
| New Laptop | R12,000 |
| Holiday Trip | R5,000 |
| Graduation Outfit | R2,500 |
| Smartphone Upgrade | R8,000 |

---

## Progress Tracking

For each goal, users can view:

- Total amount required.
- Amount already saved.
- Remaining amount needed.
- Percentage completed.
- Target date status.

### Example

| Item | Goal Amount | Saved | Remaining | Progress |
|---------|-----------:|-------:|-----------:|----------:|
| Laptop | R12,000 | R7,500 | R4,500 | 63% |

---

## Contributions

Users can:

- ➕ Add savings contributions.
- 📜 View contribution history.
- ✏️ Update target amounts if circumstances change.
- 🗑️ Delete goals that are no longer relevant.

---

## Goal Completion

Once the target amount has been reached:

- The goal is marked as **Completed**.
- A congratulatory message is displayed.
- Users may receive badges or rewards through the gamification system.

> **Example:**  
> *"Congratulations! You have reached your savings goal for your Holiday Trip."*

---

## Functional Requirements

- [ ] Create wish list goals.
- [ ] Edit wish list items.
- [ ] Delete wish list items.
- [ ] Record savings contributions.
- [ ] Automatically calculate progress.
- [ ] Indicate completed goals.
- [ ] Maintain contribution history.

---

## Suggested Data Structure

| Field | Description |
|---------|-------------|
| WishListID | Unique identifier for the goal |
| UserID | Owner of the goal |
| ItemName | Name of the item or goal |
| Description | Optional notes |
| TargetAmount | Amount required |
| SavedAmount | Current amount saved |
| TargetDate | Optional completion date |
| DateCreated | Date the goal was created |
| Status | Active or Completed |

---

## Benefits

✅ Encourages disciplined saving habits.

✅ Connects budgeting with meaningful personal goals.

✅ Reduces impulse purchases.

✅ Improves user motivation and engagement.

✅ Integrates naturally with PiggyPocket's gamification system.

---

## Example Wish List

```text
🛍️ Holiday Trip
Target: R5,000
Saved: R3,200
Remaining: R1,800
Progress: 64%
Status: Active
