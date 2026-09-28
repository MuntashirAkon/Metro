# LRCLIB integration

This fork adds direct integration with the LRCLIB API to retrieve song lyrics from the Metro lyrics screen.

## What was changed

### API client

The integration is implemented under:

`app/src/main/java/code/name/monkey/retromusic/lrclib/`

It contains:

- `LrclibApi.kt` — Retrofit API definition.
- `LrclibClient.kt` — HTTP client, request handling, logging, and result handling.
- `LrclibResponse.kt` — LRCLIB response model.

The client uses the LRCLIB `get` endpoint with the song title, artist, album, and duration when available.

### Lyrics screen

`LyricsFragment.kt` calls the LRCLIB client when lyrics are requested. Synchronized lyrics are preferred when LRCLIB returns them.

### LRC storage

`LyricUtil.kt` was updated so downloaded synchronized lyrics can be written and loaded reliably on modern Android versions. On Android 11 and newer, the integration can use the application's external files directory for its downloaded LRC files, avoiding failures caused by scoped storage restrictions.

## Network compatibility

The LRCLIB client explicitly uses HTTP/1.1 and sends JSON-oriented request headers. This was added after testing showed that LRCLIB requests could fail from Android when using the default protocol negotiation, while the equivalent request worked correctly with `curl`.

## Error handling

The lyrics flow handles:

- HTTP/API failures.
- Missing synchronized lyrics.
- LRC file creation failures.
- LRC loading failures.
- Unexpected exceptions during the asynchronous lyrics request.

The lyrics screen provides user feedback when an operation cannot be completed.

## Attribution

- **Metro:** original music player project and source code. The original copyright notices and GPLv3 license are retained in this repository.
- **LRCLIB:** lyrics data service/API used by this fork.
- **OpenAI ChatGPT:** assisted with code analysis, debugging, implementation, and documentation of the LRCLIB integration.

This repository is an unofficial fork and is not affiliated with or endorsed by Metro's original maintainers or LRCLIB.
