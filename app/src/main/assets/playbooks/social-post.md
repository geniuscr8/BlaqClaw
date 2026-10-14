---
id: social_post
name: Post to social media (X / Instagram / Facebook)
triggers:
  - "publish"
  - "post"
  - "make a post"
  - "tweet"
  - "tweet"
  - "post a story"
  - "post to Instagram"
  - "post on"
  - "tweet"
  - "share a story"
---

The user wants to post to social media. Open the composer and prepare the post,
but stop before publishing unless they told you to publish.

X (Twitter):
1. open_app_action(app="x") and wait(2000), or open the tweet composer directly:
   open_url(url="twitter://post?message=TEXTO")  (fallback web: https://twitter.com/intent/tweet?text=TEXTO)
2. get_screen_info; if the text isn't filled, tap the box and input_text it.
3. STOP before posting unless told to publish. If authorized, tap "Postear"/"Post"
   and verify_screen(expect="enviado"/the new tweet).

Instagram / Facebook:
1. open_app_action(app="instagram" | "facebook").
2. Tap the "+" / create. For a photo post/story, choose the photo from the gallery.
3. Add the caption with input_text.
4. STOP before publishing unless told to. If authorized, tap Share/Compartir.

Rules:
- Posting is public and irreversible-ish — NEVER publish without explicit authorization.
- Show the user the exact text you'll post before publishing.
- Don't post photos the user didn't choose.
