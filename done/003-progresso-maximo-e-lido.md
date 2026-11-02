# 003 — Progresso guarda o ponto mais longe lido ("Lido" não desmarca ao voltar)

**Prioridade:** alta (bug de percepção) · **Área:** `domain/ReadingProgress.kt`, `data/repo/BookRepository.kt`, leitores, `BookActionsSheet` · **Status:** feito (2026-11-02)

## Contexto
Hoje `books.progress` registra **onde o leitor está**. Se a pessoa termina um livro (100%, selo "Lido!") e volta uma página para reler um trecho, o progresso cai para 97%, o selo some e o livro volta para "em andamento". Reproduzido no celular durante os testes. O esperado é que o progresso represente **até onde já leu**, e que a retomada continue indo para **onde parou por último**.

## Objetivo
Separar os dois conceitos sem mudar o schema do banco:
- `progress` = **progresso máximo alcançado** (nunca diminui sozinho);
- `lastPosition` = **posição atual** (já existe: índice da página no PDF, `Locator` JSON no EPUB) — continua sendo usada para retomar.

## Comportamento esperado
- Ler para frente: `progress` sobe normalmente.
- Voltar páginas: `progress` **não** diminui; `lastPosition` acompanha a posição atual.
- Um livro concluído continua **Lido** (selo, barra limão, contagem "lidos") mesmo relendo trechos.
- O card **"Lendo agora"** mostra a posição atual ("Página 13 de 30 · faltam 17") a partir de `lastPosition`, e a barra mostra o progresso máximo. Avaliar se o rótulo deixa claro que são coisas diferentes (ex.: "Página 13 de 30 · lido até 43%") — decidir com o usuário.
- Nova ação na sheet do livro (`BookActionsSheet`): **"Marcar como não lido"** (zera `progress` e `lastPosition`) — é o único jeito de diminuir o progresso. E **"Marcar como lido"** para livros que a pessoa terminou fora do app.

## Critérios de aceite
- [ ] Ler o PDF de teste até 30/30, voltar para 29/30 → card continua "Lido" com selo; reabrir retoma na 29.
- [ ] EPUB: avançar até 50%, voltar a 40% → barra fica em 50%; reabrir retoma em 40%.
- [ ] "Marcar como não lido" zera e o livro sai de "lidos"; "Marcar como lido" põe 100% e o selo.
- [ ] Resumo do topo ("x em andamento · y lidos") reflete a regra nova.

## Notas de implementação
- A regra fica no domínio: `ReadingProgress.merge(stored: Float, current: Float): Float = max(stored, current)`. `BookRepository.saveProgress` passa a gravar `merge(...)` em `progress` e a posição atual em `lastPosition` — de preferência numa query única (`UPDATE books SET progress = MAX(progress, :p), lastPosition = :pos`) para não haver corrida entre leituras e escritas.
- `positionLabel()` (BookCard.kt) já calcula a página atual a partir de `lastPosition` no PDF; no EPUB, extrair `locations.totalProgression` do `Locator` salvo para o rótulo "x% lido" atual, se o rótulo mudar.
- Ações da sheet: `BookRepository.markUnread(id)` / `markRead(id)`; strings em `strings.xml`.
- Não exige mudança de schema (sem migração). Se no futuro for preciso guardar as duas porcentagens separadamente, fazer **depois** de ligar `exportSchema` e escrever o teste de migração.

## Testes (escrever antes)
- Unitário (TDD, é o primeiro ciclo sugerido): `merge(1f, 0.97f) == 1f`; `merge(0.4f, 0.5f) == 0.5f`; `isDone(merge(1f, 0.1f))`.
- Room (in-memory): `saveProgress` com valor menor mantém `progress` e atualiza `lastPosition`; `markUnread` zera os dois.
- Regressão: o cenário reproduzido no celular (100% → voltar → continua Lido).

## Fora do escopo
Histórico de sessões de leitura, estatísticas (tempo lido, páginas por dia), sincronização entre aparelhos.

## Resolução (2026-11-02)
- `ReadingProgress.merge` + testes (incluindo a regressão 30/30 → 29/30). `BookDao.updateProgress` grava `MAX(progress, :p)` e a posição atual numa query só.
- `BookDao/BookRepository.markRead` (progress = 1) e `markUnread` (zera progress, `lastPosition` **e** `lastOpenedAt`, para o livro voltar a "Novo" e sair de "Continuar lendo").
- Sheet do livro: botões "Marcar como lido" (se não concluído) e "Marcar como não lido" (se não for novo); a sheet observa o livro ao vivo.
- Rótulo do "Lendo agora": decidido sem consulta — mostra a posição atual (PDF: página; EPUB: "Em 40% do livro", lido do `Locator`) e acrescenta "· lido até 50%" só quando a pessoa voltou para trás. A barra mostra o máximo.
- Pendente: teste Room in-memory (exige Robolectric, que o projeto ainda não tem) e o roteiro manual no celular.
