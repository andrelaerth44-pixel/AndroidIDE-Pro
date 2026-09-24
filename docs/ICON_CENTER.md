# Icon Center

## Objetivo

Unificar pesquisa, preview, importação e uso de ícones no AndroidIDE Pro.

## Arquitetura

```
IconRepository
   |
   +-- Bundled
   +-- Remote
   +-- Project resources
   +-- Plugin repositories
```

## Operações

- busca;
- ranking;
- filtros;
- preview;
- import;
- conversion;
- insertion.

## Formatos

- SVG;
- VectorDrawable;
- PNG;
- WebP;
- Lottie quando suportado.

## Context awareness

XML, Kotlin/Java e Compose terão inserção específica.

## Licenciamento

Cada coleção deverá possuir metadados de licença.

## Estado

Apenas especificação. Nenhum Icon Center foi implementado ainda.
