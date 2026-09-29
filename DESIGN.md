---
name: TeamUp
description: Portal de vagas e candidaturas em vidro translúcido sobre luz verde.
colors:
  brand: "#0b7a47"
  brand-glass: "rgba(11, 122, 71, 0.88)"
  brand-soft: "rgba(11, 122, 71, 0.1)"
  brand-text: "#0a6b3e"
  ground: "#ffffff"
  ink: "#0c1a13"
  ink-2: "#4b5b53"
  line: "rgba(12, 26, 19, 0.1)"
  line-strong: "rgba(12, 26, 19, 0.2)"
  glass: "rgba(255, 255, 255, 0.3)"
  glass-card: "rgba(255, 255, 255, 0.58)"
  glass-solid: "rgba(255, 255, 255, 0.9)"
  glass-field: "rgba(255, 255, 255, 0.72)"
  glass-edge: "rgba(0, 0, 0, 0.1)"
  glow-a: "#5fd79c"
  glow-b: "#1fae6b"
  glow-c: "#b9f2d3"
  tone-green: "#17a468"
  tone-yellow: "#e2a100"
  tone-red: "#d43d33"
  tone-neutral: "#93a098"
  focus: "#17a468"
  ground-dark: "#06100b"
  ink-dark: "#e7eee9"
  ink-2-dark: "#9db1a5"
  brand-dark: "#13804c"
  brand-glass-dark: "rgba(22, 140, 84, 0.9)"
  brand-soft-dark: "rgba(52, 199, 123, 0.14)"
  brand-text-dark: "#5ed597"
  glass-dark: "rgba(12, 26, 19, 0.42)"
  glass-card-dark: "rgba(14, 30, 22, 0.55)"
  glass-solid-dark: "rgba(14, 28, 21, 0.94)"
  glass-field-dark: "rgba(255, 255, 255, 0.05)"
  glass-edge-dark: "rgba(255, 255, 255, 0.1)"
  glow-a-dark: "#1f9e62"
  glow-b-dark: "#0f6e41"
  glow-c-dark: "#164d33"
  tone-green-dark: "#3ccb82"
  tone-yellow-dark: "#f2bf33"
  tone-red-dark: "#ef6259"
  tone-neutral-dark: "#6f8278"
typography:
  display:
    fontFamily: "'Fustat Variable', 'Inter Variable', ui-sans-serif, system-ui, sans-serif"
    fontSize: "clamp(2.5rem, 5.2vw, 4.6875rem)"
    fontWeight: 700
    lineHeight: 1.05
    letterSpacing: "-2px"
  headline:
    fontFamily: "'Fustat Variable', 'Inter Variable', ui-sans-serif, system-ui, sans-serif"
    fontSize: "clamp(1.75rem, 3vw, 2.375rem)"
    fontWeight: 700
    lineHeight: 1.05
    letterSpacing: "-0.03em"
  title:
    fontFamily: "'Inter Variable', ui-sans-serif, system-ui, sans-serif"
    fontSize: "1.0625rem"
    fontWeight: 600
    lineHeight: 1.25
    letterSpacing: "-0.01em"
  body:
    fontFamily: "'Inter Variable', ui-sans-serif, system-ui, sans-serif"
    fontSize: "1rem"
    fontWeight: 400
    lineHeight: 1.5
  lede:
    fontFamily: "'Inter Variable', ui-sans-serif, system-ui, sans-serif"
    fontSize: "1.125rem"
    fontWeight: 400
    lineHeight: 1.6
    letterSpacing: "-0.01em"
  label:
    fontFamily: "'Inter Variable', ui-sans-serif, system-ui, sans-serif"
    fontSize: "0.8125rem"
    fontWeight: 500
    lineHeight: 1.35
  wordmark:
    fontFamily: "'Fustat Variable', 'Inter Variable', ui-sans-serif, system-ui, sans-serif"
    fontSize: "1.3125rem"
    fontWeight: 700
    lineHeight: 1
    letterSpacing: "-0.03em"
rounded:
  sm: "8px"
  md: "10px"
  lg: "12px"
  control-lg: "14px"
  xl: "16px"
  panel: "18px"
  tabbar: "20px"
  auth-card: "22px"
  pill: "99px"
spacing:
  xs: "4px"
  sm: "8px"
  md: "12px"
  lg: "16px"
  xl: "24px"
  2xl: "32px"
  3xl: "48px"
components:
  button-primary:
    backgroundColor: "{colors.brand-glass}"
    textColor: "{colors.ground}"
    rounded: "{rounded.lg}"
    padding: "0 16px"
    height: "42px"
  button-primary-hover:
    backgroundColor: "{colors.brand}"
  button-primary-lg:
    backgroundColor: "{colors.brand-glass}"
    textColor: "{colors.ground}"
    rounded: "{rounded.control-lg}"
    height: "48px"
  button-outline:
    backgroundColor: "{colors.glass-field}"
    textColor: "{colors.ink}"
    rounded: "{rounded.lg}"
    padding: "0 16px"
    height: "42px"
  button-destructive:
    backgroundColor: "{colors.tone-red}"
    textColor: "{colors.ground}"
    rounded: "{rounded.lg}"
    height: "42px"
  input:
    backgroundColor: "{colors.glass-field}"
    textColor: "{colors.ink}"
    rounded: "{rounded.lg}"
    height: "46px"
  chip:
    backgroundColor: "{colors.glass-field}"
    textColor: "{colors.ink}"
    typography: "{typography.label}"
    rounded: "{rounded.pill}"
    padding: "3px 11px 3px 9px"
  glass-card:
    backgroundColor: "{colors.glass-card}"
    rounded: "{rounded.panel}"
    padding: "22px 24px"
  glass-nav:
    backgroundColor: "{colors.glass}"
    rounded: "{rounded.xl}"
    padding: "8px 8px 8px 18px"
  nav-item:
    textColor: "{colors.ink-2}"
    rounded: "{rounded.md}"
    padding: "0 14px"
    height: "40px"
  nav-item-active:
    backgroundColor: "{colors.brand-soft}"
    textColor: "{colors.brand-text}"
  segmented-active:
    backgroundColor: "{colors.brand-glass}"
    textColor: "{colors.ground}"
    rounded: "{rounded.md}"
    height: "36px"
  tag:
    backgroundColor: "{colors.brand-soft}"
    textColor: "{colors.brand-text}"
    rounded: "{rounded.pill}"
    padding: "3px 4px 3px 11px"
---

# Design System: TeamUp

## Overview

**Creative North Star: "Vidro sobre luz verde"**

TeamUp é um portal de RH claro e leve: superfícies translúcidas, com um brilho interno no topo, flutuam sobre um fundo branco puro (ou verde quase preto no tema escuro) iluminado por halos verdes desfocados que nascem no canto superior esquerdo. O verde não pinta blocos; ele é a luz que atravessa o vidro. A densidade é de ferramenta, não de vitrine: listas, tabelas e formulários vivem dentro de painéis de vidro com respiro generoso, e a única peça teatral do sistema é o orbe verde de vidro na tela de entrada.

O sistema recusa o SaaS cinza de barra lateral com cards opacos: a navegação é uma cápsula de vidro flutuante e centralizada, e no celular vira barra superior mais barra de abas inferior, ambas de vidro. Os dois temas são cidadãos de primeira classe; o padrão segue o sistema operacional e a escolha é aplicada antes da primeira pintura.

**Key Characteristics:**
- Fundo liso (branco ou verde quase preto) com quatro halos verdes desfocados fixos atrás de tudo.
- Toda superfície elevada é vidro: preenchimento translúcido, `backdrop-filter` forte, contorno de 1px e brilho interno.
- Verde como luz e como estado: preenchimento translúcido no primário, tom suave no ativo, ponto colorido nos status.
- Títulos em Fustat Bold com tracking negativo; todo o resto em Inter.
- Movimento curto e com mola (`cubic-bezier(0.16, 1, 0.3, 1)`); o orbe respira e gira devagar; tudo respeita `prefers-reduced-motion`.

## Colors

Uma única família verde sobre neutros com leve viés verde, mais três tons de status; cada token tem par claro e escuro com o mesmo nome no CSS (`:root` e `.dark`), e o frontmatter registra o escuro com sufixo `-dark`.

### Primary
- **Verde TeamUp** (`brand`): preenchimento sólido do botão primário no hover, cor de seleção de texto e referência do verde da marca. No escuro sobe para `brand-dark`.
- **Verde Vidro** (`brand-glass`): o verde translúcido de tudo que está "ligado": botão primário, segmento selecionado, iniciais do avatar, nós concluídos das etapas, selo de sucesso. Sempre com texto branco e brilho interno.
- **Verde Névoa** (`brand-soft`): fundo do item ativo da navegação, hover de linhas de lista e tabela, fundo de ícones em caixinha, tags de competência e o selo "RH" da marca.
- **Verde Texto** (`brand-text`): links, texto do item ativo, voltar, cursor de digitação; é o verde legível sobre claro. No escuro vira `brand-text-dark`, mais luminoso.

### Secondary
- **Luz de Halo** (`glow-a`, `glow-b`, `glow-c`): exclusivamente para os halos desfocados do fundo, com opacidade controlada por `--glow-alpha` (0.55 no claro, 0.42 no escuro). Nunca como cor de superfície ou texto.

### Tertiary
- **Tons de status** (`tone-green`, `tone-yellow`, `tone-red`, `tone-neutral`): o ponto dos chips, o indicador de progresso, a linha de etapas concluídas, o ponto de salvo/não salvo, avisos (`tone-yellow` a 10% de mistura) e erros (`tone-red` a 17% sobre `glass-solid`). O vermelho também é o botão destrutivo.

### Neutral
- **Branco Chão** (`ground`) / **Verde Noite** (`ground-dark`): o chão da página, sem textura; a profundidade vem dos halos.
- **Tinta** (`ink`) / `ink-dark`: texto principal e títulos.
- **Tinta Secundária** (`ink-2`) / `ink-2-dark`: descrições, metadados, cabeçalhos de tabela, ícones em repouso.
- **Linha** (`line`) e **Linha Forte** (`line-strong`): divisórias internas dos painéis; contorno de campos e trilho das etapas.
- **Vidros** (`glass`, `glass-card`, `glass-solid`, `glass-field`, `glass-edge`): quatro opacidades para quatro papéis, ver Elevation & Depth.

### Named Rules
**The Luz, Não Tinta Rule.** O verde entra como luz translúcida (`brand-glass`, `brand-soft`, halos), não como bloco opaco. Grandes áreas verdes sólidas só existem no orbe da entrada.

**The Ponto de Status Rule.** Status se comunica com um ponto colorido de 7px dentro de uma pílula de vidro neutra, nunca pintando a pílula inteira; o texto do status continua em `ink`.

**The Pares de Tema Rule.** Toda cor nova nasce com par claro e escuro no mesmo nome de variável; nada de cor literal em componente.

## Typography

**Display Font:** Fustat Variable (com Inter Variable, ui-sans-serif, system-ui)
**Body Font:** Inter Variable (com ui-sans-serif, system-ui)

**Character:** Fustat em 700 com tracking negativo dá aos títulos uma presença macia e arredondada, que combina com o vidro; Inter carrega toda a interface com neutralidade e bons números tabulares.

### Hierarchy
- **Display** (Fustat 700, `clamp(2.5rem, 5.2vw, 4.6875rem)`, 1.05, -2px): só o título da tela de entrada. No celular cai para 2rem.
- **Headline** (Fustat 700, `clamp(1.75rem, 3vw, 2.375rem)`, 1.05, -0.03em): títulos de página (`.display` em `page-head`); detalhe de vaga e sucesso sobem até 2.625rem. Título de diálogo usa 1.625rem com o mesmo tratamento.
- **Title** (Inter 600, 1.0625rem, 1.25, -0.01em): título de linha de vaga e de painel; seções internas em 1.125rem/600.
- **Body** (Inter 400, 1rem, 1.5): texto geral; prosa de vaga em 1.7 de entrelinha e no máximo 68ch. Metadados em 0.9375rem.
- **Lede** (Inter 400, 1.125rem, 1.6, -0.01em): subtítulo da tela de entrada, até 34rem.
- **Label** (Inter 500, 0.8125rem, 1.35): chips, legendas de etapas, cabeçalhos de tabela, rótulos de campo em 0.875rem.

### Named Rules
**The Fustat Só Em Título Rule.** Fustat aparece apenas em títulos de página e diálogo, na palavra-marca, nas iniciais do avatar e em números de destaque (`doc-summary`). Texto corrido, botões e rótulos ficam em Inter.

**The Números Tabulares Rule.** Contadores, datas e percentuais usam `font-variant-numeric: tabular-nums`.

## Layout

O conteúdo das páginas logadas fica numa coluna centralizada de até 1120px (`width: min(100% - 32px, 1120px)`), com 44px no topo e 96px embaixo; páginas de formulário e documentos estreitam para 860px. O cabeçalho da página alinha título e ação primária pela base, com quebra flexível. A tela de entrada usa duas colunas iguais dentro de até 1600px, com 48px de intervalo: texto à esquerda e, à direita, o orbe sangrando para fora com o cartão de vidro por cima.

Ritmo de espaçamento em múltiplos de 4: 8 e 12 dentro de controles, 16 entre campos, 20 a 28 entre blocos, 44 a 48 entre regiões. O detalhe de vaga usa grade `1fr + 340px` com painel de candidatura fixo (`sticky`, top 104px); o currículo usa seções `220px + 1fr`.

Responsivo: em 1000px o detalhe, o currículo e a entrada viram coluna única. Em 767px a navegação superior perde os links, surge a barra de abas inferior fixa (12px das bordas, respeitando `safe-area-inset-bottom`), barras de ação (salvar, candidatar) sobem para acima das abas, tabelas viram fichas empilhadas com rótulo inline, e os halos passam a ser medidos em `vw`.

## Elevation & Depth

A profundidade é de vidro, não de sombra: cada superfície elevada combina preenchimento translúcido, `backdrop-filter: blur(28px) saturate(1.4)` (50px e 1.6 na navegação e na barra de abas), contorno de 1px em `glass-edge` e um brilho interno no topo. A sombra externa é longa, difusa e tingida de verde no claro; ela descola o vidro dos halos sem desenhar borda dura.

### Shadow Vocabulary
- **Brilho de vidro** (`box-shadow: inset 0 4px 4px rgba(255, 255, 255, 0.25)`; escuro 0.04): em toda superfície de vidro, campos contornados inclusive.
- **Sombra de vidro** (`box-shadow: 0 1px 2px rgba(12, 26, 19, 0.04), 0 18px 40px -22px rgba(12, 60, 35, 0.28)`; escuro `0 1px 2px rgba(0,0,0,.3), 0 20px 44px -22px rgba(0,0,0,.8)`): cards, painéis, navegação, barras fixas.
- **Brilho verde do primário** (`box-shadow: inset 0 4px 4px rgba(255, 255, 255, 0.3), 0 10px 24px -12px rgba(11, 122, 71, 0.75)`): botão primário; variações menores (`inset 0 3px 3px rgba(255,255,255,.28)`) no segmento ativo, nas iniciais e nos nós de etapa.
- **Flutuante** (`box-shadow: var(--glass-hi), 0 24px 60px -24px rgba(0, 0, 0, 0.45)`): menus, selects e diálogos, sobre `glass-solid` com blur de 30px.

### Named Rules
**The Quatro Vidros Rule.** `glass` (0.3) só para navegação e barra de abas; `glass-card` (0.58) para cards e painéis; `glass-solid` (0.9) para o que precisa ler sobre qualquer coisa: menus, diálogos, barra de salvar; `glass-field` (0.72) para campos e controles internos. Não invente uma quinta opacidade.

**The Halo Atrás De Tudo Rule.** Os halos são `position: fixed` com `z-index: -1` e `pointer-events: none`; nunca ficam sobre o conteúdo nem se movem com rolagem.

## Shapes

Cantos generosamente arredondados, crescendo com o tamanho da superfície: 8–10px em itens internos (itens de menu, links da navegação, segmentos), 12px (`--radius`) em botões e campos, 14px em botões grandes e no trilho segmentado, 16px na navegação, menus e diálogos, 18px em cards e painéis, 20px na barra de abas, 22px no cartão de entrada. Pílulas (99px) para chips, tags, contadores e progresso; círculos para o orbe, a seta do CTA, os nós de etapa e os pontos de status. Bordas são sempre 1px translúcidas; a única linha tracejada do sistema marca estado de rascunho ou demonstração.

## Components

### Buttons
Vidro verde que acende: cheio, translúcido, com brilho no topo.
- **Shape:** cantos suaves (12px); tamanho grande 14px e 48px de altura.
- **Primary:** `brand-glass` com texto branco em Inter 600, 42px de altura mínima e 16px de padding lateral, `backdrop-filter: blur(2px)` e o brilho verde do primário.
- **Hover / Focus:** hover troca para `brand` sólido e escala 1.02 em 0.2s com `cubic-bezier(0.16, 1, 0.3, 1)` (sem escala em movimento reduzido); foco é anel de 2px em `focus` com 2px de afastamento.
- **Outline:** `glass-field` com contorno `glass-edge` e brilho de vidro. **Destrutivo:** `tone-red` sólido com texto branco. **Link:** sem caixa, em `brand-text`.
- **CTA de entrada:** botões de ação da tela de entrada (Entrar, Criar conta, Ir para o login) ocupam a largura toda e levam à direita uma seta num círculo branco de 26px com ícone em `brand`.

### Chips
- **Style:** pílula de vidro neutra (`glass-field`, contorno `glass-edge`), texto `ink` em Label, com ponto de 7px à esquerda.
- **State:** `green` (ponto com halo de 3px), `yellow`, `red`, neutro (padrão), `outline` (anel verde vazado, ex. inscrito) e `dashed` (anel tracejado em `ink-2`, rascunho/demonstração).

### Cards / Containers
- **Corner Style:** 18px (cartão de entrada 22px).
- **Background:** `glass-card`; `glass-solid` para barras fixas e diálogos.
- **Shadow Strategy:** brilho de vidro mais sombra de vidro (ver Elevation & Depth).
- **Border:** 1px `glass-edge`; divisões internas em `line`, sem cards dentro de cards.
- **Internal Padding:** 22px 24px nos painéis, 24px no painel lateral, 32px no cartão de entrada; 16–20px no celular.

### Inputs / Fields
- **Style:** `glass-field` com contorno `line-strong`, 12px de raio, 46px de altura, texto a 1rem; rótulo acima em 0.875rem/500 e dica em `ink-2`.
- **Focus:** anel de 2px em `focus`; campo de tags usa `:focus-within` com o mesmo anel.
- **Error / Disabled:** inválido troca o contorno para `tone-red` com halo de 3px a 22%; o bloco de erro do formulário é vermelho a 17% sobre `glass-solid` com ícone vermelho. Desabilitado mantém opacidade 1, com fundo `muted` e texto `ink-2`.

### Navigation
- **Desktop:** cápsula de vidro (`glass`, blur 50px) centralizada, `sticky` a 20px do topo, largura do conteúdo. Marca à esquerda, links no meio, sino e perfil à direita. Links em `ink-2`/500 com ícone de 17px; hover em `muted`; ativo em `brand-soft` com texto `brand-text`/600.
- **Celular:** a cápsula ocupa a largura (12px das bordas) e esconde os links; a barra de abas inferior de vidro (raio 20px, abas de 52px, ícone de 21px sobre rótulo de 0.75rem) assume a navegação com o mesmo tratamento de ativo.
- **Segmentado:** trilho de vidro `glass-card` (raio 14px, padding 4px); o segmento selecionado é `brand-glass` com texto branco. O alternador de tema (sistema, claro, escuro) usa esse mesmo trilho em `glass-field`, só com ícones.

### Marca
Orbe de 26px com gradiente radial verde e brilho especular, seguido da palavra "TeamUp" em Fustat 700 1.3125rem; na área do RH, um selo "RH" em `brand-soft` (raio 6px, 0.6875rem/700). O favicon é o mesmo orbe.

### Etapas da candidatura
Quatro nós circulares de 28px ligados por um trilho de 2px, todos visíveis de uma vez. Concluídos e atual em `brand-glass` com texto branco e trilho `tone-green`; o atual ganha anel de 5px em `brand-soft` e entra com uma leve escala (0.7s). Etapa de reprovação vira nó `tone-red` com o rótulo "Não selecionado".

### Orbe de entrada
Esfera verde de vidro em CSS puro (gradientes radiais, sombras internas e um aro especular), até 700px, que respira (escala 1.03 em 10s) e tem um véu cônico girando em 22s por trás do cartão. É a peça-assinatura: aparece só nas telas de entrada e cadastro.

## Do's and Don'ts

### Do:
- **Do** montar toda superfície elevada com os quatro ingredientes: preenchimento `glass-*`, `backdrop-filter` com blur, contorno 1px `glass-edge` e o brilho interno `--glass-hi`.
- **Do** usar `brand-glass` com texto branco e brilho interno para o que está selecionado ou é a ação principal, e `brand-soft` com `brand-text` para o ativo discreto.
- **Do** comunicar status com o chip de ponto colorido e as etapas com o componente de quatro nós.
- **Do** definir toda cor nova como variável com par em `:root` e `.dark`.
- **Do** usar `cubic-bezier(0.16, 1, 0.3, 1)` em transições de 0.2s a 0.35s e desligar escala e animação em `prefers-reduced-motion`.
- **Do** marcar áreas de demonstração com o ponto tracejado (`demo-note`, chip `dashed`).

### Don't:
- **Don't** usar barra lateral fixa nem cards opacos de SaaS cinza; a navegação é a cápsula de vidro flutuante.
- **Don't** usar os halos ou o orbe como decoração de páginas internas; halos ficam no fundo, o orbe fica na entrada.
- **Don't** pintar pílulas de status inteiras de cor; a cor vive no ponto.
- **Don't** usar Fustat em texto corrido, botões ou rótulos.
- **Don't** pôr texto branco pequeno sobre `tone-green` ou `tone-green-dark`; o contraste não passa (cerca de 3.2:1 no claro).
- **Don't** criar opacidades de vidro fora das quatro definidas nem cores literais em componentes.
