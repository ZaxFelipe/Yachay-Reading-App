# 001 — Zoom no leitor de PDF

**Prioridade:** alta (maior lacuna de uso hoje) · **Área:** `ui/reader/pdf/` · **Status:** feito (2026-11-02)

## Contexto
O leitor de PDF (`PdfReaderScreen.kt`) mostra cada página na largura da tela, numa `LazyColumn`, sem nenhuma forma de ampliar. Em celular, PDFs de livro, artigo ou apostila (texto pequeno, duas colunas, notas de rodapé) ficam ilegíveis. É o motivo mais provável de alguém desinstalar o app.

## Objetivo
O leitor consegue ampliar qualquer parte da página e ler com conforto, sem perder a rolagem contínua entre páginas nem o registro de progresso.

## Comportamento esperado
- **Pinça** amplia e reduz, entre 1× e 5×, ancorada no ponto entre os dedos.
- **Duplo toque** alterna entre 1× e 2,5× centrado no ponto tocado; duplo toque ampliado volta a 1×.
- Ampliado, **arrastar** move a página nos dois eixos; ao chegar na borda vertical, a rolagem continua para a página seguinte/anterior (não fica "presa" na página).
- Em 1×, a rolagem vertical funciona exatamente como hoje.
- **Nitidez:** ao parar de ampliar (≈200 ms sem gesto), as páginas visíveis são re-renderizadas na resolução do zoom atual (limitada por memória), em vez de esticar o bitmap de 1×.
- O indicador "13 / 30" e o progresso continuam corretos ampliados (página atual = a que cruza o centro da tela).
- O zoom **não** é salvo entre sessões (reabrir o livro começa em 1×).

## Critérios de aceite
- [ ] Pinça e duplo toque funcionam no PDF de teste e num PDF real de texto pequeno, num celular físico.
- [ ] Texto de corpo 8pt fica legível a 2,5× (sem borrão depois do re-render).
- [ ] Rolar ampliado da página 5 até a 6 é um gesto contínuo.
- [ ] Progresso salvo e retomada de posição continuam funcionando (roteiro manual do CLAUDE.md).
- [ ] Sem `OutOfMemoryError` ao ampliar 5× num PDF de 300+ páginas (ver memória no logcat).
- [ ] Funciona nos dois temas de leitura (o `ColorFilter` continua aplicado).

## Notas de implementação
- Opção recomendada: aplicar a transformação (escala + deslocamento) **no container da lista**, via `Modifier.pointerInput` + `detectTransformGestures`/`awaitEachGesture` e `graphicsLayer`, mantendo a `LazyColumn` para a rolagem vertical. Repassar o deslocamento vertical excedente para o `LazyListState.dispatchRawDelta` quando a página chega na borda.
- Alternativa a avaliar: uma lib de zoom para Compose (ex.: `net.engawapg.lib:zoomable` / `me.saket.telephoto`). Antes de adicionar, conferir tamanho, licença e se convive com a `LazyColumn`; registrar a decisão no CLAUDE.md.
- Re-render: `PdfDocument.render(index, widthPx)` já aceita a largura; passar `larguraTela × zoom` (com teto, ex.: 2.5× ou 4096 px) e ajustar o LRU (`cacheSizeBytes`) para não estourar.
- O duplo toque não pode conflitar com o long-press nem com os toques das bordas (o PDF não tem virada por toque hoje, então não há conflito).

## Testes (escrever antes — ver convenção de TDD)
- Unitário: função pura `clampZoom(scale, min=1f, max=5f)` e `offsetLimits(scale, viewport, content)` (o deslocamento nunca mostra além da borda da página).
- Unitário: `renderWidthFor(viewportPx, scale, maxPx)` respeita o teto.
- Regressão: a página atual continua sendo a que cruza o centro com zoom ≠ 1.

## Fora do escopo
Modo página única (virar página horizontalmente), busca de texto, seleção/cópia de texto.

## Resolução (2026-11-02)
- Opção recomendada, sem lib de zoom: `graphicsLayer` na `LazyColumn`; pinça e arrasto horizontal no passe `Initial`; sobra vertical → `dispatchRawDelta` (pinça) ou `NestedScrollConnection` (rolagem nas bordas da lista). A lista mantém rolagem e fling.
- Duplo toque 1× ↔ 2,5× animado, ancorado no ponto tocado.
- Re-render 200 ms após o gesto, em `renderWidthFor(largura, zoom ≤ 2,5×, maxRenderWidth)`; `maxRenderWidth` limita a 4096 px e a 1/10 do heap por bitmap. O bitmap anterior fica na tela enquanto o novo renderiza (sem piscar).
- Página atual = a que cruza o centro da área visível com zoom (`PdfZoom.visibleCenter`/`pageAtCenter`).
- Testes: `PdfZoomTest` (8 testes: clampZoom, offsetLimits, foco fixo, renderWidthFor, teto de memória, página atual com e sem zoom). Suíte total: 29 testes passando.
- Validado no moto g73 (adb): duplo toque amplia e texto fica nítido após re-render; arrasto horizontal ampliado; rolagem ampliada passa da página 1 à 4 de forma contínua com indicador correto; duplo toque volta a 1×; progresso salvo ("Página 4 de 30").
- Pendente (manual, exige dedos): pinça real, PDF de 300+ págs a 5× observando memória no logcat, e tema Noturno ampliado.
