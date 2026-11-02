# Yachay

App Android de leitura (Kotlin + Jetpack Compose). "Yachay" significa "saber/aprender" em quéchua.
A UI e as strings estão em **português (pt-BR)**.

## Features (v1)
1. **Importar livros do dispositivo**: PDF e EPUB, pelo seletor do sistema (SAF). O arquivo fica onde está; o app guarda a URI com permissão persistida.
2. **Coleções**: agrupar livros já importados (um livro pode estar em várias coleções). Filtro por coleção na Biblioteca.
3. **Progresso de leitura**: barra em cada livro (`NeoProgressBar`), seção "Continuar lendo" e retomada da última posição.

Leitores embutidos: PDF via `PdfRenderer` nativo; EPUB via **Readium Kotlin Toolkit**.

## Build e execução
Requisitos: JDK 17, Android SDK com `platforms/android-35` (`local.properties` aponta o `sdk.dir`; não versionado).

```bash
./gradlew assembleDebug          # APK em app/build/outputs/apk/debug/
./gradlew installDebug           # instala no dispositivo/emulador conectado (adb)
./gradlew testDebugUnitTest      # testes unitários (JVM)
```

Versões fixadas em `gradle/libs.versions.toml`. Wrapper Gradle **8.9** + AGP **8.7.3** + Kotlin **2.0.21** (KSP `2.0.21-1.0.28`). Ao subir Kotlin, suba KSP junto (o prefixo da versão do KSP precisa bater com o Kotlin). Readium **3.0.3** exige `isCoreLibraryDesugaringEnabled = true` (já configurado).

## Arquitetura
Camadas simples, sem framework de DI: `AppContainer` (criado em `YachayApp`) instancia tudo uma vez por processo. Acesse com a extensão `context.appContainer`. ViewModels são criados com `viewModel { XViewModel(context.appContainer) }`.

```
app/src/main/java/com/yachay/app/
  YachayApp.kt / AppContainer.kt / MainActivity.kt
  domain/          BookFormat (formatos + detecção), ReadingProgress (regras de %)
  data/db/         Room: Entities.kt, BookDao, CollectionDao, YachayDatabase
  data/repo/       BookRepository, CollectionRepository
  data/importer/   BookImporter (SAF → metadados + capa → Room), CoverStore, ReadiumServices
  ui/theme/        Color (paleta), Type (Space Grotesk), Theme, NeoModifiers
  ui/components/   Neo* (componentes base), BookCard/BookCover, BookGrid
  ui/navigation/   Routes (type-safe, @Serializable), YachayNavHost (+ bottom bar)
  ui/library/      Biblioteca (grade, filtros, "Continuar lendo", importação)
  ui/collections/  Lista, detalhe, BookActionsSheet (marcar coleções / remover livro)
  ui/reader/       BookOpener (despacha por formato), pdf/, epub/
```

### Fluxo de leitura
- `rememberBookOpener()` (`ui/reader/BookOpener.kt`) é o único ponto que abre livros.
- **PDF** → rota Compose `PdfReaderRoute`. `PdfDocument` encapsula `PdfRenderer` (não thread-safe → `Mutex`) com cache LRU de bitmaps; páginas em `LazyColumn`.
- **Zoom do PDF** (`PdfReaderScreen` + `PdfZoom.kt`): sem lib externa. A `LazyColumn` inteira recebe `graphicsLayer` (escala + deslocamento, origem no topo-esquerdo); a rolagem vertical e o fling continuam sendo da lista (os toques chegam já em coordenadas escaladas). Pinça (1×–5×, ancorada entre os dedos) e arrasto horizontal são lidos no passe `Initial`; a pinça é consumida. Sobra vertical: na pinça vira `dispatchRawDelta`; na rolagem, um `NestedScrollConnection` move a janela ampliada quando a lista chega na borda. Duplo toque alterna 1× ↔ 2,5×. ~200 ms após o gesto, as páginas re-renderizam em `largura × zoom` (até 2,5×, 4096 px e 1/10 do heap por bitmap — `PdfDocument.maxRenderWidth`). Página atual = a que cruza o centro **da área visível** (`PdfZoom.visibleCenter`). Zoom não é salvo.
- **EPUB** → `EpubSessions.open(book)` abre a `Publication` (suspenso) **antes** de iniciar `EpubReaderActivity`. A activity instala a `FragmentFactory` do Readium antes de `super.onCreate` (exigência do `EpubNavigatorFragment`). Se o processo morreu e a sessão sumiu, a activity simplesmente fecha. A sessão é fechada em `onDestroy` quando `isFinishing`.

### Modelo de dados (Room, `yachay.db`, versão 1, sem export de schema)
- `books`: `uri` (único), `title`, `author?`, `format` (enum → TEXT), `coverPath?`, `totalPages?` (só PDF), `progress` (0f..1f), `lastPosition?`, `addedAt`, `lastOpenedAt?`
- `collections`: `name`, `createdAt`
- `book_collection`: (`bookId`, `collectionId`) com FK `CASCADE` nos dois lados
- DAOs expõem `Flow` → a UI reage sozinha a progresso/coleções. **Mudou o schema? Suba `version` e escreva uma `Migration`** (não use `fallbackToDestructiveMigration`: apagaria a biblioteca do usuário).

### Regras de progresso (`domain/ReadingProgress.kt`)
- PDF: `(páginaVisível + 1) / totalPáginas`; `lastPosition` = índice da página (String). Se a lista não rola mais, conta como última página.
- EPUB: `Locator.locations.totalProgression`; `lastPosition` = `Locator.toJSON()`.
- Concluído a partir de `0.995` (EPUB raramente chega a 1.0). O `%` exibido nunca mostra 100 antes disso.
- `progress` = **ponto mais longe já lido** (nunca diminui ao voltar páginas: `ReadingProgress.merge` / `MAX()` em `BookDao.updateProgress`); `lastPosition` = **posição atual**, usada para retomar. Um livro concluído continua "Lido" ao reler trechos. Só "Marcar como não lido" (sheet do livro) zera o progresso — e volta o livro a "Novo"; "Marcar como lido" põe 100%.
- "Lendo agora" mostra a posição atual (`positionLabel`) e a barra mostra o máximo; se a pessoa voltou, o rótulo ganha "· lido até X%".
- Gravações usam debounce e rodam em `container.appScope` para não se perderem ao fechar o leitor.

### Importação (`data/importer/BookImporter.kt`)
`takePersistableUriPermission` é obrigatório — sem ele a URI para de funcionar após reiniciar o app. Duplicatas são detectadas pela URI. Capas vão para `filesDir/covers/{id}.jpg` (PDF: página 1 renderizada; EPUB: `publication.cover()`). Remover um livro libera a permissão e apaga a capa, **nunca** o arquivo do usuário.

## Estilo visual — Neobrutalismo
Paleta "Energetic Neobrutalism Minimalist" (`ui/theme/Color.kt`, espelhada em `res/values/colors.xml`):

Paleta medida das ilustrações em `referencias/` (`ref1.png`, `ref2.png`): roxo elétrico + verde-limão + papel creme + tinta arroxeada. `ui/theme/Color.kt`, espelhada em `res/values/colors.xml`.

Tema **escuro único** (fundo `Night`). Princípio (adaptado da skill *taste*): **cada cor tem um papel**. Seleção (abas, filtros) é **limão**; não selecionado é **roxo profundo**. Sombras duras são **roxas** (`shadowFor(fill)`), e **limão** quando o elemento é roxo — tinta sumiria no escuro. O roxo é o campo da marca (barras superiores e o bloco "Lendo agora"); o limão é o destaque, como as coroas e o "zZZ" das referências.

| Token | Hex | Papel |
|---|---|---|
| `Purple` | `#7F1DF3` | barras superiores, bloco "Lendo agora", preenchimento do progresso sobre papel |
| `PurpleDeep` | `#5712AD` | identidade de coleção / capas sem imagem |
| `Lime` | `#92F728` | ação principal (FAB, "Continuar"), marca `YachayMark`, livro concluído, progresso sobre roxo |
| `Lilac` | `#EBB3EA` | identidade de coleção / capas sem imagem, estado vazio de Coleções |
| `Night` | `#1E0A3C` | **fundo de todas as telas** (e do leitor de PDF) |
| `Cream` | `#F5E5D2` | cards/plaquinhas (linhas de coleção, diálogos, sheet, snackbar); texto sobre Night e roxos |
| `CreamMuted` | creme 72% | texto secundário sobre Night/roxo |
| `Paper` | `#FFF7EC` | trilho da barra de progresso, campos de texto, capa vazia |
| `Ink` | `#0D0517` | texto sobre os cards creme/limão, contornos, checkbox da sheet |
| `InkMuted` | `#5E4F6E` | texto secundário dentro dos cards creme (≈6:1) |
| `Danger` | `#FF4D5E` | única cor fora das referências: só ações destrutivas e erro |

Barras do sistema com ícones claros nas duas (topo roxo, fundo Night) — `YachayStatusBar` / `YachayNavigationBar` em `MainActivity.kt`.

Regras:
- **Use sempre os componentes `Neo*`** (`NeoSurface`, `NeoPressable`, `NeoButton`, `NeoIconButton`, `NeoFab`, `NeoChip`, `NeoProgressBar`, `NeoTopBar`, `NeoTextField`, `NeoDialog`, `NeoEmptyState`, `NeoSnackbarHost`, `SectionHeader`, `YachayMark`). Nada de Material cru com elevação/ripple.
- Contorno `Ink` de 2.5dp, cantos de 4dp, sombra dura deslocada (4dp; 5-6dp nos objetos principais) sem blur. Clicáveis "afundam" sobre a sombra (`NeoPressable`).
- **As capas são os objetos**: na estante, a capa leva contorno + sombra e o texto fica solto embaixo, sem cartão (`BookCard`). Coleções mostram as capas em leque (`CoverFan`) sobre a cor da coleção.
- Cada tela responde uma pergunta com o maior elemento. Biblioteca: "onde eu parei?" → `ReadingNowCard` ("Página 13 de 30 · faltam 17", toque único para continuar).
- Estados honestos: livro nunca aberto mostra "Novo · PDF · 30 págs", não uma barra em 0%; resumos ("2 livros · 1 em andamento") vêm de `booksSummary()` calculado das linhas; zero tem texto próprio (plural pt-BR do Android trata 0 como "one").
- Números que mudam usam algarismos tabulares (`PercentTextStyle`, `TextStyle.tabular()`).
- Título de seção fica junto do conteúdo (12dp) e longe da seção anterior; alvos de toque ≥ 44dp.
- Cor do texto sobre fundo colorido: `contentColorFor(cor)` (creme sobre os roxos e a tinta). `NeoTopBar` repassa essa cor via `LocalContentColor`.
- Tipografia: Space Grotesk (fonte variável em `res/font/space_grotesk.ttf`), títulos em Bold; título de tela 24sp, seções 20sp.
- Um tema só, independente do modo escuro do celular. Dentro do leitor o fundo é neutro (creme escurecido no PDF); a cor fica nas barras.

### Ornamentos e texturas (linguagem de quadrinhos das referências)
Primitivas em `ui/theme/Textures.kt` (modifiers e shapes) e `ui/components/Ornaments.kt` (composables):

| Elemento | API | Onde é usado |
|---|---|---|
| Halftone em degradê (pontos em quincôncio, maiores num canto) | `Halftone(...)`, `Modifier.halftone()`, parâmetro `halftone` de `NeoSurface`/`NeoPressable`/`NeoTopBar` | barras superiores (canto superior direito), "Lendo agora", capas sem imagem, painel de cada coleção, estados vazios, diálogos |
| Tom da textura | `shadeFor(cor)` (+ `LimeDeep`, `CreamDeep`) | sempre o tom escuro da própria cor de fundo, como sombra impressa |
| Marcas de impacto "!!" | `ImpactMarks` | só "Lendo agora" e estados vazios |
| Brilho de 4 pontas | `Sparkle`, `SparkleShape` | capas sem imagem, barra da Biblioteca, estados vazios |
| Selo estourado | `BurstBadge`, `StarburstShape` | "Lido!" na capa de livros concluídos (só na estante) |
| Sublinhado grosso | `Modifier.marker()` | títulos de seção (`SectionHeader`): barra limão sob o texto creme |
| Listras diagonais | `Modifier.diagonalStripes()` | espaço vazio de capa no leque das coleções |
| Grão | `Modifier.paperGrain()` | fundo Night das listas (`BookGrid`, lista de coleções), em creme 6% — fixo, não rola |

Regra: ornamento entra onde já existe um campo de cor ou um momento de destaque, nunca solto no meio do conteúdo, e nunca por cima de texto. Texturas são desenhadas com `drawWithCache` (uma vez por tamanho) e cortadas nos limites do elemento. Ornamentos decorativos contrariam a skill *taste*; foram pedidos explicitamente para o app ficar "menos cru".

### Ajustes e tema de leitura
Terceira aba, **Ajustes** (`ui/settings/SettingsScreen.kt`, rota `SettingsRoute`). Hoje tem uma opção: **Tema de leitura** (`domain/ReadingTheme`: `PAPER` padrão, `NIGHT`), salva em DataStore (`data/settings/SettingsRepository`, chave `reading_theme`). Vale só para as páginas dos leitores — o app é sempre escuro.
- Cores: `ui/reader/ReadingColors.kt` (`ReadingTheme.colors()`: Papel = Cream/Ink, Noturno = Night/Cream).
- **EPUB**: `ReaderPreferences` (`domain/ReaderPreferences.kt`: tema + `fontScale` + `LineSpacing`) vem de um Flow único, `SettingsRepository.readerPreferences`, e vira `EpubPreferences` em `toEpubPreferences()` (`ui/reader/epub/EpubPreferencesMapping.kt`). As preferências iniciais são lidas em `EpubSessions.open` (o navegador precisa delas antes de a activity existir); mudanças posteriores chegam por `submitPreferences` (o Readium re-pagina mantendo o locator). A raiz da activity e os ícones da barra de navegação acompanham o tema.
- **Texto do EPUB**: botão **"Aa"** na barra do leitor abre `ReaderSettingsSheet` (fonte 70–250% em passos de 10% via `FontScale`, espaçamento Compacto/Normal/Amplo, atalho de tema); os mesmos controles (`ui/reader/ReaderTextControls.kt`) aparecem em Ajustes → "Texto dos EPUBs". Chaves DataStore `epub_font_scale` (float) e `epub_line_spacing` (string); valores inválidos caem no padrão. `fontSize` funciona com os estilos da editora; `lineHeight` **exige** `publisherStyles = false` no Readium 3.0.3 — por isso só desligamos os estilos da editora quando o espaçamento não é Normal.
- **PDF**: `ReadingTheme.pdfPageFilter()` no `Image` de cada página — Papel = multiplica pelo creme (mantém cores das imagens); Noturno = duotom por luminância (imagens viram duas cores). Páginas com contorno roxo profundo.
- Nova preferência: adicione chave + Flow em `SettingsRepository` e uma seção em `SettingsScreen` (com `SectionHeader`). `SettingsRepository` recebe o `DataStore` (`context.settingsStore`) para os testes usarem um arquivo temporário.

### Ícone do app
Ícone adaptativo (`res/mipmap-anydpi-v26/ic_launcher*.xml`): fundo `@color/yachay_purple`, frente `@mipmap/ic_launcher_art` (PNG em mdpi…xxxhdpi = 108…432px) recortado de `referencias/ref1.png`. A área visível do launcher (72 de 108dp) corresponde a 1080px da ilustração centrados em (800, 561) — personagem, "zZZ" e livros, com a moldura da ilustração fora do visível; o excedente é preenchido com o roxo. O ícone monocromático (ícones temáticos, Android 13+) é a silhueta vetorial `drawable/ic_launcher_monochrome.xml`. Para trocar a arte, regenere os 5 PNGs com o mesmo enquadramento (a camada inteira tem 1,5× a área visível).

## Convenções
- Strings visíveis sempre em `res/values/strings.xml` (pt-BR), com `plurals` para contagens.
- Cores sempre via tokens de `Color.kt`, nunca hex nas telas.
- IO (Room, SAF, PdfRenderer, Readium) fora da main thread (`Dispatchers.IO` / funções `suspend`).
- Sem permissões de armazenamento no manifest: todo acesso a arquivos é via SAF.
- Comentários em português, curtos, explicando o *porquê*.

## Como adicionar um formato novo (ex.: CBZ)
1. Entrada em `BookFormat` (MIME + extensões) — o seletor de arquivos já passa a aceitá-lo.
2. Extração de metadados/capa em `BookImporter.importOne` (`when (format)`).
3. Um leitor em `ui/reader/<formato>/` e o despacho em `BookOpener`.
4. Regra de progresso em `ReadingProgress` + teste em `app/src/test`.

## Testes
- Unitários (JVM) em `app/src/test`: `BookFormatTest`, `ReadingProgressTest`, `ReaderPreferencesTest`, `EpubPreferencesMappingTest`, `SettingsRepositoryTest` (DataStore em `TemporaryFolder`).
- `unitTests.isReturnDefaultValues = true`: enums do Readium (ex.: `Theme`) tocam APIs do Android ao carregar.
- Não há testes instrumentados ainda; validação de UI/leitores é manual em dispositivo (importar PDF e EPUB, ler, voltar, conferir %, reabrir o app e checar retomada; criar/renomear/excluir coleção, filtrar).

## Limitações / roadmap
- Formatos: CBZ, TXT, MOBI/AZW3 (MOBI não tem lib Kotlin madura).
- PDF sem busca; EPUB sem escolha de família de fonte, margens ou alinhamento.
- Sem marcadores, anotações, ordenação/busca na biblioteca, sincronização ou backup.
- Se o arquivo for movido/apagado fora do app, a abertura falha com aviso (o livro continua na lista).
