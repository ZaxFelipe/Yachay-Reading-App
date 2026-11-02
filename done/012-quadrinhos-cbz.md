# 012 — Quadrinhos e mangá (CBZ)

**Prioridade:** média · **Área:** `domain/BookFormat.kt`, `data/importer/BookImporter.kt`, `ui/reader/cbz/`, `ui/reader/BookOpener.kt`, `domain/ReadingProgress.kt` · **Status:** feito (2026-11-02)

## Contexto
CBZ é o formato mais comum de quadrinhos e mangá digitais: um ZIP com uma imagem por página. Está no roadmap do CLAUDE.md e combina com o visual de quadrinhos do app. O leitor de PDF já resolve quase tudo que um leitor de imagens precisa (lista de páginas, zoom por pinça/duplo toque, cache de bitmaps). O CLAUDE.md tem o passo a passo "Como adicionar um formato novo (ex.: CBZ)".

## Objetivo
Importar arquivos `.cbz` e lê-los página a página, com zoom, progresso, retomada e meta diária, como um PDF.

## Comportamento esperado
- O seletor de arquivos passa a aceitar `.cbz`.
- Capa = primeira imagem do arquivo. Card mostra "Novo · CBZ · 24 págs".
- Leitor: mesmas barras e controles do PDF (página X / Y, zoom 1×–5×, duplo toque, tema de leitura no fundo). Páginas na ordem natural dos nomes ("page2" antes de "page10").
- Progresso, "Continuar lendo", retomada e meta diária funcionam igual ao PDF.
- Arquivo corrompido ou sem imagens: aviso de falha na importação (conta em `ImportResult.failed`).

## Critérios de aceite
- [ ] Importar um CBZ com 30 imagens: capa certa, "30 págs".
- [ ] Páginas "1.jpg … 10.jpg" aparecem em ordem numérica, não alfabética.
- [ ] Zoom e retomada da última página funcionam como no PDF.
- [ ] Ler 10 páginas de um CBZ bate a meta diária.
- [ ] Arquivos que não são imagem dentro do ZIP (`ComicInfo.xml`, `Thumbs.db`) são ignorados.

## Notas de implementação
- Seguir os 4 passos do CLAUDE.md: `BookFormat.CBZ` (MIME `application/vnd.comicbook+zip`, `application/x-cbz`; extensão `cbz`), extração em `BookImporter.importOne`, leitor em `ui/reader/cbz/` + despacho no `BookOpener`, regra em `ReadingProgress` + teste.
- Leitura do ZIP: o Readium (`streamer`, já no projeto) abre CBZ a partir de content URI como `Publication` de imagens (`readingOrder` = páginas). Usar `publication.get(link).read()` para os bytes e decodificar com `BitmapFactory` (com `inSampleSize` para a largura alvo). Evita copiar o arquivo e reimplementar o ZIP.
- Reaproveitar o leitor de PDF: extrair de `PdfReaderScreen` a parte genérica (lista de páginas + `PdfZoom` + re-render na largura do zoom) para um componente que recebe uma fonte de páginas (`PageSource { pageCount; suspend render(index, widthPx) }`). `PdfDocument` e o novo `CbzDocument` implementam essa interface.
- Rota Compose própria (`CbzReaderRoute(bookId)`) ou generalizar `PdfReaderRoute` para "leitor de páginas".
- `totalPages` preenchido na importação (como PDF). Meta diária: chave `DailyGoal.pdfPage(índice)` serve (renomear para `pageIndex` se ficar confuso).
- Ordem natural: comparador próprio que compara blocos numéricos como números.

## Testes (escrever antes)
- `BookFormatTest`: detecta CBZ por MIME e por extensão (provider devolvendo `application/octet-stream`).
- Unitário: comparador de ordem natural ("p2" < "p10", maiúsculas, sem números).
- Unitário: filtro de entradas (só `jpg/jpeg/png/webp/gif`, ignora `__MACOSX/`).
- `ReadingProgressTest`: progresso de CBZ igual ao do PDF.

## Fora do escopo
CBR (RAR), leitura direita→esquerda para mangá, modo página dupla, vídeo/áudio em quadrinhos.

## Resolução (2026-11-02)
- `BookFormat.CBZ` (MIME `application/vnd.comicbook+zip`, `application/x-cbz`; extensão `cbz`) e `BookFormat.isPaged` (PDF e CBZ: `totalPages`, "Página X de Y"). O seletor também pede `application/zip` e `application/octet-stream`, porque os provedores costumam anunciar o CBZ assim; um `.zip` qualquer continua recusado (decide pela extensão).
- Leitura do ZIP pelo **container do Readium** (`ReadiumServices.openArchive` → `ContainerAsset`), com acesso aleatório direto na URI do SAF e sem copiar o arquivo. Não usei o parser de quadrinhos do Readium (`Publication`) para não depender de como ele trata `ComicInfo.xml`/lixo no arquivo: a filtragem e a ordem são nossas.
- `domain/ComicPages`: `isPage` (só jpg/jpeg/png/webp/gif; ignora `__MACOSX/`, ocultos, `ComicInfo.xml`, `Thumbs.db`) e `NaturalOrder` (blocos numéricos comparados como números, sem estourar com números enormes; texto sem diferenciar maiúsculas).
- `ui/reader/PageSource` (interface: proporções, `maxRenderWidth`, `render`, `close`); `PdfDocument` e o novo `ui/reader/cbz/CbzDocument` implementam. `CbzDocument` mede cada página lendo só os primeiros 64 KB (cai para a imagem inteira se precisar), decodifica com `inSampleSize` para a largura pedida e guarda num LRU, com o mesmo teto de memória do PDF.
- Leitor: a rota `PdfReaderRoute` virou o "leitor de páginas" — o `PdfReaderViewModel` abre `PdfDocument` ou `CbzDocument` conforme o formato; zoom, duplo toque, tema, progresso, retomada e meta diária (chave `pdfPage(índice)`) servem aos dois sem duplicar código. `BookOpener` despacha PDF e CBZ para ela.
- Importação: capa = primeira página renderizada; ZIP ilegível ou sem imagens → `ImportResult.failed`.
- `ReadingProgress.forPages` (= regra do PDF).
- Testes: `BookFormatTest` (MIME, extensão com `octet-stream`/`zip`, `.zip` recusado), `ComicPagesTest` (ordem natural, zeros à esquerda, filtro), `ReadingProgressTest` (CBZ igual ao PDF).
- **Verificado no celular (moto g73):** CBZ de 12 imagens + `ComicInfo.xml` + `Thumbs.db` + `__MACOSX/` importou com a capa certa, abriu, páginas em ordem natural, "6 / 12", 50%, setores da meta enchendo, card "Página 6 de 12 · faltam 6". Faltou testar a pinça (zoom) e um CBZ grande.
