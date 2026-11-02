package com.yachay.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.yachay.app.R
import com.yachay.app.domain.CalendarDay
import com.yachay.app.domain.CalendarGrid
import com.yachay.app.ui.theme.Cream
import com.yachay.app.ui.theme.CreamMuted
import com.yachay.app.ui.theme.Ink
import com.yachay.app.ui.theme.Lime
import com.yachay.app.ui.theme.Purple
import com.yachay.app.ui.theme.PurpleDeep
import com.yachay.app.ui.theme.tabular
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Cell = 16.dp
private val Gap = 3.dp
private val CellShape = RoundedCornerShape(2.dp)
private val PtBr = Locale("pt", "BR")
// O pt-BR do Java abrevia com ponto ("out."); nos rótulos curtos ele só pesa.
private val DayFormat = DateTimeFormatter.ofPattern("d 'de' MMM", PtBr)
private val MonthFormat = DateTimeFormatter.ofPattern("MMM", PtBr)

/**
 * Mapa de dias lidos: uma coluna por semana (dom → sáb), a semana atual na direita. Sem leitura
 * = roxo profundo; leu = roxo; meta batida = limão. Tocar num dia mostra o resumo dele embaixo.
 */
@Composable
fun ReadingCalendar(
    weeks: List<List<LocalDate>>,
    days: Map<LocalDate, CalendarDay>,
    today: LocalDate,
    modifier: Modifier = Modifier,
) {
    var selectedEpoch by rememberSaveable { mutableStateOf(today.toEpochDay()) }
    val selected = LocalDate.ofEpochDay(selectedEpoch)
    val monthStarts = remember(weeks) { CalendarGrid.monthStarts(weeks).toSet() }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // reverseLayout: começa mostrando a semana atual; rolar para a esquerda volta no tempo.
        val ordered = remember(weeks) { weeks.withIndex().reversed() }
        // O respiro no fim (à direita) deixa caber o rótulo do mês da semana atual ("nov").
        LazyRow(
            reverseLayout = true,
            horizontalArrangement = Arrangement.spacedBy(Gap),
            contentPadding = PaddingValues(end = 16.dp),
        ) {
            itemsIndexed(ordered, key = { _, week -> week.index }) { _, (index, week) ->
                Column(verticalArrangement = Arrangement.spacedBy(Gap)) {
                    Box(Modifier.width(Cell).height(18.dp)) {
                        if (index in monthStarts) {
                            Text(
                                week.first().format(MonthFormat).trimEnd('.'),
                                style = MaterialTheme.typography.labelSmall,
                                color = CreamMuted,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.wrapContentWidth(Alignment.Start, unbounded = true),
                            )
                        }
                    }
                    week.forEach { date ->
                        DayCell(
                            date = date,
                            day = days[date],
                            isToday = date == today,
                            isFuture = date.isAfter(today),
                            isSelected = date == selected,
                            onClick = { selectedEpoch = date.toEpochDay() },
                        )
                    }
                }
            }
        }
        Text(
            dayLabel(selected, days[selected]),
            style = MaterialTheme.typography.bodyMedium.tabular(),
            color = Cream,
        )
        Legend()
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    day: CalendarDay?,
    isToday: Boolean,
    isFuture: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    if (isFuture) {
        Box(Modifier.size(Cell))
        return
    }
    val description = dayLabel(date, day)
    val border = when {
        isSelected -> Cream
        isToday -> Lime
        else -> Ink
    }
    Box(
        Modifier
            .size(Cell)
            .background(colorFor(day), CellShape)
            .border(if (isSelected || isToday) 2.dp else 1.dp, border, CellShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = description
                selected = isSelected
            },
    )
}

private fun colorFor(day: CalendarDay?): Color = when {
    day == null || day.pages == 0 -> PurpleDeep
    day.goalMet -> Lime
    else -> Purple
}

/** "12 de out · 14 páginas · meta batida" (também é o que o TalkBack lê em cada dia). */
@Composable
private fun dayLabel(date: LocalDate, day: CalendarDay?): String {
    val dateText = date.format(DayFormat).replace(".", "")
    val pages = day?.pages ?: 0
    val pagesText = if (pages == 0) {
        stringResource(R.string.calendar_no_reading)
    } else {
        pluralStringResource(R.plurals.calendar_pages, pages, pages)
    }
    return if (day?.goalMet == true) {
        stringResource(R.string.calendar_day_goal_met, dateText, pagesText)
    } else {
        stringResource(R.string.calendar_day, dateText, pagesText)
    }
}

@Composable
private fun Legend() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Gap)) {
        Text(
            stringResource(R.string.calendar_less),
            style = MaterialTheme.typography.labelSmall,
            color = CreamMuted,
            modifier = Modifier.padding(end = 4.dp),
        )
        listOf(PurpleDeep, Purple, Lime).forEach {
            Box(Modifier.size(12.dp).background(it, CellShape).border(1.dp, Ink, CellShape))
        }
        Text(
            stringResource(R.string.calendar_more),
            style = MaterialTheme.typography.labelSmall,
            color = CreamMuted,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}
