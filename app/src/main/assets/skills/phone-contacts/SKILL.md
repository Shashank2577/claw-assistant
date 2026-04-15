---
name: phone-contacts
description: Search contacts on the phone and make calls.
---

# Phone Contacts

## Examples

- "Show me my contacts named John"
- "Find Sarah's phone number"
- "Call Mom"
- "What is Alex's number?"

## Instructions

To search contacts, call the `read_contacts` tool with the following parameters:

- query: The name to search for. Use empty string to list all contacts.

The tool returns a list of contacts with their name and phone number.

To make a call after finding a contact, call the `run_intent` tool with:

- intent: make_call
- parameters: A JSON string with the following field:
  - phone_number: The phone number to dial. String.
