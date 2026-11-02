# 007 — Família de fonte, margens e alinhamento no EPUB

**Prioridade:** média · **Área:** `domain/ReaderPreferences.kt`, `ui/reader/epub/EpubPreferencesMapping.kt`, `ui/reader/ReaderTextControls.kt`, `data/settings/` · **Status:** feito (2026-11-02)

## Contexto
A task 002 trouxe tamanho de fonte e espaçamento. Ficaram de fora (e estão no roadmap do CLAUDE.md) família de fonte, margens e alinhamento. É o mesmo caminho: `ReaderPreferences` → `toEpubPreferences()` → `submitPreferences`.

## Objetivo
Na sheet "Aa" e em Ajustes → "Texto dos EPUBs", escolher a fonte, a margem e o alinhamento, com efeito imediato e sem perder a posição.

## Comportamento esperado
- **Fonte:** chips "Do livro" (padrão), "Serifada", "Sem serifa", "Legível" (Atkinson Hyperlegible ou OpenDyslexic). Cada chip escrito na própria fonte.
- **Margens:** Estreita / Normal / Larga.
- **Alinhamento:** "Do livro" / Esquerda / Justificado.
- Mudança aplica na hora, mantendo o locator (como a fonte em 002).
- Quando a opção exigir desligar os estilos da editora, o mesmo aviso que já existe para o espaçamento aparece.

## Critérios de aceite
- [ ] Cada fonte muda o texto na hora; "Do livro" volta exatamente ao visual original.
- [ ] Margem Larga deixa o texto visivelmente mais estreito sem cortar nada.
- [ ] Fechar e reabrir o EPUB mantém as três escolhas.
- [ ] Com tudo em "Do livro"/Normal, `publisherStyles` não é desligado (sem regressão no visual dos livros).

## Notas de implementação
- `EpubPreferences(fontFamily, pageMargins, textAlign)`. Conferir no bytecode da 3.0.3 (como feito em 002) quais exigem `publisherStyles = false`. Provável: `textAlign` e `fontFamily` exigem; `pageMargins` não.
- Fontes próprias: registrar via `EpubNavigatorFactory`/`EpubNavigatorFragment.Configuration` (`servedAssets` + `addFontFamilyDeclaration`) com os `.ttf` em `assets/fonts/`. Atkinson Hyperlegible e OpenDyslexic são OFL (pode embarcar; incluir a licença).
- Estender `ReaderPreferences` com `fontFamily`, `margins`, `textAlign` (enums do domínio, não do Readium, para os testes ficarem na JVM).
- DataStore: chaves `epub_font_family`, `epub_margins`, `epub_text_align`; inválido cai no padrão.
- Os controles novos entram em `ReaderTextControls.kt` para aparecer nos dois lugares.

## Testes (escrever antes)
- `EpubPreferencesMappingTest`: cada combinação gera os campos certos e só liga `publisherStyles = false` quando precisa.
- `SettingsRepositoryTest`: grava/lê as três chaves; valores inválidos voltam ao padrão.

## Fora do escopo
Fonte enviada pelo usuário, modo rolagem contínua, hifenização.

## Resolução (2026-11-02)
- Domínio (enums do app, sem Readium, testáveis na JVM): `ReaderFont` (PUBLISHER, SERIF, SANS_SERIF, LEGIBLE), `PageMargins` (NARROW 0,5×, NORMAL = padrão do Readium, WIDE 2×), `TextAlignment` (PUBLISHER, LEFT, JUSTIFY) em `ReaderPreferences`, com `overridesPublisherStyles`.
- Conferido no bytecode da 3.0.3 (`EpubSettingsKt.update`): `fontFamily` só liga o `fontOverride` e `pageMargins` vale sempre → os dois funcionam **com** os estilos da editora; `textAlign` (como `lineHeight`) só vale com `publisherStyles = false`. Por isso só desligamos os estilos com espaçamento ≠ Normal ou alinhamento ≠ Do livro; tudo em "Do livro"/Normal mantém `publisherStyles = null`.
- Fonte "Legível" = **OpenDyslexic**, que o Readium 3.0.3 já embute (`assets/readium/fonts/OpenDyslexic-Regular.otf`) e declara sozinho no `EpubNavigatorFragment` → nenhum `.ttf` novo no APK nem `servedAssets`. O chip "Legível" carrega o mesmo arquivo dos assets para se escrever na própria fonte; "Serifada"/"Sem serifa" usam as famílias genéricas.
- DataStore: `epub_font_family`, `epub_margins`, `epub_text_align` (inválido → padrão). O fluxo continua o mesmo de 002 (`readerPreferences` → `toEpubPreferences()` → `submitPreferences`, mantendo o locator).
- `ReaderTextControls.kt`: `ReaderFontChips`, `PageMarginsChips`, `TextAlignmentChips` e `EpubTextControls` (todos os controles + o aviso dos estilos da editora, que agora só aparece quando alguma escolha os desliga). Usado na sheet "Aa" e em Ajustes → "Texto dos EPUBs". `NeoChip` ganhou `fontFamily`.
- Testes: `EpubPreferencesMappingTest` (todas as fontes × margens mantêm `publisherStyles`, alinhamento desliga só fora de "Do livro", valores certos) e `SettingsRepositoryTest` (grava/lê as três chaves; inválidos voltam ao padrão).
- Pendente: conferir no celular que "Do livro" volta ao visual original e que a margem larga não corta texto.
