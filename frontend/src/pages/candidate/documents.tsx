import { useEffect, useRef, useState } from 'react';
import type { ChangeEvent, FormEvent } from 'react';
import { ChevronLeft, Upload } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { portalService } from '@/services/portal-service';
import type { Application, CandidateDocument, DocumentType } from '@/types/domain';
import { formatBytes, messageOf } from '@/lib/helpers';
import { checkDocumentFile, DocumentBoardView } from '@/components/documents';
import { Field, FormError, Empty } from '@/components/common';

type UploadDocument = (applicationId: string, typeCode: string, file: File) => Promise<string | null>;

export function DocumentsPage({ applications, reloadKey, onBack, onUpload, onDownload }: {
  applications: Application[]; reloadKey: number; onBack: () => void;
  /** Devolve a mensagem de erro, ou null quando o documento foi aceito. */
  onUpload: UploadDocument;
  onDownload: (doc: CandidateDocument) => Promise<void>;
}) {
  // O backend só aceita documentos em candidaturas aprovadas ou contratadas.
  const eligible = applications.filter((app) => app.status === 'approved' || app.status === 'hired');
  // Sobe a cada envio aceito: os quadros recarregam e o item passa de pendente para enviado.
  const [version, setVersion] = useState(0);
  const upload: UploadDocument = async (applicationId, typeCode, file) => {
    const problem = await onUpload(applicationId, typeCode, file);
    if (!problem) setVersion((current) => current + 1);
    return problem;
  };
  return <main className="page narrow">
    <button type="button" className="back-link" onClick={onBack}><ChevronLeft />Candidaturas</button>
    <div className="page-head"><div><h1 className="display">Documentos</h1><p>Envie os documentos de contratação das vagas em que você foi aprovado.</p></div></div>
    {eligible.length === 0
      ? <div className="lineup"><Empty title="Documentos ainda não liberados" text="Os documentos de contratação são liberados quando você for aprovado em uma vaga. Acompanhe suas candidaturas para saber quando isso acontecer." /></div>
      : <>
          <DocumentForm applications={eligible} onUpload={upload} />
          <div className="stack">{eligible.map((app) => <DocumentBoardView key={app.id} applicationId={app.id} reloadKey={`${version}.${reloadKey}`} onDownload={onDownload} onReplace={(typeCode, file) => upload(app.id, typeCode, file)} />)}</div>
        </>}
  </main>;
}

function DocumentForm({ applications, onUpload }: { applications: Application[]; onUpload: UploadDocument }) {
  const [types, setTypes] = useState<DocumentType[]>([]);
  const [applicationId, setApplicationId] = useState(applications[0].id);
  const [typeCode, setTypeCode] = useState('');
  const [file, setFile] = useState<File | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [sending, setSending] = useState(false);
  const fileInput = useRef<HTMLInputElement>(null);
  useEffect(() => { portalService.getDocumentTypes().then(setTypes).catch((problem) => setError(messageOf(problem, 'Não conseguimos carregar os tipos de documento agora. Tente novamente em instantes.'))); }, []);
  // A candidatura escolhida pode sair da lista (ex.: o RH mudou a etapa): volta para a primeira.
  const selected = applications.some((app) => app.id === applicationId) ? applicationId : applications[0].id;
  const choose = (event: ChangeEvent<HTMLInputElement>) => {
    const picked = event.target.files?.[0]; event.target.value = '';
    if (!picked) return;
    const problem = checkDocumentFile(picked);
    setFile(problem ? null : picked); setError(problem);
  };
  const submit = async (event: FormEvent) => {
    event.preventDefault();
    if (!typeCode) { setError('Escolha o tipo do documento.'); return; }
    if (!file) { setError('Escolha o arquivo do documento.'); return; }
    setError(null); setSending(true);
    const problem = await onUpload(selected, typeCode, file);
    setSending(false);
    if (problem) setError(problem); else { setTypeCode(''); setFile(null); }
  };
  return <section className="panel doc-form">
    <div className="panel-head"><div><h2>Enviar documento</h2><p>PDF ou DOCX, até 5 MB. Enviar de novo o mesmo tipo substitui o arquivo anterior.</p></div></div>
    <form className="dialog-form" onSubmit={submit} noValidate>
      <div className="field-row">
        {applications.length > 1 && <Field label="Vaga"><Select value={selected} onValueChange={setApplicationId}><SelectTrigger className="w-full"><SelectValue /></SelectTrigger><SelectContent>{applications.map((app) => <SelectItem key={app.id} value={app.id}>{app.jobTitle}</SelectItem>)}</SelectContent></Select></Field>}
        <Field label="Tipo do documento"><Select value={typeCode} onValueChange={(value) => { setTypeCode(value); setError(null); }}><SelectTrigger className="w-full"><SelectValue placeholder="Selecione o tipo" /></SelectTrigger><SelectContent>{types.map((item) => <SelectItem key={item.code} value={item.code}>{item.name}{item.required ? '' : ' (condicional)'}</SelectItem>)}</SelectContent></Select></Field>
      </div>
      <div className="file-pick">
        <input ref={fileInput} type="file" accept=".pdf,.docx" hidden aria-label="Arquivo do documento" onChange={choose} />
        <Button type="button" variant="outline" onClick={() => fileInput.current?.click()}><Upload />{file ? 'Trocar arquivo' : 'Escolher arquivo'}</Button>
        <span>{file ? `${file.name} · ${formatBytes(file.size)}` : 'Nenhum arquivo escolhido'}</span>
      </div>
      {error && <FormError title="Não foi possível enviar" text={error} />}
      <div className="doc-form-actions"><Button type="submit" disabled={sending}>{sending ? 'Enviando…' : 'Enviar documento'}</Button></div>
    </form>
  </section>;
}
