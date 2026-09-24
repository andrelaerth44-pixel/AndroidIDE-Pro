# AndroidIDE Pro — Language Support

## Model

Cada linguagem deve separar editor-time e build-time capabilities.

~~~text
LanguageBackend
├── analyze
├── completion
├── diagnostics
├── navigation
└── compiler
~~~

Um backend pode fornecer análise sem necessariamente fornecer compilação, e o build system pode usar uma implementação diferente para gerar bytecode.

## Current

- Java: editor/backend existente; native V1 compilation path inicial.
- Kotlin: editor infrastructure exists in the AndroidIDE base; native compiler path is planned.
- XML: existing Android XML infrastructure.

## Planned

- JavaScript / TypeScript
- Python
- C / C++
- Rust
- Go
- Bash
- HTML / CSS / SCSS
- JSON / YAML / TOML
- SQL

## Kotlin

Kotlin support will be added to the native build engine as a compiler backend rather than as a special case inside AndroidModule.

Compiler plugins such as Compose must be contributed through a generic compiler-plugin contract.