# 010 — Calendário de leitura no Perfil

**Prioridade:** média · **Área:** `data/db/` ou `data/goals/`, `ui/profile/` · **Status:** feito (2026-11-02)

## Contexto
O Perfil mostra totais, mas não o ritmo: em que dias a pessoa leu e quanto. O `daily_goal` só guarda o dia atual, então hoje não existe histórico.

## Objetivo
Guardar quantas páginas foram lidas em cada dia e mostrar um mapa de quadradinhos (um por dia) no Perfil, no estilo do gráfico de contribuições do GitHub.

## Comportamento esperado
- Seção "Calendário" no Perfil, depois dos cards de totais: grade de 7 linhas (dom→sáb) × últimas ~17 semanas, rolando horizontalmente até o começo do histórico.
- Cor de cada dia: sem leitura = `PurpleDeep`; leu mas não bateu a meta = `Purple`; meta batida = `Lime`. Hoje com contorno destacado.
- Tocar num dia mostra "12 de out · 14 páginas · meta batida".
- Rótulos dos meses em cima; legenda "Menos ▢▢▢ Mais" embaixo.
- O histórico começa no dia em que esta versão for instalada (antes disso não há dados; dias vazios, sem texto falso).

## Critérios de aceite
- [ ] Ler 4 páginas hoje: o quadrado de hoje fica roxo; bater a meta: fica limão.
- [ ] Virar o dia: o quadrado de ontem mantém a cor, o de hoje começa vazio.
- [ ] Tocar num dia passado mostra as páginas certas.
- [ ] TalkBack lê cada dia com data e páginas.

## Notas de implementação
- Armazenamento: tabela Room `reading_days` (`date` TEXT PK ISO, `pages` INT, `goalMet` INT). **Schema muda:** subir `version` e escrever a `Migration` (combinar a ordem com a task 004 se as duas estiverem abertas).
- Alternativa sem Room: DataStore com uma chave por dia não escala bem; preferir Room.
- Gravar em `recordPage`: hoje o DataStore e o Room são transações separadas. Atualizar o dia no Room depois do `store.edit` (upsert com `pages` = páginas do dia, `goalMet` = alvo atingido). Se falhar, a próxima página corrige (valor absoluto, não incremento).
- Grade: `Canvas` ou `LazyRow` de colunas; cálculo das semanas em função pura (`CalendarGrid.weeks(today, firstDay)`), testável.
- A task 008 (sequência) pode derivar a sequência desta tabela.

## Testes (escrever antes)
- `CalendarGridTest`: semanas começando no domingo, virada de mês/ano, ano bissexto.
- DAO: upsert do mesmo dia substitui; consulta por intervalo de datas.
- Migração do schema.

## Fora do escopo
Estatísticas por livro, gráfico de páginas por semana, exportar histórico.

## Resolução (2026-11-02)
- Room `reading_days` (`date` TEXT PK ISO, `pages`, `goalMet`) + `ReadingDayDao` (`@Upsert`, intervalo de datas, primeiro dia). Banco foi para a **versão 2** numa migração única `Migrations.MIGRATION_1_2`, que cria também `annotations` (task 004) — combinadas para sair numa versão só.
- `exportSchema = true` + `room.schemaLocation` (`app/schemas/.../1.json` e `2.json` versionados). `MigrationsTest` roda a SQL num SQLite da JVM (`sqlite-jdbc`, só em teste) e compara colunas, FKs e índices com o schema exportado; confere que livros/coleções sobrevivem, cascata das anotações e upsert do dia.
- Gravação: `DailyGoalRepository` recebe um `ReadingHistory` (interface; no app é `ReadingHistoryRepository.record`) e grava o valor **absoluto** do dia depois do `store.edit` — se falhar, a próxima página corrige.
- Grade: `domain/CalendarGrid` (`weekStart` no domingo, `weeks(today, firstDay)` com no mínimo 17 semanas e crescendo até o começo do histórico, `monthStarts` para os rótulos).
- Perfil: seção "Calendário" (`ui/profile/ReadingCalendar.kt`): `LazyRow` invertida (abre na semana atual), cores PurpleDeep / Purple / Lime, hoje com contorno limão, dia tocado com contorno creme e resumo embaixo ("12 de out · 14 páginas · meta batida"), rótulos de mês, legenda "Menos ▢▢▢ Mais". Cada dia tem `contentDescription` com data e páginas (TalkBack).
- Concessão: as células têm 16dp (o mapa não caberia com 44dp por dia); o resumo embaixo repete a informação do dia escolhido.
- Testes: `CalendarGridTest` (domingo, virada de ano, crescer até o primeiro dia, 29/02, rótulos de mês), `MigrationsTest`, histórico no `DailyGoalRepositoryTest`.
- Pendente: roteiro manual no celular.
