import { useEffect, useMemo, useState } from 'react';
import { Button } from '@/components/ui/button';
import { Calendar } from '@/components/ui/calendar';
import { portalService } from '@/services/portal-service';
import { ptBR } from 'react-day-picker/locale';
import type { Candidate, InterviewPresence } from '@/types/domain';
import { presenceStatus, presenceTone } from '@/lib/status';
import { nowInBrasilia, initials, messageOf } from '@/lib/helpers';
import { Chip, Empty } from '@/components/common';

type AgendaFilter = 'all' | InterviewPresence;

const agendaFilters: [AgendaFilter, string][] = [['all', 'Todos'], ['confirmado', 'Confirmados'], ['pendente', 'Pendentes']];

/** AAAA-MM-DD do dia de um instante no horário de Brasília: liga a entrevista ao dia do calendário. */
const brasiliaDay = new Intl.DateTimeFormat('sv-SE', { timeZone: 'America/Sao_Paulo', year: 'numeric', month: '2-digit', day: '2-digit' });

const brasiliaTime = new Intl.DateTimeFormat('pt-BR', { timeZone: 'America/Sao_Paulo', timeStyle: 'short' });

const longDay = new Intl.DateTimeFormat('pt-BR', { weekday: 'long', day: 'numeric', month: 'long' });

/** Mesma chave para uma célula do calendário, que é uma data do fuso do navegador. */
const cellDay = (date: Date) => `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;

const dateOfDay = (key: string) => { const [year, month, day] = key.split('-').map(Number); return new Date(year, month - 1, day); };

/** Calendário de entrevistas: os dias com entrevista ficam marcados e o dia escolhido lista os candidatos, com a presença. */
export function HrAgendaPage({ reloadKey }: { reloadKey: number }) {
  const [filter, setFilter] = useState<AgendaFilter>('all');
  const [items, setItems] = useState<Candidate[] | null>(null); const [loadError, setLoadError] = useState<string | null>(null);
  // Dia e mês escolhidos pela pessoa; sem escolha, abre no próximo dia com entrevista.
  const [picked, setPicked] = useState<string | null>(null); const [month, setMonth] = useState<Date | undefined>();
  // Sobe a cada "Tentar novamente".
  const [attempt, setAttempt] = useState(0);
  useEffect(() => {
    let current = true;
    portalService.getAgenda(filter === 'all' ? undefined : filter)
      .then((next) => { if (current) { setItems(next); setLoadError(null); } })
      .catch((error) => { if (current) setLoadError(messageOf(error, 'Não foi possível carregar a agenda.')); });
    return () => { current = false; };
  }, [filter, reloadKey, attempt]);
  const choose = (next: AgendaFilter) => { setFilter(next); setItems(null); setLoadError(null); };
  const retry = () => { setLoadError(null); setAttempt((current) => current + 1); };
  // Entrevistas por dia e o que marca o dia: pendente se alguma presença ainda não foi confirmada.
  const { byDay, dayState } = useMemo(() => {
    const byDay = new Map<string, Candidate[]>(); const dayState = new Map<string, InterviewPresence>();
    for (const item of items ?? []) {
      if (!item.interviewAt) continue;
      const key = brasiliaDay.format(new Date(item.interviewAt));
      byDay.set(key, [...(byDay.get(key) ?? []), item]);
      if (item.presence !== 'confirmado') dayState.set(key, 'pendente'); else if (!dayState.has(key)) dayState.set(key, 'confirmado');
    }
    return { byDay, dayState };
  }, [items]);
  const days = [...byDay.keys()].sort(); const today = nowInBrasilia().slice(0, 10);
  const day = picked ?? days.find((key) => key >= today) ?? days[days.length - 1] ?? today;
  const dayItems = byDay.get(day) ?? [];
  return <section className="hr-page">
    <div className="page-head"><div><h1 className="display">Agenda</h1><p>Entrevistas marcadas, no horário de Brasília. Escolha um dia para ver os candidatos.</p></div></div>
    <div className="toolbar">
      <div className="segmented" role="group" aria-label="Filtrar por presença">{agendaFilters.map(([value, label]) => <button key={value} type="button" aria-pressed={filter === value} onClick={() => choose(value)}>{label}</button>)}</div>
    </div>
    {items && items.length > 0
      ? <div className="agenda">
          <div className="lineup"><Calendar className="agenda-cal" mode="single" required locale={ptBR} showOutsideDays={false} selected={dateOfDay(day)} onSelect={(next) => setPicked(cellDay(next))} month={month ?? dateOfDay(day)} onMonthChange={setMonth} modifiers={{ pending: (date) => dayState.get(cellDay(date)) === 'pendente', confirmed: (date) => dayState.get(cellDay(date)) === 'confirmado' }} modifiersClassNames={{ pending: 'event-pending', confirmed: 'event-confirmed' }} /></div>
          <section className="lineup agenda-day" aria-label="Entrevistas do dia">
            <h2 className="doc-subhead">{longDay.format(dateOfDay(day)).replace(/^./, (letter) => letter.toUpperCase())} <span>{dayItems.length}</span></h2>
            {dayItems.length > 0
              ? dayItems.map((item) => <div className="agenda-event" key={item.applicationId}>
                  <time className="tabular">{brasiliaTime.format(new Date(item.interviewAt ?? ''))}</time>
                  <div className="person"><span className="initials">{initials(item.name)}</span><span><strong>{item.name}</strong><small>{item.jobTitle}</small></span></div>
                  {item.presence && <Chip tone={presenceTone[item.presence]}>{presenceStatus[item.presence]}</Chip>}
                </div>)
              : <p className="doc-none">Nenhuma entrevista neste dia.</p>}
          </section>
        </div>
      : <div className="lineup">
          {items && (filter === 'all'
            ? <Empty title="Nenhuma entrevista marcada" text="Quando um candidato for chamado para entrevista, ela aparece aqui." />
            : <Empty title="Nenhuma entrevista neste filtro" text="Troque o filtro para ver as outras entrevistas." />)}
          {!items && loadError && <Empty title="Não foi possível carregar a agenda" text={loadError} action={<Button variant="outline" onClick={retry}>Tentar novamente</Button>} />}
          {!items && !loadError && <p className="empty muted">Carregando agenda…</p>}
        </div>}
  </section>;
}
