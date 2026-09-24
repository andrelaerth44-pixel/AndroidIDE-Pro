# AndroidIDE Pro — Icon Center

## Goal

Make icon/asset discovery an integrated IDE capability, not a separate web-search workflow.

## Initial repositories

- Material Symbols
- Material Design Icons
- Lucide
- Tabler
- Phosphor
- Fluent UI Icons
- Heroicons
- Carbon

Repository integrations must respect each project's license and distribution rules.

## Architecture

```text
Icon Center
    ↓
IconRepository
 ├── Material
 ├── MDI
 ├── Lucide
 ├── Tabler
 ├── Phosphor
 ├── Fluent
 ├── Heroicons
 └── Carbon
```

## Formats

- SVG
- VectorDrawable
- Android XML
- PNG/WebP where conversion is appropriate
- Lottie

## UX

Search:

`chat`

Results can be grouped by repository and previewed before insertion.

## Android output

Possible generated outputs:

```kotlin
Icon(
    imageVector = Icons.Rounded.Chat,
    contentDescription = null
)
```

or:

```text
res/drawable/ic_chat.xml
```

## Asset Center

Icon Center is intended to grow into Asset Center:

- icons
- illustrations
- Lottie
- emojis
- stickers
- splash assets
- templates

## AI Icon Match

Optional API-powered feature.

The IDE sends a textual project/context description to the user's configured provider and receives asset suggestions.

No local AI model is required for the first implementation.

## Architectural inspiration

O CodeAssist atual expõe uma abordagem extensível de repositórios de ícones e file icons por extension points, evitando acoplar cada biblioteca diretamente à UI. O AndroidIDE Pro pode adotar o mesmo princípio arquitetural, mantendo APIs e implementação próprias. Referência: CodeAssist/docs/extension-points.md.

A meta do Pro é mais ampla: repositories de ícones serão uma camada do Asset Center, junto com ilustrações, Lottie e outros assets.