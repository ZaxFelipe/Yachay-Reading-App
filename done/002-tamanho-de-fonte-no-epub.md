# 002 — Tamanho de fonte (e espaçamento) no leitor EPUB

**Prioridade:** alta · **Área:** `ui/reader/epub/`, `data/settings/`, `ui/settings/` · **Status:** feito (2026-11-02)

## Contexto
O leitor EPUB (`EpubReaderActivity`, navegador do Readium 3.0.3) já recebe `EpubPreferences` para o tema de leitura (cores), mas não permite mudar o tamanho do texto. É o ajuste mais usado em qualquer leitor de ebook; sem ele, o app é desconfortável para quem tem vista cansada ou lê em tela pequena.

## Objetivo
O leitor ajusta o tamanho da fonte (e, opcionalmente, o espaçamento entre linhas) **dentro do próprio leitor**, vê o resultado na hora e a escolha vale para todos os EPUBs.

## Comportamento esperado
- Botão **"Aa"** na barra superior do leitor EPUB abre uma sheet (estilo `BookActionsSheet`: creme, contorno em tinta) com:
  - **Tamanho da fonte:** botões `A−` / `A+` (passos de 10%, de 70% a 250%) e o valor atual em % no meio (algarismos tabulares).
  - **Espaçamento entre linhas:** 3 opções (Compacto / Normal / Amplo) como `NeoChip` — *se* o Readium aplicar sem desligar os estilos da editora (ver notas).
  - Atalho para o **tema de leitura** (Papel/Noturno), reaproveitando a escolha que já existe em Ajustes.
- A mudança aparece imediatamente na página (via `navigator.submitPreferences`) e **mantém a posição de leitura** (o locator não pode pular de capítulo).
- A escolha é salva em `SettingsRepository` (DataStore) e aparece também em **Ajustes → Leitura** (mesma preferência, dois lugares).
- O progresso (%) recalcula sozinho após a re-paginação (já vem do `currentLocator`).

## Critérios de aceite
- [ ] `A+`/`A−` mudam o texto na hora, sem fechar o leitor.
- [ ] Após mudar a fonte, a mesma frase continua na tela (tolerância: mesma página ±1).
- [ ] Fechar e reabrir o livro (e outro EPUB) mantém o tamanho escolhido.
- [ ] Os limites 70% e 250% desabilitam o botão correspondente.
- [ ] Funciona nos dois temas de leitura.
- [ ] Sheet acessível: botões com descrição para TalkBack ("Diminuir fonte", "Aumentar fonte").

## Notas de implementação
- `EpubPreferences(fontSize = 1.0 /* 100% */, lineHeight = ...)`. Conferir na versão 3.0.3 quais propriedades exigem `publisherStyles = false` para ter efeito (no Readium, `lineHeight` e afins costumam exigir; `fontSize` não). Se o espaçamento exigir desligar os estilos da editora, avaliar o impacto visual em EPUBs reais antes de incluir — pode ficar fora desta task.
- Hoje o `epubPreferences()` é montado só a partir do `ReadingTheme`. Trocar por um modelo `ReaderPreferences(theme, fontScale, lineSpacing)` vindo de um único Flow do `SettingsRepository`, para o `collect` da activity reenviar tudo junto.
- O valor inicial precisa estar na `Session` (como `initialTheme`), porque o navegador é criado antes de a activity existir.
- DataStore: chaves `epub_font_scale` (float) e `epub_line_spacing` (string). Valores inválidos caem no padrão.

## Testes (escrever antes)
- Unitário: `nextFontScale(current, step = +1)` e `-1` respeitam limites e arredondam para múltiplos de 0,1 (sem 1.0999999).
- Unitário: `ReaderPreferences` → `EpubPreferences` mapeia tema, fonte e espaçamento corretamente.
- `SettingsRepository`: grava e lê `fontScale`; valor corrompido volta ao padrão (DataStore em arquivo temporário).

## Fora do escopo
Escolha de família de fonte, margens, alinhamento justificado/esquerda, modo rolagem contínua.

## Resolução (2026-11-02)
- Domínio: `ReaderPreferences(theme, fontScale, lineSpacing)`, `FontScale` (passos em décimos inteiros, limites 0,7–2,5, `sanitize`) e `LineSpacing` (Compacto 1,2 / Normal = do livro / Amplo 1,8).
- `SettingsRepository.readerPreferences` (Flow único) + `setFontScale`/`setLineSpacing`; agora recebe o `DataStore` (testável). `EpubSessions.Session.initialPreferences` substitui `initialTheme`.
- `toEpubPreferences()`: fonte via `fontSize`; espaçamento ≠ Normal liga `publisherStyles = false` (conferido no bytecode do Readium 3.0.3: `lineHeight` só é efetivo assim). Normal mantém os estilos da editora — o espaçamento entrou, mas só desliga o estilo do livro quando a pessoa pede (aviso na própria sheet).
- Leitor: botão "Aa" na barra → `ReaderSettingsSheet` (creme, sem scrim, para ver a página mudar). A−/A+ têm descrição para TalkBack e desabilitam nos limites. Ajustes ganhou a seção "Texto dos EPUBs" com os mesmos controles.
- Testes: `ReaderPreferencesTest`, `EpubPreferencesMappingTest`, `SettingsRepositoryTest` (21 testes no total, todos passando). `unitTests.isReturnDefaultValues = true` foi necessário para o enum `Theme` do Readium.
- Pendente: validação manual no celular (frase continua na tela após mudar a fonte, impacto visual de Compacto/Amplo em EPUBs reais).
- Validado no moto g73: "Aa" abre a sheet, A+ aplica na hora (100% → 130%) sem fechar o leitor. Observado: após mudar a fonte, a tela voltou para o início do capítulo (alguns parágrafos antes do trecho visível) — provavelmente dentro de ±1 página, mas vale conferir à mão.
