---
version: 1
slug: "frontend-src-app-tsx"
primary_target: "frontend/src/App.tsx"
related_targets: ["frontend/src/styles.css"]
---

# Surface brief — TeamUp app (frontend/src/App.tsx)

Scope: todo o app (login, cadastro, portal do candidato, painel do RH). Mode: Operate.
Audience/job: candidato no celular procurando vaga, mantendo currículo e acompanhando etapas; RH no desktop gerenciando vagas, inscritos e documentos.
Constraints: preservar toda a comunicação com a API (auth, /vagas, /curriculos) e os mocks; tema claro/escuro com padrão do sistema; verde como identidade; sem números, avaliações ou logos inventados.
History: a direção "Escalação" (futebol, seed 8c741621) foi construída e rejeitada pelo usuário ("não tem nada a ver com time de futebol"). O usuário fixou uma nova direção a partir de um prompt de referência "Liquid Glass"; direção fixada pelo usuário vence o sorteio.

## Direction contract

THESIS: Um portal de RH claro e leve, feito de vidro sobre luz verde: superfícies translúcidas com brilho interno flutuam sobre um fundo branco com halos verdes desfocados. Recusa o SaaS cinza de barra lateral e cards opacos; recusa também o gramado anterior.

OWN-WORLD: Fundo branco puro (claro) ou verde quase preto (escuro) com elipses desfocadas de verde (#4fd18b, #1fae6b) no canto superior esquerdo. Vidro: rgba branco translúcido, backdrop-blur forte, contorno 1px rgba(0,0,0,.1), sombra interna de brilho inset 0 4px 4px rgba(255,255,255,.25), raio 16px. Orbe verde de vidro (CSS, sem vídeo de terceiros) como peça-assinatura do login. Títulos em Fustat Bold com tracking negativo; corpo e UI em Inter. Botão primário verde translúcido com brilho interno e ícone de seta em círculo branco; hover escala 1.02. Estados como pílulas de vidro com ponto colorido.

STORY: O candidato entende em segundos quais vagas estão abertas, abre uma, confirma a candidatura com o currículo único e vê todas as etapas ao mesmo tempo, a atual preenchida. O RH cria, edita e encerra vagas (com confirmação), vê inscritos e revisa documentos. Áreas ainda sem backend são marcadas como demonstração.

FIRST VIEWPORT: Login desktop (máx. 1600px): navbar de vidro flutuante centralizada no topo (marca TeamUp + alternador de tema); coluna esquerda com título grande "Sua próxima vaga, do jeito que faz sentido acompanhar." (Fustat ~72px, 1.05, -2px), subtítulo Inter 18px e três fatos do processo; coluna direita com o orbe verde enorme sangrando da área e o cartão de entrada em vidro sobre ele, botão "Entrar" verde. Celular: coluna única, título menor, cartão de vidro com o orbe atrás. App: navbar de vidro flutuante (sticky, top 16–30px) no desktop; no celular, barra superior de vidro e barra inferior de abas de vidro.

FORM: Liquid Glass (direção fixada pelo usuário, inspirada no prompt de referência; adaptada: azul → verde, vídeo do orbe → orbe em CSS, prova social e logos → fatos reais do processo). Seed anterior 8c741621 (direção rejeitada). Signature interaction: o orbe gira e respira lentamente atrás do vidro; botões primários escalam 1.02 no hover; movimento reduzido para o orbe.

FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance
