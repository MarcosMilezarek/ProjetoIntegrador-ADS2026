import { useEffect, useRef, useState } from 'react';
import type { ChangeEvent } from 'react';
import { Check, Download, FileText, Upload, X } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { portalService } from '@/services/portal-service';
import { formatDate } from '@/lib/utils';
import type { CandidateDocument, DocumentBoard } from '@/types/domain';
import { documentStatus, documentTone } from '@/lib/status';
import { maxUploadBytes, formatBytes, messageOf } from '@/lib/helpers';
import { Chip, Empty } from '@/components/common';

/** Valida o arquivo de um documento no cliente: o backend aceita PDF e DOCX de até 5 MB. */
export function checkDocumentFile(file: File): string | null {
  if (!/\.(pdf|docx)$/i.test(file.name)) return 'Só aceitamos arquivos PDF ou DOCX. Converta o documento e envie de novo.';
  if (file.size > maxUploadBytes) return 'O arquivo passa de 5 MB. Envie uma versão menor.';
  return null;
}

/**
 * Quadro de documentos de uma candidatura: enviados, pendentes e quantos obrigatórios já foram.
 * O candidato e o RH veem o mesmo quadro; só o candidato recebe `onReplace` e pode trocar arquivos,
 * e só o RH recebe `onReview` para aprovar ou recusar.
 */
export function DocumentBoardView({ applicationId, reloadKey, onDownload, onReplace, onReview }: {
  applicationId: string; reloadKey: string | number;
  onDownload: (doc: CandidateDocument) => Promise<void>;
  onReplace?: (typeCode: string, file: File) => Promise<string | null>;
  onReview?: ReviewDocument;
}) {
  const [board, setBoard] = useState<DocumentBoard | null>(null);
  const [error, setError] = useState<string | null>(null);
  // Sobe a cada revisão aceita: o quadro recarrega com o status novo.
  const [version, setVersion] = useState(0);
  useEffect(() => {
    let current = true;
    portalService.getDocumentBoard(applicationId)
      .then((next) => { if (current) { setBoard(next); setError(null); } })
      .catch((problem) => { if (current) setError(messageOf(problem, 'Não conseguimos carregar os documentos agora. Tente novamente em instantes.')); });
    return () => { current = false; };
  }, [applicationId, reloadKey, version]);
  const review: ReviewDocument | undefined = onReview && (async (doc, approve) => {
    const ok = await onReview(doc, approve);
    if (ok) setVersion((current) => current + 1);
    return ok;
  });
  if (error && !board) return <div className="lineup"><Empty title="Não foi possível carregar os documentos" text={error} /></div>;
  if (!board) return <div className="lineup"><p className="empty muted">Carregando documentos…</p></div>;
  return <section className="lineup doc-group" aria-label={`Documentos de ${board.jobTitle}`}>
    <div className="doc-group-head">
      <strong>{board.jobTitle}</strong>
      <Chip tone={board.sentRequired >= board.totalRequired ? 'green' : 'yellow'}>{board.sentRequired} de {board.totalRequired} obrigatórios enviados</Chip>
    </div>
    <h3 className="doc-subhead">Enviados <span>{board.sent.length}</span></h3>
    {board.sent.length > 0
      ? board.sent.map((doc) => <DocumentRow key={doc.id} doc={doc} onDownload={onDownload} onReplace={onReplace} onReview={review} />)
      : <p className="doc-none">Nenhum documento enviado ainda.</p>}
    <h3 className="doc-subhead">Pendentes <span>{board.pending.length}</span></h3>
    {board.pending.length > 0
      ? board.pending.map((item) => <div className="doc-row" key={item.code}>
          <span className="doc-icon"><FileText /></span>
          <div><strong>{item.name}</strong>{item.condition && <small>{item.condition}</small>}</div>
          <div className="doc-side">{item.condition && <Chip tone="yellow">Condicional</Chip>}</div>
        </div>)
      : <p className="doc-none">Nenhum documento pendente.</p>}
  </section>;
}

/** Aprova ou recusa um documento; devolve se a revisão foi aceita. */
export type ReviewDocument = (doc: CandidateDocument, approve: boolean) => Promise<boolean>;

export function DocumentRow({ doc, onDownload, onReplace, onReview }: {
  doc: CandidateDocument; onDownload: (doc: CandidateDocument) => Promise<void>;
  /** Só aparece em envios com tipo da lista: trocar o arquivo é reenviar o mesmo tipo. */
  onReplace?: (typeCode: string, file: File) => Promise<string | null>;
  /** Só o RH revisa: aprovar ou recusar o documento enviado. */
  onReview?: ReviewDocument;
}) {
  const [busy, setBusy] = useState<'download' | 'replace' | 'approve' | 'refuse' | null>(null);
  const [error, setError] = useState<string | null>(null);
  const fileInput = useRef<HTMLInputElement>(null);
  const replace = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]; event.target.value = '';
    if (!file || !onReplace || !doc.typeCode) return;
    const problem = checkDocumentFile(file);
    if (problem) { setError(problem); return; }
    setError(null); setBusy('replace');
    setError(await onReplace(doc.typeCode, file));
    setBusy(null);
  };
  return <div className="doc-row">
    <span className="doc-icon"><FileText /></span>
    <div><strong>{doc.type}</strong><small>{doc.jobTitle} · {doc.format.toUpperCase()} · {formatBytes(doc.sizeBytes)} · enviado em {formatDate(doc.sentAt)}</small>{onReplace && doc.status === 'recusado' && <small>Este arquivo não foi aceito. Substitua por uma nova versão para o RH analisar de novo.</small>}{error && <small className="field-error" role="alert">{error}</small>}</div>
    <div className="doc-side">
      <Chip tone={documentTone[doc.status]}>{documentStatus[doc.status]}</Chip>
      {doc.typeCode === null && <Chip tone="dashed">tipo antigo</Chip>}
      {onReview && <>
        <Button variant="outline" disabled={busy !== null || doc.status === 'aprovado'} onClick={async () => { setBusy('approve'); await onReview(doc, true); setBusy(null); }}><Check />{busy === 'approve' ? 'Aprovando…' : 'Aprovar'}<span className="sr-only"> {doc.type}</span></Button>
        <Button variant="outline" disabled={busy !== null || doc.status === 'recusado'} onClick={async () => { setBusy('refuse'); await onReview(doc, false); setBusy(null); }}><X />{busy === 'refuse' ? 'Recusando…' : 'Recusar'}<span className="sr-only"> {doc.type}</span></Button>
      </>}
      {onReplace && doc.typeCode && <>
        <input ref={fileInput} type="file" accept=".pdf,.docx" hidden aria-label={`Novo arquivo de ${doc.type}`} onChange={replace} />
        <Button variant="outline" disabled={busy !== null} onClick={() => fileInput.current?.click()}><Upload />{busy === 'replace' ? 'Enviando…' : 'Substituir'}<span className="sr-only"> {doc.type}</span></Button>
      </>}
      <Button variant="outline" disabled={busy !== null} onClick={async () => { setBusy('download'); await onDownload(doc); setBusy(null); }}><Download />{busy === 'download' ? 'Baixando…' : 'Baixar'}<span className="sr-only"> {doc.type}</span></Button>
    </div>
  </div>;
}
