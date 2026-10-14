---
id: banking
name: Banking (check balance / transfer — with confirmation)
triggers:
  - "check my balance"
  - "how much do I have in the bank"
  - "how much do I have in the bank"
  - "make a transfer"
  - "transfer money"
  - "send money"
  - "send money"
  - "pay my card"
  - "check my balance"
  - "send money"
  - "transfer"
---

Banking is SENSITIVE. Read-only is fine; anything that MOVES money requires
explicit, unambiguous confirmation and you must STOP before authorizing.

CHECK BALANCE (read-only):
1. Open the user's bank app (open_app / open_app_action by name, or
   get_installed_apps(keyword="banco/bank") to find it).
2. It will likely require biometric/PIN unlock — the USER must do that; wait.
3. get_screen_info / read_screen_ocr to read the balance.
4. finish(summary="Your balance is …").

TRANSFER / PAY (moves money — HARD STOP):
1. Open the bank app; the user unlocks it.
2. Navigate to transfer, fill recipient + amount from the request (get_screen_info
   between steps; verify each field).
3. **STOP at the confirmation screen.** Do NOT tap "Confirm"/"Send"/"Pay" or
   enter the transaction PIN/OTP. Summarize exactly what will be sent (to whom, how
   much) and ask the user to authorize it themselves.
4. Only proceed past confirmation if the user EXPLICITLY said "confirm and send it"
   in this same request — otherwise leave it for them.

Rules:
- NEVER enter banking PINs, OTPs, or biometric — those are the user's.
- NEVER move money without explicit authorization. When in doubt, stop and ask.
- Report amounts and recipients exactly; don't assume.
