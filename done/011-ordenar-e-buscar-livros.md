# 011 — Ordenar e buscar livros na Biblioteca

**Prioridade:** média · **Área:** `ui/library/`, `data/settings/` · **Status:** feito (2026-11-02)

## Contexto
A estante "Todos os livros" segue a ordem de importação (`addedAt DESC`) e não tem busca. Com dezenas de livros, achar um título vira rolagem. Está no roadmap do CLAUDE.md ("ordenação/busca na biblioteca").

## Objetivo
Ordenar a estante por critérios úteis e filtrar por título ou autor digitando.

## Comportamento esperado
- **Ordenação:** botão na linha de "Todos os livros" abre um menu/sheet com: Adicionados recentemente (padrão), Abertos recentemente, Título (A–Z), Autor (A–Z), Progresso (mais lidos primeiro). A escolha fica salva.
- **Busca:** ícone de lupa na barra superior da Biblioteca. Ao tocar, aparece um `NeoTextField` na barra; a estante filtra enquanto digita (título e autor, sem diferenciar maiúsculas e acentos).
- Durante a busca, "Lendo agora" e "Também em andamento" somem; fica só a estante filtrada com a contagem ("3 resultados").
- Sem resultados: estado vazio próprio ("Nenhum livro com “x”").
- Busca e ordenação valem junto com o filtro de coleção.
- Voltar (gesto ou botão) fecha a busca e limpa o texto.

## Critérios de aceite
- [ ] Ordenar por título coloca "Árvore" junto dos "A" (ordem ciente de acentos, pt-BR).
- [ ] Buscar "machado" encontra livros com autor "Machado de Assis".
- [ ] A ordenação escolhida continua depois de fechar o app.
- [ ] Livros sem autor vão para o fim na ordenação por autor.

## Notas de implementação
- Listas são pequenas: ordenar e filtrar em memória no `LibraryViewModel` (um `combine` com `query` e `sort`), sem queries novas no DAO.
- Ordenação de texto com `java.text.Collator.getInstance(Locale("pt", "BR"))`; normalização para a busca com `Normalizer` (NFD + remover marcas).
- `SettingsRepository`: chave `library_sort` (string do enum; inválido → padrão).
- O `combine` da Biblioteca já está no limite de 5 fluxos tipados; agrupar `query`+`sort` num fluxo só ou adicionar um terceiro passo, como já é feito com meta e nível.

## Testes (escrever antes)
- Unitário: `sortBooks(list, sort)` para cada critério (acentos, autor nulo, empates por título).
- Unitário: `matches(book, query)` com acentos e maiúsculas.
- `SettingsRepositoryTest`: `library_sort` grava, lê e cai no padrão se inválido.

## Fora do escopo
Busca no conteúdo dos livros (ver 005), filtros por formato ou status (Lido/Novo).

## Resolução (2026-11-02)
- `domain/LibrarySort` (ADDED padrão, OPENED, TITLE, AUTHOR, PROGRESS) salvo em `SettingsRepository.librarySort` (chave `library_sort`; inválido → padrão).
- `ui/library/LibraryFilter.kt`: `sortBooks` com `Collator` pt-BR ("Árvore" junto dos "A"), sem autor no fim, nunca abertos no fim em "Abertos recentemente", empates pelo título; `matches` em título/autor via `domain/TextSearch` (NFD sem marcas + minúsculas — a mesma normalização da busca no livro, task 005).
- `LibraryViewModel`: busca (`query`, null = fechada) e ordem entram no segundo `combine` junto com meta/nível, aplicadas sobre a estante já filtrada pela coleção.
- Tela: lupa na barra (só com livros) abre `NeoTextField` no lugar do subtítulo, com foco; X ou voltar (`BackHandler`) fecham e limpam. Durante a busca somem "Lendo agora" e "Também em andamento"; o título vira "3 resultados"; sem resultado, estado vazio "Nenhum livro com “x”". Botão de ordenar (`YachayIcons.Sort`) na linha de "Todos os livros" abre `SortSheet` (opções em `Role.RadioButton`, escolhida em limão).
- Novos componentes compartilhados: `NeoSheet` (sheet creme padrão), `YachayIcons` (vetores que faltam no `material-icons-core`), parâmetro `action` em `SectionHeader`.
- Testes: `LibraryFilterTest` (cada critério, acentos, autor nulo/vazio, empates; busca com acentos/maiúsculas), `TextSearchTest`, `SettingsRepositoryTest` (`library_sort`).
- Pendente: conferir no celular (instalado por cima da 0.3.0 sem crash na abertura).
