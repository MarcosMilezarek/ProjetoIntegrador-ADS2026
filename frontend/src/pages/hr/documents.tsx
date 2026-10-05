import { useMemo } from 'react';
import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from '@/components/ui/accordion';
import type { CandidateDocument } from '@/types/domain';
import { DocumentRow } from '@/components/documents';
import type { ReviewDocument } from '@/components/documents';
import { Chip, Empty } from '@/components/common';
import { initials } from '@/lib/helpers';

/** Documentos do RH e do administrador: um item expansível por candidato, com as vagas dele no cabeçalho e os arquivos dentro. */
export function HrDocumentsPage({ documents, onDownload, onReview }: { documents: CandidateDocument[]; onDownload: (doc: CandidateDocument) => Promise<void>; onReview: ReviewDocument }) {
  const groups = useMemo(() => {
    const byCandidate = new Map<string, { id: string; name: string; jobs: string[]; documents: CandidateDocument[] }>();
    for (const doc of documents) {
      const group = byCandidate.get(doc.candidateId) ?? { id: doc.candidateId, name: doc.candidateName, jobs: [], documents: [] };
      if (!group.jobs.includes(doc.jobTitle)) group.jobs.push(doc.jobTitle);
      group.documents.push(doc);
      byCandidate.set(doc.candidateId, group);
    }
    return [...byCandidate.values()].sort((a, b) => a.name.localeCompare(b.name));
  }, [documents]);
  return <section className="hr-page">
    <div className="page-head"><div><h1 className="display">Documentos de contratação</h1><p>Arquivos enviados pelos candidatos aprovados. Abra um candidato para ver e revisar os documentos dele.</p></div></div>
    {groups.length === 0
      ? <div className="lineup"><Empty title="Nenhum documento enviado" text="Os documentos enviados pelos candidatos aprovados aparecem aqui." /></div>
      : <Accordion type="multiple" className="stack">{groups.map((group) => {
          const toReview = group.documents.filter((doc) => doc.status === 'pendente').length;
          return <AccordionItem value={group.id} key={group.id} className="lineup doc-candidate">
            <AccordionTrigger className="doc-candidate-head">
              <span className="person"><span className="initials">{initials(group.name)}</span><span><strong>{group.name}</strong><small>{group.jobs.join(' · ')}</small></span></span>
              <span className="doc-candidate-meta">
                <Chip>{group.documents.length === 1 ? '1 documento' : `${group.documents.length} documentos`}</Chip>
                {toReview > 0 && <Chip tone="yellow">{toReview === 1 ? '1 para analisar' : `${toReview} para analisar`}</Chip>}
              </span>
            </AccordionTrigger>
            <AccordionContent className="doc-items">
              {group.documents.map((doc) => <DocumentRow key={doc.id} doc={doc} onDownload={onDownload} onReview={onReview} />)}
            </AccordionContent>
          </AccordionItem>;
        })}</Accordion>}
  </section>;
}
