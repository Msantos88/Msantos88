'use strict';

const $ = (sel) => document.querySelector(sel);
const brl = (v) => Number(v).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
const DIAS_SEMANA = ['SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'];
const ROTULO_DIA = ['Dom', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb'];

const estado = { info: null, servico: null, barbeiro: null, data: null, horario: null, agendamento: null };
let timerPolling = null;
let timerContador = null;

async function api(caminho, opcoes = {}) {
  const resp = await fetch('/api' + caminho, {
    headers: { 'Content-Type': 'application/json' },
    ...opcoes,
  });
  const corpo = resp.status === 204 ? null : await resp.json().catch(() => null);
  if (!resp.ok) throw new Error((corpo && corpo.mensagem) || 'Algo deu errado. Tente novamente.');
  return corpo;
}

function mostrarErro(msg) {
  const el = $('#erro');
  el.textContent = msg || '';
  el.hidden = !msg;
  if (msg) el.scrollIntoView({ behavior: 'smooth', block: 'center' });
}

function irPara(passo) {
  mostrarErro(null);
  for (let i = 1; i <= 4; i++) {
    $('#etapa-' + i).hidden = i !== passo;
    const li = document.querySelector(`.passos li[data-passo="${i}"]`);
    li.classList.toggle('ativo', i === passo);
    li.classList.toggle('feito', i < passo);
  }
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

function isoData(d) {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

function formatarDataHora(iso) {
  const d = new Date(iso);
  return d.toLocaleDateString('pt-BR', { weekday: 'long', day: '2-digit', month: 'long' }) +
    ' às ' + d.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
}

function el(tag, attrs = {}, ...filhos) {
  const e = document.createElement(tag);
  for (const [k, v] of Object.entries(attrs)) {
    if (k === 'class') e.className = v;
    else if (k.startsWith('on')) e.addEventListener(k.slice(2), v);
    else e.setAttribute(k, v);
  }
  for (const f of filhos) e.append(f);
  return e;
}

function resumo(linhas) {
  const dl = el('dl');
  for (const [rotulo, valor, destaque] of linhas) {
    dl.append(el('dt', {}, rotulo), el('dd', destaque ? { class: 'destaque' } : {}, valor));
  }
  return dl;
}

// ---------------------------------------------------------------- etapa 1

function selecionarUnico(container, botao) {
  container.querySelectorAll('.sel').forEach((b) => b.classList.remove('sel'));
  botao.classList.add('sel');
}

function atualizarBotao1() {
  $('#ir-2').disabled = !(estado.servico && estado.barbeiro);
}

async function carregarEtapa1() {
  const [servicos, barbeiros] = await Promise.all([api('/servicos'), api('/barbeiros')]);
  const ls = $('#lista-servicos');
  ls.replaceChildren(...servicos.map((s) => {
    const b = el('button', { class: 'card', type: 'button' },
      el('div', { class: 'nome' }, s.nome),
      el('div', { class: 'desc' }, s.descricao || ''),
      el('div', { class: 'preco' }, el('b', {}, brl(s.preco)), el('span', { class: 'dur' }, `${s.duracaoMinutos} min`)),
    );
    b.addEventListener('click', () => { estado.servico = s; selecionarUnico(ls, b); atualizarBotao1(); });
    return b;
  }));

  const lb = $('#lista-barbeiros');
  const opcoes = barbeiros.map((bb) => {
    const b = el('button', { class: 'chip', type: 'button' }, bb.nome);
    b.addEventListener('click', () => { estado.barbeiro = bb; selecionarUnico(lb, b); atualizarBotao1(); });
    return b;
  });
  lb.replaceChildren(...opcoes);
  if (barbeiros.length === 1) opcoes[0].click();
}

// ---------------------------------------------------------------- etapa 2

function montarDias() {
  const ld = $('#lista-dias');
  const hoje = new Date();
  const botoes = [];
  for (let i = 0; i <= estado.info.diasMaximosParaAgendar; i++) {
    const d = new Date(hoje.getFullYear(), hoje.getMonth(), hoje.getDate() + i);
    if (!estado.info.diasDeFuncionamento.includes(DIAS_SEMANA[d.getDay()])) continue;
    const rotulo = i === 0 ? 'Hoje' : i === 1 ? 'Amanhã' : ROTULO_DIA[d.getDay()];
    const b = el('button', { class: 'dia', type: 'button' },
      el('small', {}, rotulo), el('span', {}, String(d.getDate())),
      el('small', {}, d.toLocaleDateString('pt-BR', { month: 'short' }).replace('.', '')));
    b.addEventListener('click', () => { selecionarUnico(ld, b); estado.data = isoData(d); carregarHorarios(); });
    botoes.push(b);
  }
  ld.replaceChildren(...botoes);
}

async function carregarHorarios() {
  estado.horario = null;
  $('#ir-3').disabled = true;
  const lh = $('#lista-horarios');
  lh.replaceChildren(el('p', { class: 'vazio' }, 'Carregando…'));
  try {
    const q = new URLSearchParams({ barbeiroId: estado.barbeiro.id, servicoId: estado.servico.id, data: estado.data });
    const horarios = await api('/horarios?' + q);
    if (!horarios.length) {
      lh.replaceChildren(el('p', { class: 'vazio' }, 'Nenhum horário livre neste dia. Tente outro dia.'));
      return;
    }
    lh.replaceChildren(...horarios.map((h) => {
      const b = el('button', { class: 'chip', type: 'button' }, h.slice(0, 5));
      b.addEventListener('click', () => { estado.horario = h.slice(0, 5); selecionarUnico(lh, b); $('#ir-3').disabled = false; });
      return b;
    }));
  } catch (e) {
    lh.replaceChildren();
    mostrarErro(e.message);
  }
}

// ---------------------------------------------------------------- etapa 3

function prepararEtapa3() {
  const inicio = `${estado.data}T${estado.horario}:00`;
  $('#resumo').replaceChildren(resumo([
    ['Serviço', estado.servico.nome],
    ['Barbeiro', estado.barbeiro.nome],
    ['Quando', formatarDataHora(inicio)],
    ['Valor total', brl(estado.servico.preco)],
    [`Sinal (${estado.info.percentualSinal}%)`, brl(estado.servico.sinal), true],
  ]));
  $('#valor-sinal-form').textContent = brl(estado.servico.sinal);
  $('#valor-restante-form').textContent = brl(estado.servico.preco - estado.servico.sinal);
  $('#minutos-pagar').textContent = estado.info.minutosParaPagarSinal;
}

async function reservar(ev) {
  ev.preventDefault();
  const form = ev.target;
  if (!form.reportValidity()) return;
  const dados = Object.fromEntries(new FormData(form));
  const botao = $('#btn-reservar');
  botao.disabled = true;
  botao.textContent = 'Gerando PIX…';
  try {
    const ag = await api('/agendamentos', {
      method: 'POST',
      body: JSON.stringify({
        servicoId: estado.servico.id,
        barbeiroId: estado.barbeiro.id,
        inicio: `${estado.data}T${estado.horario}:00`,
        ...dados,
      }),
    });
    history.replaceState(null, '', '?agendamento=' + ag.id);
    exibirAgendamento(ag);
  } catch (e) {
    mostrarErro(e.message);
  } finally {
    botao.disabled = false;
    botao.textContent = 'Reservar e pagar sinal';
  }
}

// ---------------------------------------------------------------- etapa 4

function exibirAgendamento(ag) {
  estado.agendamento = ag;
  irPara(4);
  $('#pagamento-pendente').hidden = ag.status !== 'AGUARDANDO_PAGAMENTO';
  $('#pagamento-ok').hidden = !['CONFIRMADO', 'CONCLUIDO'].includes(ag.status);
  $('#pagamento-falhou').hidden = !['EXPIRADO', 'CANCELADO', 'REEMBOLSO_PENDENTE'].includes(ag.status);

  const linhas = [
    ['Serviço', ag.servico],
    ['Barbeiro', ag.barbeiro],
    ['Quando', formatarDataHora(ag.inicio)],
    ['Valor total', brl(ag.valorTotal)],
  ];

  if (ag.status === 'AGUARDANDO_PAGAMENTO') {
    $('#resumo-pagamento').replaceChildren(resumo([...linhas, ['Sinal a pagar agora', brl(ag.valorSinal), true]]));
    const pix = ag.pix || {};
    $('#pix-codigo').value = pix.copiaECola || '';
    $('#pix-qr').hidden = !pix.qrCodeBase64;
    if (pix.qrCodeBase64) $('#pix-qr').src = 'data:image/png;base64,' + pix.qrCodeBase64;
    $('#pix-sem-qr').hidden = !!pix.qrCodeBase64;
    $('#pix-link').hidden = !pix.url;
    if (pix.url) $('#pix-link').href = pix.url;
    $('#btn-simular').hidden = !estado.info.pagamentoSimulado;
    iniciarContador(ag.pagarAte);
    iniciarPolling(ag.id);
  } else {
    pararTimers();
  }

  if (['CONFIRMADO', 'CONCLUIDO'].includes(ag.status)) {
    $('#resumo-ok').replaceChildren(resumo([
      ...linhas,
      ['Sinal pago', brl(ag.valorSinal)],
      ['Pagar na barbearia', brl(ag.valorRestante), true],
    ]));
    $('#link-agendamento').value = location.href;
  }
  if (ag.status === 'CANCELADO') {
    $('#falha-titulo').textContent = 'Agendamento cancelado';
    $('#falha-texto').textContent = 'Este agendamento foi cancelado. Em caso de dúvida, fale com a barbearia.';
  }
  if (ag.status === 'REEMBOLSO_PENDENTE') {
    $('#falha-titulo').textContent = 'Pagamento recebido após o prazo';
    $('#falha-texto').textContent = 'Seu PIX chegou depois que o horário foi liberado e ele já foi ocupado. ' +
      'A barbearia fará a devolução do sinal. Você pode escolher outro horário.';
  }
}

function pararTimers() {
  clearInterval(timerPolling);
  clearInterval(timerContador);
}

function iniciarPolling(id) {
  clearInterval(timerPolling);
  timerPolling = setInterval(async () => {
    try {
      const ag = await api('/agendamentos/' + id);
      if (ag.status !== 'AGUARDANDO_PAGAMENTO') exibirAgendamento(ag);
    } catch (_) { /* tenta de novo no próximo ciclo */ }
  }, 4000);
}

function iniciarContador(pagarAte) {
  clearInterval(timerContador);
  const limite = new Date(pagarAte).getTime();
  const tick = () => {
    const restante = Math.max(0, limite - Date.now());
    const m = Math.floor(restante / 60000);
    const s = Math.floor((restante % 60000) / 1000);
    $('#contador').textContent = `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
  };
  tick();
  timerContador = setInterval(tick, 1000);
}

async function copiarPix() {
  const campo = $('#pix-codigo');
  try {
    await navigator.clipboard.writeText(campo.value);
  } catch (_) {
    campo.select();
    document.execCommand('copy');
  }
  $('#btn-copiar').textContent = 'Copiado!';
  setTimeout(() => { $('#btn-copiar').textContent = 'Copiar código'; }, 2000);
}

async function simularPagamento() {
  try {
    exibirAgendamento(await api(`/simulador/agendamentos/${estado.agendamento.id}/pagar`, { method: 'POST' }));
  } catch (e) {
    mostrarErro(e.message);
  }
}

// ---------------------------------------------------------------- início

async function iniciar() {
  try {
    estado.info = await api('/info');
    document.title = `Agendamento | ${estado.info.nome}`;
    $('#nome-barbearia').textContent = estado.info.nome;
    $('#pct-sinal').textContent = estado.info.percentualSinal + '%';

    $('#ir-2').addEventListener('click', () => { montarDias(); $('#lista-horarios').replaceChildren(el('p', { class: 'vazio' }, 'Selecione um dia.')); irPara(2); });
    $('#ir-3').addEventListener('click', () => { prepararEtapa3(); irPara(3); });
    document.querySelectorAll('[data-voltar]').forEach((b) =>
      b.addEventListener('click', () => irPara(Number(b.dataset.voltar))));
    $('#form-cliente').addEventListener('submit', reservar);
    $('#btn-copiar').addEventListener('click', copiarPix);
    $('#btn-simular').addEventListener('click', simularPagamento);

    const id = new URLSearchParams(location.search).get('agendamento');
    if (id) {
      exibirAgendamento(await api('/agendamentos/' + encodeURIComponent(id)));
    }
    await carregarEtapa1();
  } catch (e) {
    mostrarErro(e.message);
  }
}

iniciar();
