---
name: alarm-clock
description: Set alarms on the phone.
---

# Alarm Clock

## Examples

- "Set an alarm for 7:30 AM tomorrow"
- "Wake me up at 6 AM"
- "Set a reminder alarm for 3 PM with the label 'Meeting'"

## Instructions

Call the `run_intent` tool with the following exact parameters:

- intent: set_alarm
- parameters: A JSON string with the following fields:
  - hour: The hour in 24-hour format (0-23). Integer.
  - minute: The minute (0-59). Integer.
  - message: A label for the alarm (optional). String.
