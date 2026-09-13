"""Builds Un_Signed_Overview.pptx — a professional deck about the app.

Style: DARK/Ashes theme — pure black background, ash-grey text,
red gaming accent for section headers, orange accent for callouts.
Widescreen 16:9.
"""
from pptx import Presentation
from pptx.util import Inches, Pt, Emu
from pptx.dml.color import RGBColor
from pptx.enum.shapes import MSO_SHAPE
from pptx.enum.text import PP_ALIGN, MSO_ANCHOR


BG_BLACK       = RGBColor(0x00, 0x00, 0x00)
BG_CHARCOAL    = RGBColor(0x14, 0x14, 0x14)
ASH_LIGHT      = RGBColor(0xE6, 0xE6, 0xE6)
ASH_MED        = RGBColor(0xB0, 0xB0, 0xB0)
ASH_DIM        = RGBColor(0x88, 0x88, 0x88)
RED_ACCENT     = RGBColor(0xE4, 0x14, 0x17)
RED_BRIGHT     = RGBColor(0xFF, 0x33, 0x33)
ORANGE_ACCENT  = RGBColor(0xFF, 0x8A, 0x00)
GOLD_AMBER     = RGBColor(0xFF, 0xB4, 0x54)
CREAM_PINK     = RGBColor(0xFF, 0x3B, 0x7C)
DIVIDER_GREY   = RGBColor(0x2A, 0x2A, 0x2A)


prs = Presentation()
prs.slide_width  = Inches(13.333)
prs.slide_height = Inches(7.5)

BLANK = prs.slide_layouts[6]


def solid_fill(shape, rgb):
    shape.fill.solid()
    shape.fill.fore_color.rgb = rgb
    shape.line.fill.background()


def add_bg(slide, rgb=BG_BLACK):
    bg = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, 0, 0, prs.slide_width, prs.slide_height)
    solid_fill(bg, rgb)


def add_text(slide, left, top, width, height, text, *,
             size=18, bold=False, italic=False, color=ASH_LIGHT,
             align=PP_ALIGN.LEFT, anchor=MSO_ANCHOR.TOP, spacing=None,
             font_name="Calibri"):
    tb = slide.shapes.add_textbox(left, top, width, height)
    tf = tb.text_frame
    tf.margin_left = tf.margin_right = Emu(0)
    tf.margin_top = tf.margin_bottom = Emu(0)
    tf.word_wrap = True
    tf.vertical_anchor = anchor
    lines = text.split("\n")
    for i, line in enumerate(lines):
        p = tf.paragraphs[0] if i == 0 else tf.add_paragraph()
        p.alignment = align
        if spacing is not None:
            p.space_after = Pt(spacing)
        run = p.add_run()
        run.text = line
        run.font.name = font_name
        run.font.size = Pt(size)
        run.font.bold = bold
        run.font.italic = italic
        run.font.color.rgb = color
    return tb


def add_accent_bar(slide, left, top, width, height, rgb):
    bar = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, left, top, width, height)
    solid_fill(bar, rgb)
    return bar


def add_footer(slide, page):
    add_text(slide, Inches(0.4), Inches(7.1), Inches(6), Inches(0.3),
             "UN_SIGNED  ·  OFFLINE-FIRST HEALTH & PRODUCTIVITY DASHBOARD",
             size=9, color=ASH_DIM)
    add_text(slide, Inches(12.4), Inches(7.1), Inches(0.6), Inches(0.3),
             str(page), size=9, color=ASH_DIM, align=PP_ALIGN.RIGHT)


def title_slide(page, title, subtitle, kicker):
    slide = prs.slides.add_slide(BLANK)
    add_bg(slide, BG_BLACK)

    add_accent_bar(slide, 0, Inches(0.3), prs.slide_width, Inches(0.05), RED_ACCENT)
    add_accent_bar(slide, 0, Inches(7.15), prs.slide_width, Inches(0.05), RED_ACCENT)

    add_text(slide, Inches(0.6), Inches(1.3), Inches(12), Inches(0.5),
             kicker, size=14, color=RED_BRIGHT, bold=True)
    add_text(slide, Inches(0.6), Inches(1.9), Inches(12), Inches(2.0),
             title, size=72, bold=True, color=ASH_LIGHT)
    add_text(slide, Inches(0.6), Inches(4.5), Inches(12), Inches(1.5),
             subtitle, size=22, color=ASH_MED, italic=True)

    add_text(slide, Inches(0.6), Inches(6.6), Inches(12), Inches(0.4),
             "com.example.un_signed  ·  Android  ·  Kotlin + Compose",
             size=11, color=ASH_DIM, bold=True)
    return slide


def section_slide(page, section_number, section_title, tagline):
    slide = prs.slides.add_slide(BLANK)
    add_bg(slide, BG_BLACK)
    add_accent_bar(slide, 0, Inches(3.5), Inches(0.4), Inches(0.05), RED_ACCENT)
    add_text(slide, Inches(0.6), Inches(2.8), Inches(4), Inches(0.6),
             f"PART {section_number}", size=16, color=RED_BRIGHT, bold=True)
    add_text(slide, Inches(0.6), Inches(3.6), Inches(12), Inches(1.6),
             section_title, size=56, bold=True, color=ASH_LIGHT)
    add_text(slide, Inches(0.6), Inches(5.4), Inches(12), Inches(1.0),
             tagline, size=18, color=ASH_MED, italic=True)
    add_footer(slide, page)
    return slide


def content_slide(page, title, kicker=None):
    slide = prs.slides.add_slide(BLANK)
    add_bg(slide, BG_BLACK)
    add_accent_bar(slide, Inches(0.6), Inches(0.55), Inches(0.15), Inches(0.6), RED_ACCENT)
    add_text(slide, Inches(0.9), Inches(0.5), Inches(12), Inches(0.6),
             title, size=32, bold=True, color=ASH_LIGHT)
    if kicker:
        add_text(slide, Inches(0.9), Inches(1.05), Inches(12), Inches(0.35),
                 kicker, size=12, color=RED_BRIGHT, bold=True)
    add_accent_bar(slide, Inches(0.6), Inches(1.55), Inches(12.1), Inches(0.02), DIVIDER_GREY)
    add_footer(slide, page)
    return slide


def bullet_list(slide, left, top, width, height, items, *,
                bullet_color=RED_BRIGHT, text_color=ASH_LIGHT, text_size=15,
                spacing=8):
    tb = slide.shapes.add_textbox(left, top, width, height)
    tf = tb.text_frame
    tf.margin_left = tf.margin_right = Emu(0)
    tf.margin_top = tf.margin_bottom = Emu(0)
    tf.word_wrap = True
    for i, item in enumerate(items):
        p = tf.paragraphs[0] if i == 0 else tf.add_paragraph()
        p.alignment = PP_ALIGN.LEFT
        p.space_after = Pt(spacing)
        run = p.add_run()
        run.text = "▸  "
        run.font.name = "Consolas"
        run.font.size = Pt(text_size)
        run.font.bold = True
        run.font.color.rgb = bullet_color
        run2 = p.add_run()
        run2.text = item
        run2.font.name = "Calibri"
        run2.font.size = Pt(text_size)
        run2.font.color.rgb = text_color


def kv_column(slide, left, top, width, rows, *,
              key_color=RED_BRIGHT, val_color=ASH_LIGHT, key_size=12, val_size=13,
              row_gap=10):
    y = top
    for key, val in rows:
        add_text(slide, left, y, width, Inches(0.3), key,
                 size=key_size, bold=True, color=key_color)
        add_text(slide, left, y + Inches(0.3), width, Inches(0.6), val,
                 size=val_size, color=val_color)
        y += Inches(0.95) + Emu(row_gap * 9525)


# =========================================================================
# SLIDE 1 — COVER
# =========================================================================
title_slide(
    page=1,
    kicker="A PERSONAL ANALYTICS DASHBOARD",
    title="UN_SIGNED",
    subtitle=("Health · Productivity · Learning — in one gesture-driven,\n"
              "offline-first, 19-language Android app.")
)

# =========================================================================
# SLIDE 2 — ONE-LINER
# =========================================================================
slide = prs.slides.add_slide(BLANK)
add_bg(slide, BG_BLACK)
add_accent_bar(slide, 0, Inches(0), prs.slide_width, Inches(0.05), RED_ACCENT)
add_text(slide, Inches(0.6), Inches(0.3), Inches(12), Inches(0.5),
         "IN ONE SENTENCE", size=14, color=RED_BRIGHT, bold=True)
add_text(slide, Inches(0.8), Inches(1.5), Inches(11.7), Inches(4.5),
         ("A local-only, offline-first, gesture-driven personal-analytics dashboard "
          "that turns your daily health, learning, and productivity data into "
          "evidence-cited insights across three themed skins and 19 languages — "
          "with no account, no cloud, and no telemetry."),
         size=32, bold=True, color=ASH_LIGHT, italic=True)
add_accent_bar(slide, Inches(0.8), Inches(6.4), Inches(1.2), Inches(0.04), RED_ACCENT)
add_text(slide, Inches(0.8), Inches(6.55), Inches(12), Inches(0.4),
         "com.example.un_signed  ·  Android  ·  Kotlin + Jetpack Compose",
         size=11, color=ASH_DIM, bold=True)
add_footer(slide, 2)

# =========================================================================
# SLIDE 3 — WHAT IT DOES (pillars)
# =========================================================================
slide = content_slide(3, "What it does", "THREE PILLARS · ONE APP")

pillars = [
    ("HEALTH", ASH_LIGHT, RED_BRIGHT,
     "Water, Sleep, Junk\n(Open Food Facts),\nExercise, Meds, Weight,\nSteps (Health Connect)."),
    ("PRODUCTIVITY", ASH_LIGHT, ORANGE_ACCENT,
     "Lectures, Subjects,\nCourses, Practices,\nSkills, Focus (Pomodoro),\nHabits, Calendar."),
    ("BENCHMARKING", ASH_LIGHT, GOLD_AMBER,
     "You vs. global norms\n(WHO, EFSA, Lancet).\nEvidence-cited\nrecommendations."),
]

x = Inches(0.6)
for label, txt_color, accent, body in pillars:
    card = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, x, Inches(2.0), Inches(4.0), Inches(4.6))
    solid_fill(card, BG_CHARCOAL)
    accent_stripe = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, x, Inches(2.0), Inches(4.0), Inches(0.08))
    solid_fill(accent_stripe, accent)
    add_text(slide, x + Inches(0.3), Inches(2.4), Inches(3.6), Inches(0.5),
             label, size=18, bold=True, color=accent)
    add_text(slide, x + Inches(0.3), Inches(3.1), Inches(3.6), Inches(3.4),
             body, size=16, color=txt_color, spacing=6)
    x += Inches(4.2)

# =========================================================================
# SLIDE 4 — HOME SCREEN LAYOUT
# =========================================================================
slide = content_slide(4, "The Home Screen", "SINGLE ACTIVITY · EVERYTHING ELSE IS AN OVERLAY")

add_text(slide, Inches(0.6), Inches(1.9), Inches(6.5), Inches(0.4),
         "The three doors", size=17, bold=True, color=RED_BRIGHT)
bullet_list(slide, Inches(0.6), Inches(2.35), Inches(6.5), Inches(2.5), [
    "IDEAL PROFILE  —  the main hub with 9 modules",
    "CUSTOM PROFILE  —  named routines (duration + type)",
    "EXPORT PROGRESS  —  CSV, JSON, or a PPTX deck",
])

add_text(slide, Inches(0.6), Inches(4.4), Inches(6.5), Inches(0.4),
         "Bottom quick-action bar (gestures only)", size=17, bold=True, color=RED_BRIGHT)
bullet_list(slide, Inches(0.6), Inches(4.85), Inches(6.5), Inches(2.3), [
    "SLEEP  —  hold 3s to start · double-tap to end",
    "JUNK  —  tap −1, double-tap +1, hold 3s = log",
    "WATER —  tap −1, double-tap +1, hold 3s = reset",
], text_size=14)

add_text(slide, Inches(7.6), Inches(1.9), Inches(5.2), Inches(0.4),
         "Also on screen", size=17, bold=True, color=RED_BRIGHT)
bullet_list(slide, Inches(7.6), Inches(2.35), Inches(5.2), Inches(4.5), [
    "Live clock (Jersey 10 Charted font)",
    "Year-progress bar with % elapsed",
    "Upcoming events feed",
    "Active-lecture banner",
    "Weather-aware hydration hints",
    "Haptic tick / click / success on every action",
])

# =========================================================================
# SLIDE 5 — HEALTH MODULES
# =========================================================================
slide = content_slide(5, "Health tracking", "SIX MODULES · EVIDENCE-CITED · WEATHER-AWARE")

rows = [
    ("WATER",    "Glasses logged; custom glass mL; goal auto-computed from body weight + local temperature."),
    ("SLEEP",    "Bedtime, wake time, quality 1–5, disturbance count; hours computed. Session survives app kill."),
    ("JUNK",     "Full Open Food Facts search (country-filtered, LRU-cached); Nutri-Score, NOVA, kcal, sugar, sat fat, salt, additives."),
    ("EXERCISE", "Activity type, minutes, intensity, notes."),
    ("MEDS",     "Meds / Supplements / Severe / Diet log — free-form with 0–10 severity."),
    ("WEIGHT",   "Date + kg/lb with historical view."),
    ("STEPS",    "Via Health Connect; graceful fallback to device step-sensor if unavailable."),
]
y = Inches(2.0)
for key, val in rows:
    add_text(slide, Inches(0.6), y, Inches(2.2), Inches(0.5),
             key, size=14, bold=True, color=RED_BRIGHT)
    add_text(slide, Inches(2.9), y, Inches(10.0), Inches(0.7),
             val, size=13, color=ASH_LIGHT)
    y += Inches(0.68)

# =========================================================================
# SLIDE 6 — PRODUCTIVITY MODULES
# =========================================================================
slide = content_slide(6, "Productivity & learning", "STUDY  ·  FOCUS  ·  ROUTINE")

rows = [
    ("LECTURES",  "Named topics, checkable, progress bar; drives a 'YOU HAVE A LECTURE' home banner."),
    ("SUBJECTS",  "Chapter checklists, durations, repeat schedules."),
    ("COURSES",   "Long-form learning tracks with the same chapter model."),
    ("PRACTICES", "Repeated practice sessions with calendar view."),
    ("SKILLS",    "Three tiers — Hobby / Minor / Major — with per-session history and total minutes."),
    ("FOCUS",     "Pomodoro (25/5) with cycle goals; state and elapsed time survive app kill."),
    ("HABITS",    "Simple named counters."),
    ("CALENDAR",  "Tasks with time + colour; syncs to Android Calendar + Alarm; internal reminder chain."),
]
y = Inches(2.0)
for key, val in rows:
    add_text(slide, Inches(0.6), y, Inches(2.2), Inches(0.5),
             key, size=14, bold=True, color=ORANGE_ACCENT)
    add_text(slide, Inches(2.9), y, Inches(10.0), Inches(0.7),
             val, size=13, color=ASH_LIGHT)
    y += Inches(0.6)

# =========================================================================
# SLIDE 7 — ANALYTICS ENGINE
# =========================================================================
slide = content_slide(7, "Analytics & insights", "YOUR NUMBERS vs. THE WORLD")

add_text(slide, Inches(0.6), Inches(2.0), Inches(6.0), Inches(0.4),
         "Global norms with citations", size=17, bold=True, color=RED_BRIGHT)
bullet_list(slide, Inches(0.6), Inches(2.45), Inches(6.0), Inches(3.5), [
    "WHO — weekly exercise targets",
    "EFSA — hydration baseline",
    "IOM — age-banded sleep hours",
    "Paluch et al. — steps meta-analysis",
    "BMI bands (WHO / ADA / regional)",
], text_size=14)

add_text(slide, Inches(7.0), Inches(2.0), Inches(5.8), Inches(0.4),
         "Four engines", size=17, bold=True, color=RED_BRIGHT)
bullet_list(slide, Inches(7.0), Inches(2.45), Inches(5.8), Inches(3.5), [
    "ComparisonEngine — percentile vs global norms",
    "InsightsEngine — daily snapshot",
    "RecommendationEngine — prioritised tips with sources",
    "AnalyticsEngine — session, sync, completion stats",
], text_size=14)

add_accent_bar(slide, Inches(0.6), Inches(5.8), Inches(12.1), Inches(0.02), DIVIDER_GREY)
add_text(slide, Inches(0.6), Inches(5.95), Inches(12), Inches(0.35),
         "OUTPUT SURFACES", size=12, color=ORANGE_ACCENT, bold=True)
add_text(slide, Inches(0.6), Inches(6.3), Inches(12), Inches(0.6),
         "Daily Briefing overlay  ·  Compare-to-Globe overlay  ·  Recommendations overlay",
         size=13, color=ASH_MED)

# =========================================================================
# SLIDE 8 — THREE THEMES
# =========================================================================
slide = content_slide(8, "Three complete skins", "NOT PALETTES — WHOLE VISUAL IDENTITIES")

themes = [
    ("DARK — ASHES",        RED_BRIGHT,   "#000000 bg, ashen grey plates, red gaming borders (top+bottom). Forged-glass 'SELECT PROFILE' in Nokia Kokia."),
    ("CREAM — HELLO KITTY", CREAM_PINK,   "#F7EFDD bg, pink accents, sparkle field, Canvas-drawn bow decoration and pink header ribbon."),
    ("AMBER — LOKI",        GOLD_AMBER,   "#14090A bg, gold #FFB454 accents, emerald secondary. Canvas-drawn horns + '— GLORIOUS PURPOSE —'."),
]
y = Inches(2.0)
for name, accent, body in themes:
    card = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(0.6), y, Inches(12.1), Inches(1.4))
    solid_fill(card, BG_CHARCOAL)
    stripe = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(0.6), y, Inches(0.15), Inches(1.4))
    solid_fill(stripe, accent)
    add_text(slide, Inches(1.0), y + Inches(0.15), Inches(11), Inches(0.4),
             name, size=16, bold=True, color=accent)
    add_text(slide, Inches(1.0), y + Inches(0.6), Inches(11), Inches(0.8),
             body, size=13, color=ASH_LIGHT)
    y += Inches(1.6)

# =========================================================================
# SLIDE 9 — LOCALIZATION
# =========================================================================
slide = content_slide(9, "Localization", "19 LANGUAGES · 8 INDIC NUMERAL SCRIPTS")

add_text(slide, Inches(0.6), Inches(2.0), Inches(6.0), Inches(0.4),
         "Full translations (8)", size=15, bold=True, color=RED_BRIGHT)
add_text(slide, Inches(0.6), Inches(2.4), Inches(6.0), Inches(3.0),
         ("English · Hindi · Bengali · Marathi\nRussian · Chinese · Japanese · French"),
         size=14, color=ASH_LIGHT, spacing=6)

add_text(slide, Inches(0.6), Inches(3.9), Inches(6.0), Inches(0.4),
         "Numeral-only localization (6)", size=15, bold=True, color=RED_BRIGHT)
add_text(slide, Inches(0.6), Inches(4.3), Inches(6.0), Inches(3.0),
         ("Telugu · Tamil · Gujarati · Kannada\nMalayalam · Punjabi (Gurmukhi)"),
         size=14, color=ASH_LIGHT, spacing=6)

add_text(slide, Inches(0.6), Inches(5.8), Inches(6.0), Inches(0.4),
         "Partial fallback (5)", size=15, bold=True, color=RED_BRIGHT)
add_text(slide, Inches(0.6), Inches(6.2), Inches(6.0), Inches(0.6),
         "German · Spanish · Italian · Portuguese · Dutch",
         size=14, color=ASH_LIGHT)

add_text(slide, Inches(7.0), Inches(2.0), Inches(5.8), Inches(0.4),
         "Example numeral swaps", size=15, bold=True, color=ORANGE_ACCENT)

samples = [
    ("HINDI",     "12:34 → १२:३४"),
    ("BENGALI",   "12:34 → ১২:৩৪"),
    ("TAMIL",     "12:34 → ௧௨:௩௪"),
    ("GUJARATI",  "12:34 → ૧૨:૩૪"),
    ("TELUGU",    "12:34 → ౧౨:౩౪"),
    ("KANNADA",   "12:34 → ೧೨:೩೪"),
    ("MALAYALAM", "12:34 → ൧൨:൩൪"),
    ("PUNJABI",   "12:34 → ੧੨:੩੪"),
]
y = Inches(2.4)
for name, ex in samples:
    add_text(slide, Inches(7.0), y, Inches(2.3), Inches(0.3),
             name, size=11, bold=True, color=ASH_DIM)
    add_text(slide, Inches(9.3), y, Inches(3.5), Inches(0.3),
             ex, size=13, color=ASH_LIGHT, font_name="Consolas")
    y += Inches(0.42)

# =========================================================================
# SLIDE 10 — ARCHITECTURE
# =========================================================================
slide = content_slide(10, "Architecture", "SINGLE ACTIVITY · OVERLAYS · LOCAL JSON")

add_text(slide, Inches(0.6), Inches(2.0), Inches(6.2), Inches(0.4),
         "Runtime", size=15, bold=True, color=RED_BRIGHT)
bullet_list(slide, Inches(0.6), Inches(2.45), Inches(6.2), Inches(4.5), [
    "SplashActivity (1.5s) → ProfileSelectionActivity",
    "All features open as Compose overlays — no new activities",
    "Layout mixes XML with ComposeView islands",
    "State in mutableStateOf/mutableStateListOf on the activity",
    "applyThemeTint() re-renders home on state change",
], text_size=13)

add_text(slide, Inches(7.0), Inches(2.0), Inches(5.8), Inches(0.4),
         "Persistence", size=15, bold=True, color=RED_BRIGHT)
bullet_list(slide, Inches(7.0), Inches(2.45), Inches(5.8), Inches(4.5), [
    "FitDataRepository — Gson-serialised JSON files",
    "FitnessDataRepository — Health Connect + step sensor",
    "~25 JSON files under /fitdata/",
    "No database, no ORM, no cloud, no auth",
    "Timer/Stopwatch/Sleep/Focus survive app kill",
], text_size=13)

# =========================================================================
# SLIDE 11 — DATA MODEL SNAPSHOT
# =========================================================================
slide = content_slide(11, "Data model", "20+ ENTITIES · ALL LOCAL · ALL GSON")

col1 = [
    "UserProfile — computed BMI, BMR, TDEE, sleep target",
    "AppPreferences — units, theme, lang, sync flags",
    "CustomProfile — id, name, duration, type",
    "WaterDailyLog — glasses, goal, weather-aware",
    "SleepEntry / SleepSessionState",
    "ExerciseEntry — activity, min, intensity",
    "WeightEntry — date + kg/lb",
    "LogEntry — meds/supp/severe/diet",
    "JunkLogEntry — OFF product, Nutri-Score, NOVA",
    "FitnessSample — steps, source",
]
col2 = [
    "Subject / Chapter — study tracks",
    "LectureTopic / LectureState",
    "Habit — id, name, count",
    "SkillItem / SkillSession — tiered skills",
    "ActivitySession — cycling/yoga/walking",
    "FocusSession / FocusTimerState",
    "CalendarTask — sync flags for cal + alarm",
    "TimerPersistedState / StopwatchPersistedState",
    "AppSession — session bounds",
    "WeatherData / OffProduct / UpdateInfo",
]
bullet_list(slide, Inches(0.6), Inches(2.0), Inches(6.2), Inches(5.0), col1,
            text_size=11, spacing=4)
bullet_list(slide, Inches(7.0), Inches(2.0), Inches(5.8), Inches(5.0), col2,
            text_size=11, spacing=4)

# =========================================================================
# SLIDE 12 — CALENDAR & REMINDERS
# =========================================================================
slide = content_slide(12, "Calendar & reminders", "TASK IN, THREE PLACES OUT")

add_text(slide, Inches(0.6), Inches(2.0), Inches(12), Inches(0.5),
         "One CalendarTask can fan out to three destinations",
         size=15, italic=True, color=ASH_MED)

fanout = [
    ("SYSTEM CALENDAR", ORANGE_ACCENT, "SystemCalendarSync writes an event via CalendarContract; stores the eventId back on the task."),
    ("SYSTEM ALARM",    RED_BRIGHT,    "SystemAlarmSync opens the phone's Alarm Clock app with prefilled hh:mm; marks systemAlarmSet."),
    ("INTERNAL REMINDER CHAIN", GOLD_AMBER, "TaskReminderScheduler + BroadcastReceiver — Pomodoro-style repeating pings at a configurable interval."),
]
y = Inches(2.9)
for name, accent, body in fanout:
    card = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(0.6), y, Inches(12.1), Inches(1.1))
    solid_fill(card, BG_CHARCOAL)
    stripe = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(0.6), y, Inches(0.15), Inches(1.1))
    solid_fill(stripe, accent)
    add_text(slide, Inches(1.0), y + Inches(0.15), Inches(11), Inches(0.4),
             name, size=13, bold=True, color=accent)
    add_text(slide, Inches(1.0), y + Inches(0.55), Inches(11), Inches(0.5),
             body, size=12, color=ASH_LIGHT)
    y += Inches(1.3)

# =========================================================================
# SLIDE 13 — CRAFTSMANSHIP CALLOUTS
# =========================================================================
slide = content_slide(13, "Non-obvious craftsmanship", "THINGS YOU DON'T SEE IN SIMILAR APPS")

items = [
    "PPTX generator from scratch — hand-crafted OOXML, avoids Apache POI's ~10 MB dep",
    "Numeral localization for 8 Indic scripts — rare in consumer apps",
    "Timer / Stopwatch / Sleep / Focus survive app kill via savedAtEpochMs replay",
    "Gesture-only quick-action bar — zero visible controls, single/double/long-press only",
    "Open Food Facts integration with country-filter + LRU cache + product quality scoring",
    "Weather-aware water goal — hydration scales with local temperature",
    "Evidence-cited recommendations — every tip carries its source (WHO / EFSA / Lancet / Paluch)",
    "Red gaming bezel via compositing trick — bg PNG peeks around a black middle mask",
    "Three themes = three full Compose skins, not just palette swaps",
]
bullet_list(slide, Inches(0.6), Inches(2.0), Inches(12), Inches(5.0),
            items, text_size=13, spacing=8)

# =========================================================================
# SLIDE 14 — PERMISSIONS & PRIVACY
# =========================================================================
slide = content_slide(14, "Privacy & permissions", "OFFLINE-FIRST · NO ACCOUNT · NO TELEMETRY")

add_text(slide, Inches(0.6), Inches(2.0), Inches(12.1), Inches(0.4),
         "What leaves the device", size=15, bold=True, color=RED_BRIGHT)
bullet_list(slide, Inches(0.6), Inches(2.45), Inches(12.1), Inches(1.6), [
    "Open Food Facts product lookup (opt-in via junk log)",
    "Weather API for temperature (opt-in via location permission)",
    "GitHub version.json for update checks",
    "That's it — no analytics, no crash reporter, no ad SDKs",
], text_size=13)

add_text(slide, Inches(0.6), Inches(4.6), Inches(12.1), Inches(0.4),
         "Permissions declared", size=15, bold=True, color=RED_BRIGHT)
bullet_list(slide, Inches(0.6), Inches(5.05), Inches(12.1), Inches(2.0), [
    "Internet + coarse location  (weather + OFF)",
    "Vibration  (haptic feedback)",
    "Calendar read/write + exact alarms  (task sync)",
    "Activity recognition + Health Connect  (steps, distance, calories)",
    "Post notifications  (reminders)",
], text_size=13)

# =========================================================================
# SLIDE 15 — TECH STACK
# =========================================================================
slide = content_slide(15, "Tech stack", "KOTLIN · COMPOSE · NO HEAVY DEPS")

rows = [
    ("LANGUAGE",   "Kotlin"),
    ("UI",         "Jetpack Compose + XML ConstraintLayout (hybrid)"),
    ("MIN SDK",    "Android with Health Connect fallback to step sensor"),
    ("STORAGE",    "Gson-serialised JSON files under /fitdata/"),
    ("FONTS",      "Bebas Neue (titles), Jersey 10 Charted (data/clock), Nokia Kokia (data)"),
    ("NETWORK",    "3 endpoints only — OFF, weather, GitHub version.json"),
    ("UPDATES",    "GitHub-hosted version.json → DownloadManager → FileProvider install"),
    ("EXPORTS",    "CSV, JSON, and PPTX (hand-rolled OOXML)"),
]
y = Inches(2.0)
for key, val in rows:
    add_text(slide, Inches(0.6), y, Inches(2.5), Inches(0.5),
             key, size=13, bold=True, color=RED_BRIGHT)
    add_text(slide, Inches(3.2), y, Inches(9.7), Inches(0.7),
             val, size=14, color=ASH_LIGHT)
    y += Inches(0.6)

# =========================================================================
# SLIDE 16 — WHY IT'S DIFFERENT
# =========================================================================
slide = content_slide(16, "Why it's different", "THREE THINGS MOST TRACKERS GET WRONG")

points = [
    ("NO CLOUD, NO ACCOUNT", RED_BRIGHT,
     "Your data lives in the app's private folder. Delete the app, delete the data. Zero servers to breach."),
    ("EVIDENCE, NOT VIBES", ORANGE_ACCENT,
     "Every recommendation and every 'good/concern/at-risk' band cites its source. WHO, EFSA, Lancet, IOM, Paluch — visible in the UI."),
    ("GESTURES, NOT MENUS", GOLD_AMBER,
     "The home bar has no visible buttons. Tap, double-tap, long-press. Fewer taps to log, faster to disappear."),
]
y = Inches(2.0)
for title, accent, body in points:
    add_text(slide, Inches(0.6), y, Inches(12), Inches(0.4),
             title, size=17, bold=True, color=accent)
    add_text(slide, Inches(0.6), y + Inches(0.5), Inches(12), Inches(1.1),
             body, size=14, color=ASH_LIGHT)
    y += Inches(1.65)

# =========================================================================
# SLIDE 17 — CLOSING
# =========================================================================
slide = prs.slides.add_slide(BLANK)
add_bg(slide, BG_BLACK)
add_accent_bar(slide, 0, Inches(0.3), prs.slide_width, Inches(0.05), RED_ACCENT)
add_accent_bar(slide, 0, Inches(7.15), prs.slide_width, Inches(0.05), RED_ACCENT)

add_text(slide, Inches(0.6), Inches(1.3), Inches(12), Inches(0.5),
         "UN_SIGNED", size=16, color=RED_BRIGHT, bold=True)
add_text(slide, Inches(0.6), Inches(2.0), Inches(12), Inches(2.5),
         "Track everything.\nSee where you stand.\nOwn your data.",
         size=54, bold=True, color=ASH_LIGHT)
add_text(slide, Inches(0.6), Inches(5.2), Inches(12), Inches(0.6),
         "Three themes  ·  19 languages  ·  0 accounts required",
         size=18, italic=True, color=ASH_MED)
add_text(slide, Inches(0.6), Inches(6.6), Inches(12), Inches(0.4),
         "com.example.un_signed  ·  Android · Kotlin + Compose",
         size=11, color=ASH_DIM, bold=True)

# =========================================================================
prs.save("Un_Signed_Overview.pptx")
print(f"Wrote Un_Signed_Overview.pptx ({len(prs.slides)} slides)")
