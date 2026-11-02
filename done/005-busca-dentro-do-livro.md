# 005 — Busca dentro do livro

**Prioridade:** média · **Área:** `ui/reader/epub/`, `ui/reader/pdf/` · **Status:** feito (2026-11-02)

## Contexto
Não há como achar uma palavra ou frase no livro. Está no roadmap do CLAUDE.md ("PDF sem busca"). No EPUB o Readium já tem busca de texto; no PDF o `PdfRenderer` só expõe texto a partir do Android 15 (API 35).

## Objetivo
Buscar um termo no livro aberto, ver a lista de ocorrências com contexto e pular até qualquer uma.

## Comportamento esperado
- Ícone de lupa na barra superior do leitor abre uma sheet com `NeoTextField` e a lista de resultados.
- Cada resultado mostra o trecho com o termo em negrito e o capítulo/página. A contagem ("23 resultados") usa algarismos tabulares.
- Toque num resultado fecha a sheet, vai até a posição e destaca o termo na página (EPUB) por alguns segundos.
- A busca ignora maiúsculas e acentos ("acao" encontra "Ação").
- Resultados chegam aos poucos (livro grande não trava a tela); indicador de "Buscando…" enquanto isso.
- Sem resultados: texto próprio ("Nada encontrado para “x”").

## Critérios de aceite
- [ ] Buscar "Capitu" em Dom Casmurro lista ocorrências de vários capítulos e o toque leva até cada uma.
- [ ] Busca sem acento encontra palavra acentuada.
- [ ] Digitar rápido não dispara várias buscas ao mesmo tempo (debounce + cancelamento da anterior).
- [ ] PDF em Android < 15: a lupa não aparece (ou aparece desabilitada com explicação). Nada de botão que não funciona.

## Notas de implementação
- EPUB: `publication.search(query)` (`SearchService`, `@ExperimentalReadiumApi`) devolve um `SearchIterator`; `next()` traz `LocatorCollection` por página de resultados. Conferir na 3.0.3 se o `streamer` já instala o `StringSearchService` para EPUB e quais opções aceita (`SearchService.Options(caseSensitive, diacriticSensitive)`).
- Ir até o resultado: `navigator.go(locator)`; destaque temporário via decorations (mesmo mecanismo da task 004, grupo `"search"`).
- PDF: `PdfRenderer.Page.getTextContents()` existe só na API 35+. Nesta task: busca no PDF apenas em Android 15+, página a página dentro do `Mutex` do `PdfDocument`, em `Dispatchers.IO`. Abaixo disso, avaliar depois uma biblioteca (pdfbox-android é pesada, ~10 MB).
- Rodar a busca num `Job` cancelável no ViewModel/activity; `debounce` de ~300 ms no texto.

## Testes (escrever antes)
- Unitário: função de normalização (`"Ação" → "acao"`) usada para destacar o termo no trecho.
- Unitário: montagem do trecho de contexto (corta em palavras, reticências nas pontas).

## Fora do escopo
Busca na biblioteca inteira, expressões regulares, histórico de buscas.

## Resolução (2026-11-02)
- Motor comum `ui/reader/BookSearch` (sem Android, só coroutines): debounce de 300 ms, mínimo de 2 caracteres, `flatMapLatest` (termo novo cancela a busca anterior), resultados emitidos aos poucos (lotes do motor) com `searching = true` até o fim, teto de 500 ocorrências.
- `SearchSheet` compartilhada: `NeoTextField` com foco, contagem "23 resultados" em algarismos tabulares, "Buscando…", "Nada encontrado para “x”", trecho com o termo em negrito e o rótulo da posição.
- **EPUB** (`epub/EpubSearch.kt`): `publication.search(query, Options(caseSensitive = false, diacriticSensitive = false))` — conferido no bytecode que o `EpubParser` da 3.0.3 já instala o `StringSearchService` (ICU). Cada página do `SearchIterator` vira um lote. Trecho montado de `locator.text` (before/highlight/after) com `TextSearch.snippet`; rótulo "Capítulo N · X%" (o mesmo dos marcadores). Toque: fecha a sheet, `navigator.go` e destaca o termo por 4 s (grupo de decoração `"search"`).
- **PDF**: `PdfDocument.pageText` (`PdfRenderer.Page.getTextContents()`, só API 35+, dentro do `Mutex`, em IO), página a página; busca com `TextSearch.matches` (sem acento/maiúsculas), rótulo "Página N", toque pula para a página. Abaixo do Android 15 e no CBZ a lupa **não aparece** (`PdfReaderViewModel.canSearch`).
- `domain/TextSearch`: `normalize` (NFD sem marcas + minúsculas), `matches` (intervalos no texto original, mesmo com acento decomposto) e `snippet` (corta em palavras, reticências nas pontas, colapsa quebras de linha).
- Testes: `TextSearchTest` (normalização, intervalos no original, trecho de contexto).
- **Verificado no celular (moto g73, Android 14)** com o EPUB de teste: "acao" encontra "ação" e "coração" (108 ocorrências, termo em negrito); tocar leva até o trecho e pinta o termo por alguns segundos, também saltando do Capítulo 1 para o 3. Digitar rápido não dispara buscas em paralelo (debounce + cancelamento). Não testado: busca no PDF (precisa de Android 15+), que fica só no código; e um livro grande de verdade (Dom Casmurro).
