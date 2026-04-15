---
name: calendar-manager
description: Read upcoming calendar events and add new events to the calendar.
---

# Calendar Manager

## Examples

- "What events do I have this week?"
- "Show my calendar for the next 3 days"
- "Add a meeting tomorrow at 3 PM"
- "Schedule lunch with Sarah on Friday at noon"

## Instructions

To read calendar events, call the `read_calendar` tool with:

- daysAhead: Number of days from today to look ahead. String (e.g., "7" for one week).

The tool returns a list of events with title, start time, end time, and location.

To add a new calendar event, call the `run_intent` tool with:

- intent: write_calendar
- parameters: A JSON string with the following fields:
  - title: The event title. String.
  - begin_time: Event start time in milliseconds since epoch. Long.
  - end_time: Event end time in milliseconds since epoch. Long.
  - location: The event location (optional). String.
  - description: Additional notes (optional). String.

When the user mentions a date/time like "tomorrow at 3pm", compute the correct Unix timestamp in milliseconds.
