# MobileTrack Design Guide

MobileTrack uses a small Compose design system under `presentation/design`. New UI should use these tokens and components instead of inventing one-off colors, spacing, radii, and cards.

## Visual Direction

MobileTrack should feel calm, focused, and utilitarian. Use blue as the focus/action color, soft neutral surfaces, rounded cards, and clear hierarchy. Avoid random purple gradients, arbitrary `dp` values, and custom hex colors inside screens.

## Rules

- Use `MTColors`, `MTSpacing`, `MTRadius`, `MTIconSize`, and `MTElevation` for visual constants.
- Use shared components from `presentation/design/components` for common cards, buttons, search fields, empty states, metric cards, and list rows.
- Screens should compose product behavior, not redefine styling details.
- Add a token before adding a repeated hardcoded value.
- Launcher-specific visuals may be more expressive, but should still use shared spacing, radius, and icon tokens where practical.
- Do not copy proprietary launcher assets; create inspired layouts using our own colors and components.

## Common Imports

```kotlin
import com.mobiletrack.app.presentation.design.MTSpacing
import com.mobiletrack.app.presentation.design.MTRadius
import com.mobiletrack.app.presentation.design.MTIconSize
import com.mobiletrack.app.presentation.design.components.MTCard
import com.mobiletrack.app.presentation.design.components.MTPrimaryButton
import com.mobiletrack.app.presentation.design.components.MTSectionTitle
```
