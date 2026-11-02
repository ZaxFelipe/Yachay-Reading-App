# 004 — Marcadores e destaques

**Prioridade:** média · **Área:** `data/db/`, `ui/reader/epub/`, `ui/reader/pdf/`, `ui/collections/BookActionsSheet` · **Status:** feito (2026-11-02)

## Contexto
Hoje o leitor só lembra a última posição (`lastPosition`). Não dá para marcar uma página para voltar depois nem grifar um trecho. Está em "Limitações / roadmap" no CLAUDE.md ("Sem marcadores, anotações").

## Objetivo
Marcar páginas (PDF e EPUB) e grifar trechos (EPUB), com uma lista por livro para pular até cada um.

## Comportamento esperado
- **Marcador:** ícone na barra superior dos dois leitores. Toque alterna marcado/desmarcado na posição atual; ícone preenchido quando a página atual já tem marcador.
- **Destaque (só EPUB):** selecionar texto mostra a ação "Destacar" no menu de seleção. O trecho fica pintado (limão translúcido no tema Papel, roxo translúcido no Noturno) e continua pintado ao reabrir o livro e ao mudar a fonte.
- Tocar num destaque abre uma sheet com "Remover destaque".
- **Lista:** botão na barra do leitor abre uma sheet com duas abas ("Marcadores" / "Destaques"), cada item com o rótulo da posição ("Página 12" ou "Capítulo 3 · 40%") e, para destaques, o trecho em 2 linhas. Toque vai até a posição; deslizar ou ícone remove.
- Remover o livro apaga os marcadores e destaques (FK `CASCADE`).

## Critérios de aceite
- [ ] Marcar a página 12 de um PDF, fechar o app, reabrir: a página 12 aparece na lista e o toque leva até ela.
- [ ] Destacar um trecho no EPUB, mudar a fonte para 150%: o destaque continua sobre o mesmo texto.
- [ ] Remover o livro apaga os marcadores dele (conferir pelo DAO).
- [ ] O app atualizado a partir da 0.3.0 mantém a biblioteca (migração testada).
- [ ] Ações com descrição para o TalkBack ("Marcar página", "Remover marcador").

## Notas de implementação
- **Schema muda:** tabela `annotations` (`id`, `bookId` FK CASCADE, `type` BOOKMARK/HIGHLIGHT, `position` String, `text?`, `createdAt`). Para o EPUB, `position` = `Locator.toJSON()` (o mesmo formato de `lastPosition`); para o PDF, o índice da página. Subir `YachayDatabase.version` para 2 e escrever a `Migration(1, 2)`. **Não** usar `fallbackToDestructiveMigration`.
- Se a task 010 (calendário) também mudar o schema, as duas precisam combinar a ordem das migrações.
- EPUB: destaques via `DecorableNavigator.applyDecorations(list, group = "highlights")` com `Decoration.Style.Highlight(tint)`. Seleção: `EpubNavigatorFragment.Configuration.selectionActionModeCallback` + `SelectableNavigator.currentSelection()`. Conferir as assinaturas na 3.0.3.
- Toque no destaque: `DecorableNavigator.addDecorationListener("highlights", ...)`.
- PDF: só marcador por página nesta task (`PdfRenderer` não expõe texto para selecionar).

## Testes (escrever antes)
- Migração 1→2 com `MigrationTestHelper` (exige exportar o schema: ligar `exportSchema` e `room.schemaLocation`). Se ficar pesado, no mínimo um teste da SQL da migração num banco em memória.
- DAO: inserir, listar por livro em ordem de posição/criação, remover; remoção do livro apaga em cascata.

## Fora do escopo
Notas de texto em destaques, várias cores, exportar destaques, destaques no PDF.

## Resolução (2026-11-02)
- **Schema v2** (junto com a task 010, numa migração só, `Migrations.MIGRATION_1_2`): tabela `annotations` (`id`, `bookId` FK CASCADE + índice, `type` BOOKMARK/HIGHLIGHT, `position`, `text?`, `createdAt`). `exportSchema` ligado, schemas em `app/schemas`; `MigrationsTest` (SQLite da JVM) compara a SQL da migração com o schema exportado, confere que a biblioteca sobrevive e que remover o livro apaga as anotações (cascata). `AnnotationDao` + `AnnotationRepository` (no `AppContainer`).
- Leitor de páginas (PDF/CBZ): marcador por página na barra (`BookmarkButton`: contorno ↔ preenchido em limão; TalkBack "Marcar página"/"Remover marcador") e botão de lista. A lista pula pela `jumpTo` do ViewModel (`scrollToItem`).
- EPUB (`ui/reader/epub/EpubAnnotations.kt` para não inchar a activity):
  - Seleção de texto → "Destacar" via `EpubNavigatorFragment.Configuration { selectionActionModeCallback }` + `currentSelection()`; o trecho salvo é `locator.text.highlight`.
  - Destaques via `applyDecorations(…, "highlights")` com `Decoration.Style.Highlight` (limão no Papel, roxo no Noturno; o Readium aplica a transparência), reaplicados quando o tema muda. Tocar num destaque (`addDecorationListener`) abre `HighlightActionsSheet` com "Remover destaque".
  - Marcador da tela: `Bookmarks.sameScreen` (mesmo capítulo e mesmo começo de tela, o arredondamento de `DailyGoal.epubPage`). Mudar a fonte gera telas novas: o marcador continua na lista e leva ao lugar certo; só o ícone deixa de acender naquela tela.
  - Rótulo "Capítulo 3 · 40%": título do sumário (TOC) ou "Capítulo N" pela ordem de leitura + `totalProgression`. Lista em ordem de leitura (`Bookmarks.EpubOrder`).
- `AnnotationsSheet` compartilhada (abas Marcadores/Destaques, trecho em 2 linhas, lixeira com descrição para o TalkBack; o PDF mostra só Marcadores). `NeoTopBar(stackTitle = true)`: título numa linha própria quando a barra tem muitas ações (leitor de EPUB).
- Testes: `MigrationsTest`, `BookmarksTest`.
- **Verificado no celular (moto g73), por cima da 0.3.0:** marcar a página 12 do PDF → fechar o app à força → reabrir → voltar à página 1 → a lista mostra "Página 12" e o toque leva até ela com o marcador aceso. EPUB: "Destacar" no menu de seleção, destaque limão; com fonte 150% + OpenDyslexic o destaque continua sobre a mesma palavra; abas da lista certas; tocar no destaque → "Remover destaque" funciona.
- Concessão: com o callback próprio, o menu de seleção mostra só "Destacar" (o Readium troca o menu inteiro).
