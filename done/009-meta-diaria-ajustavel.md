# 009 — Meta diária ajustável

**Prioridade:** alta · **Área:** `domain/DailyGoal.kt`, `domain/ReaderLevel.kt`, `data/goals/DailyGoalRepository.kt`, `ui/settings/SettingsScreen.kt`, `ui/components/NeoComponents.kt` (`DailyGoalSegments`) · **Status:** feito (2026-11-02)

## Contexto
A meta é fixa em 10 páginas (`DailyGoal.DEFAULT_TARGET`, "fixa por enquanto"). Para quem lê pouco, 10 desanima; para quem lê muito, é fácil demais.

## Objetivo
Escolher a meta em Ajustes: 5, 10, 20 ou 30 páginas por dia, com o XP proporcional ao tamanho da meta.

## Comportamento esperado
- Ajustes ganha a seção "Meta diária" com 4 opções (cards ou chips, a escolhida em limão) e uma linha explicando: "Cada meta batida vale 10 XP por página da meta".
- A barra de setores (`DailyGoalSegments`) mostra um setor por página da meta. Com 20 ou 30, os setores ficam finos; se não couber, agrupar (30 → 10 setores de 3 páginas).
- "Quase lá" continua em 60% da meta nova.
- **Mudar no meio do dia:** vale na hora. Se as páginas de hoje já passam do novo alvo, a meta conta como batida na próxima página registrada (sem disparar retroativamente).
- **XP:** meta de N páginas vale `N × 10` XP (5 → 50, 10 → 100, 20 → 200, 30 → 300). Assim reduzir a meta não vira atalho para subir de nível.

## Critérios de aceite
- [ ] Trocar para 5 páginas: a barra mostra 5 setores no leitor e no "Lendo agora".
- [ ] Bater a meta de 20 dá 200 XP; a de 5 dá 50.
- [ ] Baixar a meta no mesmo dia e bater de novo não dá XP duas vezes no mesmo dia.
- [ ] Atualizar da 0.3.0 preserva nível e XP de quem já tinha metas batidas.

## Notas de implementação
- O XP deixa de ser `goalsMet * 100`: passar a **guardar o XP** (`xp` no DataStore `daily_goal`). Migração: se `xp` não existir, `xp = goals_met * 100`. `goals_met` continua existindo para o Perfil.
- Guardar no dia se a meta já foi batida hoje (`goal_met_day`), para não pagar duas vezes ao trocar o alvo.
- O alvo fica no próprio `daily_goal` (chave `target`), para ser lido na mesma transação de `recordPage`. `progress: Flow` passa a usar o alvo salvo.
- `DailyGoalProgress(target = ...)` já recebe o alvo; trocar os usos de `DEFAULT_TARGET` pelo valor salvo.
- Se a task 008 (sequência) entrar junto, o bônus de 7 dias continua fixo em 100 XP ou vira "uma meta de bônus"? Decidir antes e registrar aqui.

## Testes (escrever antes)
- `DailyGoalTest`: `almostThereAt` e `reachesTarget` para 5, 20, 30.
- `DailyGoalRepositoryTest`: XP proporcional; trocar alvo no meio do dia; sem XP duplicado; migração de `goals_met` → `xp`.
- `ReaderLevelTest`: nível calculado a partir do XP salvo.

## Fora do escopo
Meta em minutos, metas diferentes por dia da semana.

## Resolução (2026-11-02)
- `DailyGoal.TARGETS = [5, 10, 20, 30]`, `sanitizeTarget`, `xpFor(target) = target × 10`. Alvo salvo no próprio `daily_goal` (chave `target`), lido na transação de `recordPage`; `progress` e `target` (Flow) usam o valor salvo.
- XP passou a ser **guardado** (chave `xp`). Sem a chave (vindo da 0.3.0): `goals_met × 100`. `ReaderStats.xp` virou parâmetro (padrão = `goalsMet × 100`, o que mantém os testes antigos) e `goalsToNextLevel` usa o XP da meta atual (`goalTarget`).
- Sem XP duplicado: `goal_met_day` guarda o dia em que a meta foi paga. Meta batida = `!pagaHoje && páginas >= alvo`, então baixar o alvo no meio do dia conta na **próxima página registrada**. Legado: na primeira página desta versão, se o dia salvo já tinha 10 páginas (meta fixa da 0.3.0), `goal_met_day` é congelado nesse dia (senão `""`), para não pagar de novo nem travar alvos maiores.
- Barra de setores: `DailyGoal.segments/pagesPerSegment/filledSegments` — até 10 páginas, um setor por página; 20 → 10 setores de 2; 30 → 10 de 3 (só setor completo pinta).
- Ajustes: seção "Meta diária" com 4 cards (escolhido em limão, `Role.RadioButton`, descrição para TalkBack "20 páginas por dia, 200 XP").
- Decisão (pergunta das notas): o bônus da sequência (task 008) é **fixo em 100 XP**, independente do alvo.
- Testes: `DailyGoalTest` (60% e alvo para 5/20/30, XP, setores, sanitize), `DailyGoalRepositoryTest` (XP proporcional, troca de alvo no meio do dia, sem XP duplicado, migração 0.3.0 → `xp`), `ReaderLevelTest` (nível pelo XP salvo).
- Pendente: roteiro manual no celular.
