'use strict';

const $ = (s) => document.querySelector(s);
const brl = (v) => Number(v).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
const hora = (iso) => iso.slice(11, 16);
const ROTULOS = {
  AGUARDANDO_PAGAMENTO: 'Aguardando sinal', CONFIRMADO: 'Confirmado', CONCLUIDO: 'Concluído',
  EXPIRADO: 'Expirado', CANCELADO: 'Cancelado', REEMBOLSO_PENDENTE: 'Devolver sinal',
};

function csrf() {
  const m = document.cookie.match(/(?:^|; )XSRF-TOKEN=([^;]+)/);
  return m ? decodeURIComponent(m[1]) : '';
}

async function api(caminho, opcoes = {}) {
  const resp = await fetch('/api/admin' + caminho, {
    ...opcoes,
    headers: { 'Content-Type': 'application/json', 'X-XSRF-TOKEN': csrf() },
  });
  const corpo = await resp.json().catch(() => null);
  if (resp.status === 429) throw new Error((corpo && corpo.mensagem) || 'Muitas tentativas. Aguarde.');
  if (!resp.ok) throw new Error((corpo && corpo.mensagem) || `Erro ${resp.status}`);
  return corpo;
}

function td(rotulo, ...conteudo) {
  const c = document.createElement('td');
  c.dataset.rotulo = rotulo;
  c.append(...conteudo);
  return c;
}

function botao(texto, acao) {
  const b = document.createElement('button');
  b.className = 'btn sec mini';
  b.textContent = texto;
  b.addEventListener('click', acao);
  return b;
}

async function acao(id, tipo, confirmacao) {
  if (!confirm(confirmacao)) return;
  try {
    await api(`/agendamentos/${id}/${tipo}`, { method: 'POST' });
    carregar();
  } catch (e) {
    alert(e.message);
  }
}

async function carregar() {
  const erro = $('#erro');
  erro.hidden = true;
  try {
    const filtro = $('#filtro-barbeiro').value;
    const lista = (await api('/agendamentos?data=' + $('#data').value))
      .filter((a) => !filtro || a.barbeiro === filtro);
    const visiveis = lista.filter((a) => a.status !== 'EXPIRADO');
    $('#vazio').hidden = visiveis.length > 0;
    $('#linhas').replaceChildren(...visiveis.map((a) => {
      const tr = document.createElement('tr');
      const selo = document.createElement('span');
      selo.className = 'selo ' + a.status;
      selo.textContent = ROTULOS[a.status] || a.status;
      const contato = document.createElement('div');
      contato.append(a.clienteNome, document.createElement('br'));
      const tel = document.createElement('a');
      tel.href = 'https://wa.me/55' + a.clienteTelefone.replace(/\D/g, '').replace(/^55/, '');
      tel.target = '_blank';
      tel.rel = 'noopener';
      tel.textContent = a.clienteTelefone;
      contato.append(tel);
      const acoes = document.createElement('div');
      if (a.status === 'CONFIRMADO') {
        acoes.append(botao('Concluir', () => acao(a.id, 'concluir', `Marcar atendimento de ${a.clienteNome} como concluído?`)));
      }
      if (['CONFIRMADO', 'AGUARDANDO_PAGAMENTO'].includes(a.status)) {
        acoes.append(' ', botao('Cancelar', () => acao(a.id, 'cancelar',
          `Cancelar agendamento de ${a.clienteNome}? Se o sinal já foi pago, faça a devolução pelo Mercado Pago.`)));
      }
      tr.append(
        td('Horário', `${hora(a.inicio)}–${hora(a.fim)}`),
        td('Cliente', contato),
        td('Serviço', a.servico),
        td('Barbeiro', a.barbeiro),
        td('Status', selo),
        td('Valores', `Total ${brl(a.valorTotal)} · Sinal ${brl(a.valorSinal)}`),
        td('', acoes),
      );
      return tr;
    }));
    const confirmados = lista.filter((a) => ['CONFIRMADO', 'CONCLUIDO'].includes(a.status));
    $('#t-confirmados').textContent = confirmados.length;
    $('#t-sinais').textContent = brl(confirmados.reduce((s, a) => s + Number(a.valorSinal), 0));
    $('#t-receber').textContent = brl(lista.filter((a) => a.status === 'CONFIRMADO')
      .reduce((s, a) => s + Number(a.valorRestante), 0));
  } catch (e) {
    erro.textContent = e.message;
    erro.hidden = false;
  }
}

function mudarDia(delta) {
  const d = $('#data').valueAsDate || new Date();
  d.setUTCDate(d.getUTCDate() + delta);
  $('#data').valueAsDate = d;
  carregar();
}

function hojeLocal() {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

// ---------------------------------------------------------------- abas

function mostrarAba(nome) {
  document.querySelectorAll('.aba').forEach((b) => {
    const ativa = b.dataset.aba === nome;
    b.classList.toggle('ativa', ativa);
    if (ativa) b.setAttribute('aria-current', 'page'); else b.removeAttribute('aria-current');
  });
  $('#aba-agenda').hidden = nome !== 'agenda';
  $('#aba-barbeiros').hidden = nome !== 'barbeiros';
  $('#erro').hidden = true;
  if (nome === 'barbeiros') carregarEquipe();
}

// ---------------------------------------------------------------- barbeiros

function mostrarErro(msg) {
  $('#erro').textContent = msg;
  $('#erro').hidden = false;
}

async function mudarBarbeiro(caminho, opcoes) {
  try {
    await api('/barbeiros' + caminho, opcoes);
    $('#erro').hidden = true;
    await carregarEquipe();
  } catch (e) {
    mostrarErro(e.message);
  }
}

async function carregarEquipe() {
  try {
    const equipe = await api('/barbeiros');
    atualizarFiltroBarbeiro(equipe);
    $('#lista-equipe').replaceChildren(...equipe.map((b) => {
      const li = document.createElement('li');
      if (!b.ativo) li.className = 'inativo';
      const nome = document.createElement('span');
      nome.className = 'quem';
      nome.textContent = b.nome;
      const selo = document.createElement('span');
      selo.className = 'selo ' + (b.ativo ? 'CONFIRMADO' : 'CANCELADO');
      selo.textContent = b.ativo ? 'Ativo' : 'Inativo';
      const acoes = document.createElement('span');
      acoes.className = 'acoes-linha';
      acoes.append(botao('Renomear', () => {
        const novo = prompt('Novo nome:', b.nome);
        if (novo && novo.trim() && novo.trim() !== b.nome) {
          mudarBarbeiro('/' + b.id, { method: 'PUT', body: JSON.stringify({ nome: novo.trim() }) });
        }
      }));
      acoes.append(b.ativo
        ? botao('Desativar', () => confirm(`Tirar ${b.nome} da página de agendamento?`)
            && mudarBarbeiro(`/${b.id}/desativar`, { method: 'POST' }))
        : botao('Reativar', () => mudarBarbeiro(`/${b.id}/ativar`, { method: 'POST' })));
      li.append(nome, selo, acoes);
      return li;
    }));
  } catch (e) {
    mostrarErro(e.message);
  }
}

function atualizarFiltroBarbeiro(equipe) {
  const sel = $('#filtro-barbeiro');
  const atual = sel.value;
  const opcoes = [new Option('Todos', '')].concat(equipe.map((b) => new Option(b.nome, b.nome)));
  sel.replaceChildren(...opcoes);
  sel.value = equipe.some((b) => b.nome === atual) ? atual : '';
  $('#filtro-barbeiro-rotulo').hidden = equipe.length < 2;
}

$('#form-barbeiro').addEventListener('submit', async (ev) => {
  ev.preventDefault();
  const campo = ev.target.nome;
  if (!campo.value.trim()) return;
  await mudarBarbeiro('', { method: 'POST', body: JSON.stringify({ nome: campo.value.trim() }) });
  if ($('#erro').hidden) campo.value = '';
});
document.querySelectorAll('.aba').forEach((b) => b.addEventListener('click', () => mostrarAba(b.dataset.aba)));
$('#filtro-barbeiro').addEventListener('change', carregar);
api('/barbeiros').then(atualizarFiltroBarbeiro).catch(() => {});

$('#data').value = hojeLocal();
$('#data').addEventListener('change', carregar);
$('#anterior').addEventListener('click', () => mudarDia(-1));
$('#proximo').addEventListener('click', () => mudarDia(1));
$('#hoje').addEventListener('click', () => { // ---------------------------------------------------------------- abas

function mostrarAba(nome) {
  document.querySelectorAll('.aba').forEach((b) => {
    const ativa = b.dataset.aba === nome;
    b.classList.toggle('ativa', ativa);
    if (ativa) b.setAttribute('aria-current', 'page'); else b.removeAttribute('aria-current');
  });
  $('#aba-agenda').hidden = nome !== 'agenda';
  $('#aba-barbeiros').hidden = nome !== 'barbeiros';
  $('#erro').hidden = true;
  if (nome === 'barbeiros') carregarEquipe();
}

// ---------------------------------------------------------------- barbeiros

function mostrarErro(msg) {
  $('#erro').textContent = msg;
  $('#erro').hidden = false;
}

async function mudarBarbeiro(caminho, opcoes) {
  try {
    await api('/barbeiros' + caminho, opcoes);
    $('#erro').hidden = true;
    await carregarEquipe();
  } catch (e) {
    mostrarErro(e.message);
  }
}

async function carregarEquipe() {
  try {
    const equipe = await api('/barbeiros');
    atualizarFiltroBarbeiro(equipe);
    $('#lista-equipe').replaceChildren(...equipe.map((b) => {
      const li = document.createElement('li');
      if (!b.ativo) li.className = 'inativo';
      const nome = document.createElement('span');
      nome.className = 'quem';
      nome.textContent = b.nome;
      const selo = document.createElement('span');
      selo.className = 'selo ' + (b.ativo ? 'CONFIRMADO' : 'CANCELADO');
      selo.textContent = b.ativo ? 'Ativo' : 'Inativo';
      const acoes = document.createElement('span');
      acoes.className = 'acoes-linha';
      acoes.append(botao('Renomear', () => {
        const novo = prompt('Novo nome:', b.nome);
        if (novo && novo.trim() && novo.trim() !== b.nome) {
          mudarBarbeiro('/' + b.id, { method: 'PUT', body: JSON.stringify({ nome: novo.trim() }) });
        }
      }));
      acoes.append(b.ativo
        ? botao('Desativar', () => confirm(`Tirar ${b.nome} da página de agendamento?`)
            && mudarBarbeiro(`/${b.id}/desativar`, { method: 'POST' }))
        : botao('Reativar', () => mudarBarbeiro(`/${b.id}/ativar`, { method: 'POST' })));
      li.append(nome, selo, acoes);
      return li;
    }));
  } catch (e) {
    mostrarErro(e.message);
  }
}

function atualizarFiltroBarbeiro(equipe) {
  const sel = $('#filtro-barbeiro');
  const atual = sel.value;
  const opcoes = [new Option('Todos', '')].concat(equipe.map((b) => new Option(b.nome, b.nome)));
  sel.replaceChildren(...opcoes);
  sel.value = equipe.some((b) => b.nome === atual) ? atual : '';
  $('#filtro-barbeiro-rotulo').hidden = equipe.length < 2;
}

$('#form-barbeiro').addEventListener('submit', async (ev) => {
  ev.preventDefault();
  const campo = ev.target.nome;
  if (!campo.value.trim()) return;
  await mudarBarbeiro('', { method: 'POST', body: JSON.stringify({ nome: campo.value.trim() }) });
  if ($('#erro').hidden) campo.value = '';
});
document.querySelectorAll('.aba').forEach((b) => b.addEventListener('click', () => mostrarAba(b.dataset.aba)));
$('#filtro-barbeiro').addEventListener('change', carregar);
api('/barbeiros').then(atualizarFiltroBarbeiro).catch(() => {});

$('#data').value = hojeLocal(); carregar(); });
carregar();
setInterval(carregar, 60000);
