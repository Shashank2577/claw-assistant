---
name: web-browser
description: Fetch and read web page content from URLs, and open URLs in the browser.
---

# Web Browser

## Examples

- "What's on this page: https://example.com"
- "Summarize this article: [URL]"
- "Open https://google.com in the browser"
- "What does this link say?"

## Instructions

To read the content of a web page (to understand or summarize it), call the `fetch_url` tool with:

- url: The full URL starting with http:// or https://

The tool returns the page title and text content (up to ~4000 characters).

To open a URL in the device's default browser, call the `run_intent` tool with:

- intent: open_url
- parameters: A JSON string with the following field:
  - url: The full URL to open. String.

When the user shares a link and asks about its content, use `fetch_url` first to read it, then summarize or answer questions based on the content. If the user wants to open the link, use `run_intent` with `open_url`.
