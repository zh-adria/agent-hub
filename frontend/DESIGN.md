# AgentHub — DESIGN.md

## Visual Theme & Atmosphere

Dark-first developer platform with a single teal accent. Think **Supabase** meets **Linear** — void-black canvas with precise, intentional surfaces. Every pixel earns its place. No decoration without function. The interface should feel like a premium IDE: confident, calm, technical.

- **Density**: Information-rich but breathable. Dashboard-level density for the admin console, spacious for forms.
- **Mood**: Professional, capable, slightly austere. The kind of tool that engineers trust.
- **Philosophy**: Subtract until nothing can be removed. Every element has a purpose.

---

## Color Palette & Roles

| Name | Hex | Role |
|------|-----|------|
| bg-base | #0b0e12 | Page background |
| bg-raised | #12161d | Card / panel surface |
| bg-overlay | #181d27 | Hover states, elevated surfaces |
| bg-inset | #0d1117 | Input fields, code blocks |
| bg-hover | #1c2230 | Interactive hover |
| bg-active | #222a3a | Active / selected state |
| border-base | #21293a | Default borders |
| border-subtle | #161c2a | Dividers, inner borders |
| border-strong | #2d3750 | Strong dividers |
| border-focus | #2dd4a8 | Focus ring |
| text-primary | #e6ecf5 | Primary text |
| text-secondary | #a3b1c6 | Secondary text, labels |
| text-tertiary | #6b7a94 | Muted labels, hints |
| text-muted | #3d4a60 | Placeholders, disabled |
| accent | #2dd4a8 | Primary CTA, active states, links |
| accent-hover | #5eead4 | Hover on accent |
| accent-strong | #0d9488 | Accent on dark bg |
| accent-soft | rgba(45,212,168,0.08) | Subtle accent bg |
| accent-softer | rgba(45,212,168,0.04) | Very subtle accent bg |
| accent-ring | rgba(45,212,168,0.35) | Focus ring shadow |
| danger | #f87171 | Errors, destructive |
| danger-soft | rgba(248,113,113,0.1) | Danger background |
| warning | #fbbf24 | Warnings |
| warning-soft | rgba(251,191,36,0.1) | Warning background |
| success | #2dd4a8 | Success (same as accent) |
| success-soft | rgba(45,212,168,0.1) | Success background |
| info | #60a5fa | Info / informational |
| info-soft | rgba(96,165,250,0.1) | Info background |

---

## Typography Rules

| Role | Font Family | Weight | Size | Line Height |
|------|------------|--------|------|-------------|
| Display | Space Grotesk | 700–800 | 2rem–2.5rem | 1.1 |
| Headings | Space Grotesk | 700 | 1.25rem–1.5rem | 1.2 |
| Body | Inter | 400–500 | 0.875rem | 1.55 |
| Labels | Inter | 600 | 0.8125rem | 1.4 |
| Mono | JetBrains Mono | 400–500 | 0.8125rem | 1.6 |
| Caps | Inter | 600 | 0.75rem | 1.4 |

**Rules**:
- NEVER use `Inter` for display text (headings use Space Grotesk)
- Mono font only for code, IDs, endpoints, token counts
- Text hierarchy: primary (#e6ecf5) → secondary (#a3b1c6) → tertiary (#6b7a94) → muted (#3d4a60)
- Letter-spacing: -0.02em on display, +0.04em on uppercase labels

---

## Component Stylings

### Buttons
- **Primary**: `#2dd4a8` bg, `#0b0e12` text, no border, `8px` radius
- **Secondary**: transparent bg, `#21293a` border, `#a3b1c6` text
- **Ghost**: transparent bg, no border, `#6b7a94` text → hover `#1c2230` bg
- **Danger**: `rgba(248,113,113,0.1)` bg, `#f87171` text, danger border
- Min height: 36px, padding: 8px 16px, weight 600
- Hover: translateY(-1px) + glow shadow on primary

### Cards
- Background: `#12161d`, border: `1px solid #21293a`, radius: `12px`
- Hover: border brightens to `#2d3750`
- Active/selected: border `#2dd4a8` + `rgba(45,212,168,0.08)` bg + glow ring
- Padding: 20px

### Inputs
- Background: `#0d1117`, border: `1px solid #21293a`, radius: `8px`
- Focus: `#2dd4a8` border + `0 0 0 3px rgba(45,212,168,0.35)` ring
- Placeholder: `#3d4a60`
- Min height: 38px, padding: 8px 12px

### Navigation (Sidebar)
- Width: 260px, bg: `#12161d`, right border: `#21293a`
- Nav item: 40px min-height, 8px radius on hover
- Active: `rgba(45,212,168,0.08)` bg, `#2dd4a8` text, left accent bar (3px, glow)
- Icon + label layout, font 14px, weight 500

### Tables
- Header bg: `#181d27`, uppercase labels, 12px, weight 600
- Row hover: `#1c2230` bg
- Row border-bottom: `#161c2a`
- Padding: 9px vertical, 14px horizontal

### Badges / Pills
- Radius: full (9999px), padding: 2px 8px
- Success: `#2dd4a8` on `rgba(45,212,168,0.1)`
- Danger: `#f87171` on `rgba(248,113,113,0.1)`
- Warning: `#fbbf24` on `rgba(251,191,36,0.1)`
- Info: `#60a5fa` on `rgba(96,165,250,0.1)`

### Modal
- Overlay: `rgba(0,0,0,0.65)` + backdrop-blur(4px)
- Panel: `#12161d` bg, `#21293a` border, `16px` radius
- Shadow: `0 12px 40px rgba(0,0,0,0.6)`
- Animation: fadeIn + slideUp, 350ms ease-out

### Chat Messages
- User: `#2dd4a8` bg, `#0b0e12` text, bottom-right radius 6px
- Assistant: `#12161d` bg, `#e6ecf5` text, `#21293a` border, bottom-left radius 6px
- Avatar: 32px circle, user gets accent bg, assistant gets overlay bg

---

## Layout Principles

- **Sidebar**: Fixed 260px, sticky, full viewport height
- **Main content**: Flex 1, scrollable, max-width 1400–1600px for content areas
- **Spacing scale**: 4 / 8 / 12 / 16 / 20 / 24 / 32 / 40 / 48 / 64px
- **Grid gaps**: 16–20px between major sections
- **Panel padding**: 20–24px
- **Responsive**: Collapse sidebar to icons at <860px, single-column at <1000px

---

## Depth & Elevation

| Level | Shadow | Usage |
|-------|--------|-------|
| sm | 0 1px 3px rgba(0,0,0,0.4) | Cards, list items |
| md | 0 4px 16px rgba(0,0,0,0.5) | Elevated cards on hover |
| lg | 0 12px 40px rgba(0,0,0,0.6) | Modals |
| glow | 0 0 20px rgba(45,212,168,0.15) | Accent button glow |
| inset | inset 0 1px 3px rgba(0,0,0,0.3) | Input inner shadow |

Z-index layers:
- Base: 0
- Sidebar indicator: 1
- Cards: 1
- Modal overlay: 100
- Modal panel: 101

---

## Do's and Don'ts

### DO
- Use `#2dd4a8` (teal) as the single accent color everywhere
- Use Space Grotesk for all headings and display text
- Use subtle border transitions on hover (`#21293a` → `#2d3750`)
- Add `translateY(-1px)` + glow on primary button hover
- Show active state on sidebar with accent bar + soft bg
- Use `font-mono` for code, IDs, endpoints
- Show skeleton/empty states with dashed borders

### DON'T
- Don't use pure black (`#000`) — use `#0b0e12` at darkest
- Don't use pure white — use `#e6ecf5` at lightest
- Don't use purple, blue, or gradient accents
- Don't nest cards inside cards without a clear visual reason
- Don't use `Inter` for headings
- Don't use bounce/elastic easing
- Don't use gray text on colored backgrounds (contrast failure)
- Don't use more than 3 font families

---

## Responsive Behavior

| Breakpoint | Behavior |
|-----------|----------|
| > 1200px | Full 3-column KB layout, multi-column grids |
| 860–1200px | Collapse grids to 2 columns, 2-col KB |
| < 860px | Collapsed sidebar (icons only), single column |
| < 768px | Full-width forms, stacked layout, hidden sidebar labels |

Touch targets: minimum 40px height, 44px on mobile.

---

## Agent Prompt Guide

### Quick Reference
```
Primary bg: #0b0e12
Surface: #12161d
Border: #21293a
Accent: #2dd4a8
Text: #e6ecf5 / #a3b1c6 / #6b7a94
Font headings: Space Grotesk
Font body: Inter
Font mono: JetBrains Mono
Radius: 6–12px
```

### Generating a new page
1. Background: `var(--bg-base)`
2. Content wrapper: `padding: 24px; max-width: 1400px`
3. Page title: Space Grotesk, 24px, weight 700, -0.02em tracking
4. Subtitle: Inter, 14px, `#6b7a94`
5. Cards: `#12161d` bg, `#21293a` border, 12px radius, 20px padding
6. Primary button: `#2dd4a8` bg, `#0b0e12` text, 8px radius
7. Input: `#0d1117` bg, `#21293a` border, focus `#2dd4a8` + ring
8. Tables: header `#181d27` bg, uppercase labels, row hover `#1c2230`
9. Animations: `cubic-bezier(0.16, 1, 0.3, 1)`, 200–350ms
