# Business Requirements — MobileTrack

## Problem Statement

The user struggles with compulsive phone usage patterns, specifically:
- **Glance addiction** — picking up the phone "just to check" but being unable to put it down
- **Endless scrolling** — getting pulled into YouTube Reels, Instagram Reels, and similar infinite-scroll feeds
- **Peeking behavior** — constantly unlocking the phone without a real purpose
- **Stalking behavior** — compulsively checking other people's profiles/stories/activity

The core issue is a **lack of conscious control** over phone interactions. The user wants a system that acts as an external enforcer of their own intentions.

---

## Goal

Build a well-engineered, systematically planned mobile application that:
1. **Tracks** mobile usage behavior in detail
2. **Controls** and restricts compulsive actions
3. **Raises awareness** in real-time when bad habits are triggered
4. **Empowers** the user to set and enforce their own rules

---

## Core Feature Areas

### 1. Usage Tracking
- Track total screen-on time per day
- Track per-app usage duration and frequency
- Track how many times the phone is unlocked (peeking count)
- Track time spent on specific addictive apps (YouTube, Instagram, TikTok, etc.)
- Daily/weekly usage reports and trends

### 2. Scroll Control
- Detect and limit time spent on infinite-scroll apps (YouTube, Instagram, etc.)
- Set daily time budgets per app
- Alert or block when time budget is exceeded
- Introduce friction (e.g., a pause/confirmation screen) before opening addictive apps

### 3. Peek / Unlock Control
- Count and limit the number of times the phone is unlocked per hour/day
- Show a purpose prompt on unlock: "Why are you picking up your phone?"
- If no valid purpose is selected, nudge the user to put the phone down
- Set quiet windows (times when phone is locked down except for calls)

### 4. Habit Awareness & Nudges
- Real-time notifications when a usage threshold is crossed
- End-of-day summary with behavior score
- Weekly insights: "You unlocked your phone 87 times today. Average: 40."
- Motivational streaks for staying within goals

### 5. Goal Setting & Rules Engine
- User can set daily limits per app
- User can set max unlock count per day
- User can block specific apps during focus/work hours
- Rules can be made stricter over time (gradual discipline mode)

### 6. Accountability
- Optional: share weekly report with a trusted contact
- Lock settings behind a delay or password to prevent impulsive rule-breaking

---

## Non-Functional Requirements

- **Privacy-first**: All data stays on device; no data sent to external servers
- **Lightweight**: Minimal battery impact from background tracking
- **Hard to bypass**: Controls should be genuinely difficult to circumvent impulsively
- **Simple UX**: The app itself should not become another addictive surface

---

## Target User

Solo user (self-discipline tool). No multi-user or social features in v1.

---

## Out of Scope (v1)

- Parental controls
- Multi-device sync
- Cloud backup
- Social/community features
