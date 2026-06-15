# 🔔 Smart Budget Notifications

## Overview
The **Smart Budget Notifications** feature helps users stay informed about their spending habits by providing timely alerts and reminders. Instead of discovering at the end of the month that they have overspent, users receive notifications throughout the budgeting period that encourage better financial decisions.

---

## Purpose

This feature aims to:

- Help users avoid overspending.
- Encourage consistent use of the application.
- Keep users informed about their budgeting progress.
- Support responsible money management through timely reminders.

---

## How It Works

The application continuously monitors the user's expenses and compares them against their predefined budget limits. When certain conditions are met, the system automatically generates notifications.

---

## Notification Triggers

### 📊 Budget Threshold Alerts

Notifications are generated when users approach or exceed their spending limits.

#### Examples:

- 50% of a category budget used.
- 80% of a category budget used.
- 100% of a category budget used.
- Category budget exceeded.

> **Example:**  
> *"You have used 80% of your Transport budget for this month."*

---

### 💸 Monthly Budget Alerts

Notifications relating to the user's overall monthly budget.

#### Examples:

- Nearing the monthly spending limit.
- Monthly budget exceeded.

> **Example:**  
> *"Warning: You have exceeded your monthly budget by R250."*

---

### 📝 Expense Logging Reminders

Encourages users to maintain accurate financial records.

> **Example:**  
> *"Don't forget to log today's expenses to keep your records up to date."*

---

### 🎯 Savings Encouragement Notifications

Motivational alerts when users are close to reaching savings goals.

> **Example:**  
> *"You're only R150 away from reaching your monthly savings goal!"*

---

## Functional Requirements

- [ ] Monitor spending against budget limits.
- [ ] Generate notifications based on predefined thresholds.
- [ ] Display notifications within the application.
- [ ] Allow users to mark notifications as read.
- [ ] Store notification history for future reference.

---

## Suggested Data Structure

| Field | Description |
|---------|-------------|
| NotificationID | Unique identifier for the notification |
| UserID | User receiving the notification |
| Title | Notification heading |
| Message | Detailed notification content |
| Type | Budget Alert, Reminder, Savings Alert, etc. |
| DateCreated | Date and time generated |
| IsRead | Indicates whether the notification has been viewed |

---

## Benefits

✅ Promotes proactive budgeting.

✅ Reduces the likelihood of overspending.

✅ Encourages regular engagement with the application.

✅ Improves users' awareness of their financial behaviour.

---

## Example Notifications

```text
🔔 You have used 80% of your Grocery budget.

⚠️ Warning: You have exceeded your Entertainment budget by R120.

📝 Don't forget to log today's expenses.

🎉 You're only R150 away from reaching your savings goal!
