# AndroidIDE Pro — UI Guidelines

## Visual direction

Professional mobile IDE.

Reference families:

- Android Studio
- IntelliJ IDEA
- VS Code
- modern Android / Material 3

## Allowed

- Material 3
- Jetpack Compose
- Dynamic Color
- subtle transparency
- restrained blur when useful
- rounded surfaces
- adaptive layouts
- meaningful motion
- dense but readable information

## Avoid

- neon
- cyberpunk styling
- RGB/glow effects
- excessive blur
- arbitrary gradients
- perpetual pulsing
- giant cards
- decorative UI that reduces editor space
- fake "premium" marketing language

## Layout

Phone:

- content first
- bottom sheets and compact panels when needed
- one primary task at a time

Tablet:

- exploit width
- Explorer + Editor + Problems/Build may coexist
- support multi-window/resizable layouts

## Motion

Animation should communicate:

- navigation
- state change
- expansion
- insertion/removal

Avoid animation that merely exists to attract attention.

## Color

Do not force a single brand color across every screen.

Prefer semantic Material color roles and Dynamic Color where available.

## Typography

Use Material 3 typography roles.

Keep code, diagnostics and paths visually distinct with monospace where appropriate.

## Accessibility

Every new surface should consider:

- contrast
- content descriptions
- touch target sizes
- keyboard navigation
- font scaling
- reduced motion
