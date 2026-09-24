
# AndroidIDE Pro — Plugin Platform

## Escopo

O sistema de plugins é reservado para extensões opcionais que não pertencem ao núcleo de linguagens e compiladores.

Exemplos:

- temas;
- integrações externas;
- ferramentas opcionais;
- provedores de IA;
- automações e ações adicionais.

## Fora do sistema de plugins

Estas capacidades são nativas do AndroidIDE Pro:

- Java;
- Kotlin;
- C;
- C++;
- XML;
- JSON;
- Markdown;
- language services;
- compiladores;
- build engine;
- Android toolchains.

Não existe language plugin nem um plugin que substitua o build engine nativo.

## Compiler plugins

Compiler plugins Kotlin são extensões do compilador Kotlin nativo. Eles não são linguagens, não instalam um novo backend e não mudam a política de linguagem incorporada.

## Segurança

Extensões opcionais continuam sujeitas a permissões explícitas, escopo de filesystem, escopo de rede e dependências declaradas.
