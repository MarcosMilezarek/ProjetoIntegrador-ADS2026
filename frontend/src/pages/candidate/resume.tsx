import { useEffect, useRef, useState } from 'react';
import type { ChangeEvent, FormEvent } from 'react';
import { Download, FileText, Plus, Trash2, Upload, X } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Checkbox } from '@/components/ui/checkbox';
import { Input } from '@/components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Textarea } from '@/components/ui/textarea';
import { formatDate } from '@/lib/utils';
import type { CandidateProfile, ResumeEducation, ResumeExperience, Sexo } from '@/types/domain';
import { maxUploadBytes, localToday, formatBytes } from '@/lib/helpers';
import { Field, Chip } from '@/components/common';

type SaveResult = { ok: true } | { ok: false; campos?: Record<string, string> };

type ResumeErrors = Record<string, string>;

export const sexoOptions: { value: Sexo; label: string }[] = [
  { value: 'feminino', label: 'Feminino' },
  { value: 'masculino', label: 'Masculino' },
  { value: 'outro', label: 'Outro' },
  { value: 'nao_informado', label: 'Prefiro não informar' },
];

/** Chaves de erro que a tela sabe mostrar: os `campos` do backend seguem este mesmo formato. */
const resumeKeyPattern = /^(dataNascimento|sexo|cidade|uf|numeroContato|perfilLinkedin|competencias|certificacoes|resumo)$|^(experiencias|formacoes)\[\d+\]\.\w+$/;

/** O que o usuário edita, sem chaves de lista, idade e arquivo: serve para saber se há alterações não salvas. */
const resumeSignature = (profile: CandidateProfile) => JSON.stringify({
  ...profile, idade: undefined, arquivo: undefined, updatedAt: undefined,
  formacoes: profile.formacoes.map(({ uid: _uid, ...item }) => item),
  experiencias: profile.experiencias.map(({ uid: _uid, ...item }) => item),
});

/** Valida no cliente o que a API também valida, com as mesmas chaves de erro do backend. */
function validateResume(form: CandidateProfile): ResumeErrors {
  const errors: ResumeErrors = {};
  if (form.dataNascimento && form.dataNascimento >= localToday()) errors.dataNascimento = 'A data de nascimento deve ser no passado.';
  if (form.uf && form.uf.length !== 2) errors.uf = 'Use a sigla com 2 letras.';
  form.experiencias.forEach((item, index) => {
    const key = (field: string) => `experiencias[${index}].${field}`;
    if (!item.cargo.trim()) errors[key('cargo')] = 'Informe o cargo.';
    if (!item.empresa.trim()) errors[key('empresa')] = 'Informe a empresa.';
    if (!item.dataContratacao) errors[key('dataContratacao')] = 'Informe a data de contratação.';
    else if (!item.trabalhoAtual && item.dataDemissao && item.dataDemissao < item.dataContratacao) errors[key('dataDemissao')] = 'A demissão não pode ser anterior à contratação.';
  });
  form.formacoes.forEach((item, index) => {
    const key = (field: string) => `formacoes[${index}].${field}`;
    if (!item.curso.trim()) errors[key('curso')] = 'Informe o curso.';
    if (!item.instituicao.trim()) errors[key('instituicao')] = 'Informe a instituição.';
    if (!item.dataInicio) errors[key('dataInicio')] = 'Informe a data de início.';
  });
  return errors;
}

export function ResumePage({ profile, userName, userEmail, onSave, onUpload, onDownload }: {
  profile: CandidateProfile; userName: string; userEmail: string;
  onSave: (profile: CandidateProfile) => Promise<SaveResult>;
  /** Devolve a mensagem de erro, ou null quando o PDF foi aceito. */
  onUpload: (file: File) => Promise<string | null>;
  onDownload: () => Promise<void>;
}) {
  const [form, setForm] = useState(profile);
  const [errors, setErrors] = useState<ResumeErrors>({});
  const [saving, setSaving] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [fileError, setFileError] = useState<string | null>(null);
  const formRef = useRef<HTMLFormElement>(null);
  const fileInput = useRef<HTMLInputElement>(null);
  const signature = resumeSignature(profile);
  // Só recarrega o formulário quando o currículo salvo muda de fato (não a cada atualização de dados da página).
  useEffect(() => { setForm(profile); setErrors({}); }, [signature]); // eslint-disable-line react-hooks/exhaustive-deps
  const dirty = resumeSignature(form) !== signature;
  const pending = dirty || !profile.id;
  const failing = Object.keys(errors).length > 0;

  const clearErrors = (...keys: string[]) => setErrors((current) => {
    if (!keys.some((key) => key in current)) return current;
    const next = { ...current };
    keys.forEach((key) => delete next[key]);
    return next;
  });
  const focusFirstInvalid = () => window.setTimeout(() => formRef.current?.querySelector<HTMLElement>('[aria-invalid="true"]')?.focus(), 50);
  const update = <K extends keyof CandidateProfile>(key: K, value: CandidateProfile[K]) => { setForm((current) => ({ ...current, [key]: value })); clearErrors(key); };
  const patchExperience = (index: number, patch: Partial<ResumeExperience>) => {
    setForm((current) => ({ ...current, experiencias: current.experiencias.map((item, position) => position === index ? { ...item, ...patch } : item) }));
    clearErrors(...Object.keys(patch).map((field) => `experiencias[${index}].${field}`), `experiencias[${index}].dataDemissao`);
  };
  const patchEducation = (index: number, patch: Partial<ResumeEducation>) => {
    setForm((current) => ({ ...current, formacoes: current.formacoes.map((item, position) => position === index ? { ...item, ...patch } : item) }));
    clearErrors(...Object.keys(patch).map((field) => `formacoes[${index}].${field}`));
  };
  const dropErrorsOf = (list: 'experiencias' | 'formacoes') => setErrors((current) => Object.fromEntries(Object.entries(current).filter(([key]) => !key.startsWith(`${list}[`))));

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    const found = validateResume(form);
    setErrors(found);
    if (Object.keys(found).length) { focusFirstInvalid(); return; }
    setSaving(true);
    const result = await onSave(form);
    setSaving(false);
    if (!result.ok && result.campos) {
      const mapped: ResumeErrors = {};
      for (const [key, message] of Object.entries(result.campos)) {
        const field = key.replace(/\.periodoValido$/, '.dataDemissao');
        if (resumeKeyPattern.test(field)) mapped[field] = message;
      }
      setErrors(mapped);
      focusFirstInvalid();
    }
  };

  const chooseFile = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]; event.target.value = '';
    if (!file) return;
    if (file.type !== 'application/pdf' && !file.name.toLowerCase().endsWith('.pdf')) { setFileError('Só aceitamos arquivos PDF. Converta o currículo e envie de novo.'); return; }
    if (file.size > maxUploadBytes) { setFileError('O arquivo passa de 5 MB. Envie uma versão menor.'); return; }
    setFileError(null); setUploading(true);
    setFileError(await onUpload(file));
    setUploading(false);
  };

  const invalid = (key: string) => errors[key] ? true : undefined;
  const status = failing ? 'Corrija os campos destacados' : dirty ? 'Alterações não salvas' : 'Currículo ainda não salvo';

  return <main className="page narrow">
    <div className="page-head"><div><h1 className="display">Meu currículo</h1><p>Um currículo só, usado em todas as suas candidaturas.</p></div>{!pending && <Chip tone="green">Tudo salvo{profile.updatedAt ? ` · ${formatDate(profile.updatedAt)}` : ''}</Chip>}</div>
    <form onSubmit={submit} ref={formRef} noValidate>
      <div className="form-sheet">
        <section className="form-section"><header><h2>Contato</h2><p>Nome e e-mail vêm do seu cadastro.</p></header><div>
          <div className="field-row"><Field label="Nome completo"><Input value={userName} disabled /></Field><Field label="E-mail"><Input value={userEmail} disabled /></Field></div>
          <div className="field-row">
            <Field label="Telefone" error={errors.numeroContato}><Input type="tel" inputMode="tel" autoComplete="tel" maxLength={20} placeholder="(54) 99999-0000" value={form.numeroContato} aria-invalid={invalid('numeroContato')} onChange={(event) => update('numeroContato', event.target.value)} /></Field>
            <Field label="LinkedIn" error={errors.perfilLinkedin}><Input type="url" inputMode="url" maxLength={255} placeholder="https://linkedin.com/in/seu-perfil" value={form.perfilLinkedin} aria-invalid={invalid('perfilLinkedin')} onChange={(event) => update('perfilLinkedin', event.target.value)} /></Field>
          </div>
        </div></section>
        <section className="form-section"><header><h2>Dados pessoais</h2><p>Usados só para o processo seletivo.</p></header><div>
          <div className="field-row">
            <Field label="Data de nascimento" hint={profile.idade != null ? `${profile.idade} anos` : undefined} error={errors.dataNascimento}><Input type="date" max={localToday()} value={form.dataNascimento} aria-invalid={invalid('dataNascimento')} onChange={(event) => update('dataNascimento', event.target.value)} /></Field>
            <Field label="Sexo" error={errors.sexo}><Select value={form.sexo} onValueChange={(value) => update('sexo', value as Sexo)}><SelectTrigger className="w-full" aria-invalid={invalid('sexo')}><SelectValue placeholder="Selecione" /></SelectTrigger><SelectContent>{sexoOptions.map((option) => <SelectItem key={option.value} value={option.value}>{option.label}</SelectItem>)}</SelectContent></Select></Field>
          </div>
          <div className="field-row city-row">
            <Field label="Cidade" error={errors.cidade}><Input maxLength={100} autoComplete="address-level2" placeholder="Erechim" value={form.cidade} aria-invalid={invalid('cidade')} onChange={(event) => update('cidade', event.target.value)} /></Field>
            <Field label="UF" error={errors.uf}><Input maxLength={2} autoComplete="address-level1" placeholder="RS" value={form.uf} aria-invalid={invalid('uf')} onChange={(event) => update('uf', event.target.value.toUpperCase())} /></Field>
          </div>
        </div></section>
        <section className="form-section"><header><h2 id="cv-resumo">Resumo profissional</h2><p>Duas ou três frases sobre você e o que procura.</p></header><div><Textarea aria-labelledby="cv-resumo" placeholder="Ex.: Estudante de ADS, busco estágio em desenvolvimento web." value={form.resumo} onChange={(event) => update('resumo', event.target.value)} />{errors.resumo && <small className="field-error" role="alert">{errors.resumo}</small>}</div></section>
        <section className="form-section"><header><h2>Experiência profissional</h2><p>Opcional. Cargo, empresa, período e o que você fazia.</p></header><div>
          {form.experiencias.map((item, index) => {
            const error = (field: string) => errors[`experiencias[${index}].${field}`];
            return <div className="repeat-item" key={item.uid}>
              <div className="repeat-head"><strong>Experiência {index + 1}</strong><Button type="button" variant="ghost" size="sm" onClick={() => { setForm((current) => ({ ...current, experiencias: current.experiencias.filter((_, position) => position !== index) })); dropErrorsOf('experiencias'); }}><Trash2 />Remover<span className="sr-only"> experiência {index + 1}</span></Button></div>
              <div className="field-row">
                <Field label="Cargo" error={error('cargo')}><Input value={item.cargo} aria-invalid={invalid(`experiencias[${index}].cargo`)} onChange={(event) => patchExperience(index, { cargo: event.target.value })} /></Field>
                <Field label="Empresa" error={error('empresa')}><Input value={item.empresa} aria-invalid={invalid(`experiencias[${index}].empresa`)} onChange={(event) => patchExperience(index, { empresa: event.target.value })} /></Field>
              </div>
              <div className="field-row">
                <Field label="Contratação" error={error('dataContratacao')}><Input type="date" value={item.dataContratacao} aria-invalid={invalid(`experiencias[${index}].dataContratacao`)} onChange={(event) => patchExperience(index, { dataContratacao: event.target.value })} /></Field>
                <Field label="Demissão" error={error('dataDemissao')}><Input type="date" min={item.dataContratacao || undefined} disabled={item.trabalhoAtual} value={item.trabalhoAtual ? '' : item.dataDemissao} aria-invalid={invalid(`experiencias[${index}].dataDemissao`)} onChange={(event) => patchExperience(index, { dataDemissao: event.target.value })} /></Field>
              </div>
              <label className="check-label"><Checkbox checked={item.trabalhoAtual} onCheckedChange={(value) => patchExperience(index, value === true ? { trabalhoAtual: true, dataDemissao: '' } : { trabalhoAtual: false })} /> Trabalho aqui atualmente</label>
              <Field label="Atividades" hint="opcional"><Textarea placeholder="Ex.: Atendimento a clientes, planilhas e relatórios." value={item.descricaoAtividades} onChange={(event) => patchExperience(index, { descricaoAtividades: event.target.value })} /></Field>
            </div>;
          })}
          {!form.experiencias.length && <p className="repeat-empty">Nenhuma experiência adicionada.</p>}
          <Button type="button" variant="outline" className="repeat-add" onClick={() => setForm((current) => ({ ...current, experiencias: [...current.experiencias, { uid: crypto.randomUUID(), cargo: '', empresa: '', dataContratacao: '', dataDemissao: '', trabalhoAtual: false, descricaoAtividades: '' }] }))}><Plus />Adicionar experiência</Button>
        </div></section>
        <section className="form-section"><header><h2>Formação acadêmica</h2><p>Opcional. Curso, instituição e período.</p></header><div>
          {form.formacoes.map((item, index) => {
            const error = (field: string) => errors[`formacoes[${index}].${field}`];
            return <div className="repeat-item" key={item.uid}>
              <div className="repeat-head"><strong>Formação {index + 1}</strong><Button type="button" variant="ghost" size="sm" onClick={() => { setForm((current) => ({ ...current, formacoes: current.formacoes.filter((_, position) => position !== index) })); dropErrorsOf('formacoes'); }}><Trash2 />Remover<span className="sr-only"> formação {index + 1}</span></Button></div>
              <div className="field-row">
                <Field label="Curso" error={error('curso')}><Input value={item.curso} aria-invalid={invalid(`formacoes[${index}].curso`)} onChange={(event) => patchEducation(index, { curso: event.target.value })} /></Field>
                <Field label="Instituição" error={error('instituicao')}><Input value={item.instituicao} aria-invalid={invalid(`formacoes[${index}].instituicao`)} onChange={(event) => patchEducation(index, { instituicao: event.target.value })} /></Field>
              </div>
              <div className="field-row">
                <Field label="Início" error={error('dataInicio')}><Input type="date" value={item.dataInicio} aria-invalid={invalid(`formacoes[${index}].dataInicio`)} onChange={(event) => patchEducation(index, { dataInicio: event.target.value })} /></Field>
                <Field label="Término" hint="vazio = em andamento" error={error('dataTermino')}><Input type="date" min={item.dataInicio || undefined} value={item.dataTermino} aria-invalid={invalid(`formacoes[${index}].dataTermino`)} onChange={(event) => patchEducation(index, { dataTermino: event.target.value })} /></Field>
              </div>
            </div>;
          })}
          {!form.formacoes.length && <p className="repeat-empty">Nenhuma formação adicionada.</p>}
          <Button type="button" variant="outline" className="repeat-add" onClick={() => setForm((current) => ({ ...current, formacoes: [...current.formacoes, { uid: crypto.randomUUID(), curso: '', instituicao: '', dataInicio: '', dataTermino: '' }] }))}><Plus />Adicionar formação</Button>
        </div></section>
        <section className="form-section"><header><h2 id="cv-skills">Competências</h2><p>Enter ou vírgula para adicionar.</p></header><div><TagInput labelId="cv-skills" value={form.skills} onChange={(skills) => update('skills', skills)} />{errors.competencias && <small className="field-error" role="alert">{errors.competencias}</small>}</div></section>
        <section className="form-section"><header><h2 id="cv-cert">Certificações</h2><p>Opcional. Cursos e certificados fora da formação acadêmica.</p></header><div><Textarea aria-labelledby="cv-cert" placeholder="Ex.: AWS Cloud Practitioner (2024)." value={form.certificacoes} onChange={(event) => update('certificacoes', event.target.value)} />{errors.certificacoes && <small className="field-error" role="alert">{errors.certificacoes}</small>}</div></section>
        <section className="form-section"><header><h2 id="cv-arquivo">Currículo em PDF</h2><p>Opcional. Só PDF, até 5 MB. Enviar de novo substitui o anterior.</p></header><div>
          {profile.arquivo
            ? <div className="file-card"><span className="doc-icon"><FileText /></span><div><strong>{profile.arquivo.nomeOriginal}</strong><small>{formatBytes(profile.arquivo.tamanhoBytes)} · enviado em {formatDate(profile.arquivo.enviadoEm)}</small></div><Button type="button" variant="outline" size="sm" onClick={() => void onDownload()}><Download />Baixar</Button></div>
            : <p className="repeat-empty">Nenhum arquivo enviado.</p>}
          <input ref={fileInput} type="file" accept="application/pdf,.pdf" hidden aria-labelledby="cv-arquivo" onChange={chooseFile} />
          {profile.id
            ? <Button type="button" variant="outline" className="repeat-add" disabled={uploading} onClick={() => fileInput.current?.click()}><Upload />{uploading ? 'Enviando…' : profile.arquivo ? 'Substituir PDF' : 'Enviar PDF'}</Button>
            : <p className="repeat-empty">Salve o currículo primeiro para poder anexar o PDF.</p>}
          {fileError && <small className="field-error" role="alert">{fileError}</small>}
        </div></section>
      </div>
      {pending && <div className="save-bar" data-dirty={dirty} data-invalid={failing}><span>{status}</span><Button type="submit" disabled={saving}>{saving ? 'Salvando…' : 'Salvar currículo'}</Button></div>}
    </form>
  </main>;
}

function TagInput({ value, onChange, labelId }: { value: string[]; onChange: (value: string[]) => void; labelId: string }) {
  const [draft, setDraft] = useState('');
  const add = (raw: string) => {
    const next = raw.split(',').map((item) => item.trim()).filter((item) => item && !value.includes(item));
    if (next.length) onChange([...value, ...next]);
    setDraft('');
  };
  return <div className="tag-input">
    {value.map((tag) => <span className="tag" key={tag}>{tag}<button type="button" aria-label={`Remover ${tag}`} onClick={() => onChange(value.filter((item) => item !== tag))}><X /></button></span>)}
    <input aria-labelledby={labelId} value={draft} placeholder={value.length ? 'Adicionar outra' : 'Ex.: React, SQL, Comunicação'}
      onChange={(event) => { const next = event.target.value; if (next.includes(',')) add(next); else setDraft(next); }}
      onKeyDown={(event) => { if (event.key === 'Enter') { event.preventDefault(); add(draft); } else if (event.key === 'Backspace' && !draft && value.length) onChange(value.slice(0, -1)); }}
      onBlur={() => { if (draft.trim()) add(draft); }} />
  </div>;
}
