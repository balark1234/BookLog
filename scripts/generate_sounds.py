"""Generate kid-friendly WAV assets for BookLog."""
import math
import struct
import wave
from pathlib import Path

SAMPLE_RATE = 22050
OUT = Path(__file__).resolve().parent.parent / "app" / "src" / "main" / "res" / "raw"


def synth(freq: float, duration: float, volume: float = 0.35) -> list[int]:
    count = int(SAMPLE_RATE * duration)
    samples = []
    for i in range(count):
        t = i / SAMPLE_RATE
        envelope = min(1.0, i / (SAMPLE_RATE * 0.02), (count - i) / (SAMPLE_RATE * 0.05))
        value = volume * envelope * math.sin(2 * math.pi * freq * t)
        samples.append(int(max(-32767, min(32767, value * 32767))))
    return samples


def concat(*chunks: list[int]) -> list[int]:
    out: list[int] = []
    for chunk in chunks:
        out.extend(chunk)
    return out


def write_wav(name: str, samples: list[int]) -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    path = OUT / name
    with wave.open(str(path), "w") as wav:
        wav.setnchannels(1)
        wav.setsampwidth(2)
        wav.setframerate(SAMPLE_RATE)
        frames = b"".join(struct.pack("<h", s) for s in samples)
        wav.writeframes(frames)
    print(f"Wrote {path}")


def fanfare() -> list[int]:
    notes = [523, 659, 784, 1047]
    parts = []
    for freq in notes:
        parts.append(synth(freq, 0.18, 0.4))
        parts.append(synth(0, 0.04, 0))
    return concat(*parts)


def coin_jingle() -> list[int]:
    return concat(
        synth(988, 0.08, 0.45),
        synth(1319, 0.12, 0.4),
        synth(1568, 0.16, 0.35),
    )


def happy_pop() -> list[int]:
    return concat(synth(440, 0.07, 0.35), synth(660, 0.12, 0.35))


def success_chime() -> list[int]:
    return concat(synth(587, 0.1, 0.35), synth(740, 0.1, 0.35), synth(880, 0.18, 0.35))


def ka_ching() -> list[int]:
    return concat(
        synth(1200, 0.05, 0.4),
        synth(1500, 0.05, 0.4),
        synth(1800, 0.2, 0.35),
    )


def scan_beep() -> list[int]:
    return synth(880, 0.12, 0.3)


def reading_loop() -> list[int]:
    melody = [262, 330, 392, 523, 392, 330, 294, 330, 392, 440, 392, 330, 262, 294, 330, 262]
    parts = []
    for freq in melody:
        parts.append(synth(freq, 0.28, 0.22))
    return concat(*parts)


if __name__ == "__main__":
    write_wav("sound_milestone_unlock.wav", fanfare())
    write_wav("sound_milestone_rewards.wav", coin_jingle())
    write_wav("sound_book_added.wav", happy_pop())
    write_wav("sound_book_saved.wav", success_chime())
    write_wav("sound_reward_redeem.wav", ka_ching())
    write_wav("sound_scan_success.wav", scan_beep())
    write_wav("music_reading_loop.wav", reading_loop())