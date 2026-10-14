---
id: email_compose
name: Write and send email (Gmail / Outlook)
triggers:
  - "send an email"
  - "send an email"
  - "send an email"
  - "send a message by email"
  - "write an email"
  - "email to"
  - "email to"
  - "send an email"
  - "email to"
  - "write an email"
---

The user wants to send an email. Prefer a deep link to open the composer
pre-filled, then fill the rest by accessibility, and stop before sending unless
they told you to send.

1. Parse the recipient, subject and body from the request when given.
2. Open the composer directly with a mailto deep link (fills recipient/subject/body):
   **open_url(url="mailto:DEST?subject=SUBJECT&body=BODY")**
   - URL-encode spaces as %20. Omit the parts you don't have.
   - If no recipient is known, open Gmail instead: open_app_action(app="gmail").
3. **wait(duration_ms=1500)** then **get_screen_info** to see the composer.
4. Fill any empty field by tapping it and **input_text**: recipient → subject → body.
   For the recipient, after typing, pick the matching contact suggestion.
5. **STOP before sending** and tell the user it's ready to review, UNLESS they said
   "send it"/"send it now". If they authorized it, tap Send and
   verify_screen(expect="Sent" or the conversation/sent view).
6. **finish(summary="Email to X ready/sent: subject …")**.

Rules:
- Never send to the wrong recipient — confirm the contact suggestion matches.
- If the body is long/sensitive, leave it for the user to review before sending.
