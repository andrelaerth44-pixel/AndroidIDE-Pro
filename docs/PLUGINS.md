# AndroidIDE Pro — Plugin Platform

## Goal

Allow the IDE to gain capabilities without turning the core application into a monolith.

## Plugin categories

- languages
- themes
- build systems
- assets
- tools
- integrations
- AI providers

## Principles

Plugins should declare:

- ID
- version
- capabilities
- permissions
- dependencies

## Safety boundary

A plugin must not receive broad access to the filesystem, network or projects unless the capability explicitly requires it.

The permission model must be designed before a marketplace is introduced.

## Initial API areas

```text
Plugin
├── UI extension
├── command/action
├── language provider
├── build provider
├── asset repository
└── tool provider
```

## Long-term

A local/offline plugin install flow should work without requiring a central marketplace.

## Compiler extension point

O CodeAssist atual usa uma SPI explícita para Kotlin compiler plugins, permitindo que Compose e outras extensões sejam aplicadas por módulo e entregues à tarefa de compilação Kotlin. O AndroidIDE Pro seguirá o mesmo conceito de extensão, mas através dos próprios contratos de build-api. Referência: CodeAssist/docs/extension-points.md.