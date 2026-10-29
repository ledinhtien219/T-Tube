# Hey Mimi Pro v11 — Expanded Vietnamese commands
Version 1.1.14 · build 312

This test branch inherits all v10 features, including the TikTok→YouTube application switch and PiP chooser. CI applies the patch after v10.

Voice commands (examples):
- “Mimi tôi cần đổ xăng”, “Mimi sắp hết pin xe điện”, “Mimi xe bị thủng lốp”
- “Mimi tôi đói quá”, “Mimi tìm quán ăn”, “Mimi tìm trạm dừng chân”
- “Mimi bỏ qua bài này”, “Mimi phát nhạc tiếp”, “Mimi cho nhạc to hơn”
- “Mimi đưa mình đến sân bay Nội Bài”, “Mimi về nhà thôi”
- “Mimi cảm ơn”, “Mimi bạn tên gì”, “Mimi tôi mệt quá”
- AI questions need a configured HTTPS proxy and explicit opt-in; API secrets must remain on the server.

Tap the Mimi avatar to talk to the assistant directly even in TikTok/browser without repeating the wake phrase. Do not let addressed non-media questions fall through to YouTube/TikTok search.

Hotword listening with mic off is still NOT implemented and this build does not claim it is.
Source changes are in ci/hey-mimi/v11-expanded-commands.patch, with tests in app/src/test and tests/.

CI AAB is UNSIGNED; sign locally with the registered upload key only, do not publish secrets to GitHub.
