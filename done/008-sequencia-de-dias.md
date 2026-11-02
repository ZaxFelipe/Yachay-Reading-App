# 008 — Sequência de dias batendo a meta

**Prioridade:** alta · **Área:** `domain/DailyGoal.kt`, `domain/ReaderLevel.kt`, `data/goals/DailyGoalRepository.kt`, `ui/profile/`, `ui/library/` · **Status:** feito (2026-11-02)

## Contexto
A meta diária e o XP (0.3.0) premiam cada dia isolado. Uma sequência ("7 dias seguidos") é o que mais motiva voltar no dia seguinte. Hoje o `daily_goal` só guarda o dia atual, então não há como saber se ontem a meta foi batida.

## Objetivo
Contar quantos dias seguidos a pessoa bateu a meta, mostrar isso no app e dar um bônus de XP a cada 7 dias.

## Comportamento esperado
- **Perfil:** card "🔥 5 dias seguidos" ao lado de "páginas lidas"/"metas batidas", com "Recorde: 12" embaixo.
- **Biblioteca:** subtítulo da barra superior ganha "· 5 dias seguidos" quando a sequência ≥ 2.
- A sequência conta **dias com meta batida**. Bater hoje soma 1. Se o último dia batido foi anteontem ou antes, a sequência mostrada é 0 (mesmo sem abrir o app ontem).
- Hoje ainda sem meta e ontem batida: mostra a sequência de ontem (não zera antes do fim do dia).
- **Bônus:** ao completar 7, 14, 21… dias, +100 XP extra. O aviso da meta vira "Meta batida! 7 dias seguidos" (prioridade: avatar novo > sequência múltipla de 7 > meta normal).

## Critérios de aceite
- [ ] Bater a meta 3 dias seguidos → 3. Pular um dia → no dia seguinte mostra 0; batendo a meta → 1.
- [ ] Recorde nunca diminui.
- [ ] O 7º dia dá 100 XP extra uma única vez.
- [ ] Atualizar da 0.3.0 começa com sequência 1 se a meta de hoje já estava batida, senão 0.

## Notas de implementação
- DataStore `daily_goal`, na **mesma transação** de `recordPage`: `streak_last_day` (data ISO do último dia batido), `streak_count`, `streak_best`, `bonus_xp`.
- Regra pura em `DailyGoal`/`Streak` (testável): `onGoalMet(lastDay, count, today)` e `current(lastDay, count, today)`.
- `ReaderStats.xp = goalsMet * XP_PER_GOAL + bonusXp`. Atenção: o desbloqueio de avatar (`unlockedLevel`) precisa considerar o bônus também.
- Se a task 010 (calendário/histórico) vier antes, derivar a sequência do histórico em vez de chaves próprias.
- Se a task 009 (meta ajustável) vier antes, a sequência continua sendo "dias com a meta do dia batida", qualquer que seja o alvo.

## Testes (escrever antes)
- `StreakTest`: dias consecutivos, buraco de 1 dia, virada de mês/ano, recorde.
- `DailyGoalRepositoryTest`: relógio falso avançando dias; bônus no 7º dia; `unlockedLevel` com bônus.

## Fora do escopo
"Congelar" a sequência (dia de folga), notificações lembrando de ler.

## Resolução (2026-11-02)
- Regra pura em `domain/Streak.kt`: `onGoalMet(lastDay, count, today)`, `current(...)` (hoje sem meta e ontem batida ainda vale; anteontem ou antes = 0), `isBonus(count)` (múltiplos de 7), `BONUS_XP = 100`.
- Chaves no `daily_goal`, gravadas na mesma transação de `recordPage`: `streak_last_day`, `streak_count`, `streak_best`. O bônus soma direto no `xp` guardado (task 009), sem chave `bonus_xp` separada. O desbloqueio de avatar compara o nível pelo XP antes/depois, já com o bônus.
- Sequência guardada em chaves próprias (não derivada do histórico da 010): a sequência precisa estar na mesma transação do XP, e o histórico fica no Room.
- Atualização da 0.3.0: sem as chaves, a sequência deriva do dia em que a meta foi paga (`goal_met_day`/legado) → 1 se a meta de hoje já estava batida, senão 0.
- `PageRecorded.streakBonus`; o aviso vira "Meta batida! 7 dias seguidos" (prioridade: avatar novo > sequência múltipla de 7 > meta normal). Os dois leitores guardam o `PageRecorded` do último aviso.
- Perfil: card "🔥 N / dias seguidos / Recorde: X" ao lado de páginas e metas. Biblioteca: subtítulo ganha "· N dias seguidos" com sequência ≥ 2.
- Testes: `StreakTest` (consecutivos, buraco, virada de mês/ano, bissexto, bônus), `DailyGoalRepositoryTest` (3 dias → buraco → 0 → 1, recorde, bônus só no 7º, avatar desbloqueado pelo bônus, migração com e sem meta de hoje).
- Pendente: roteiro manual no celular.
