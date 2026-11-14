(function () {
  'use strict';
  document.documentElement.classList.add('js');

  // Menu mobile
  var btn = document.querySelector('.menu-btn');
  var menu = document.getElementById('menu');
  function toggle(open) {
    menu.classList.toggle('open', open);
    btn.setAttribute('aria-expanded', String(open));
  }
  btn.addEventListener('click', function () { toggle(!menu.classList.contains('open')); });
  menu.addEventListener('click', function (e) { if (e.target.tagName === 'A') toggle(false); });

  // Animação de entrada
  var items = document.querySelectorAll('.reveal');
  if ('IntersectionObserver' in window) {
    var io = new IntersectionObserver(function (entries) {
      entries.forEach(function (en) {
        if (en.isIntersecting) { en.target.classList.add('in'); io.unobserve(en.target); }
      });
    }, { threshold: 0.12 });
    items.forEach(function (el) { io.observe(el); });
  } else {
    items.forEach(function (el) { el.classList.add('in'); });
  }

  // Filtro de conteúdo (Vídeos, Artigos & Podcasts)
  var tabs = document.querySelectorAll('.tabs button');
  var cards = document.querySelectorAll('.card');
  tabs.forEach(function (tab) {
    tab.addEventListener('click', function () {
      var f = tab.dataset.f;
      tabs.forEach(function (t) { t.setAttribute('aria-pressed', String(t === tab)); });
      cards.forEach(function (c) { c.classList.toggle('hide', f !== 'all' && c.dataset.t !== f); });
    });
  });

  // Formulário de contato: abre o app de e-mail do visitante.
  var form = document.getElementById('contato');
  var msg = form.querySelector('.form-msg');
  var DESTINO = 'contato@exemplo.com.br'; // TODO: e-mail do autor
  form.addEventListener('submit', function (e) {
    e.preventDefault();
    var d = new FormData(form);
    var nome = (d.get('nome') || '').trim();
    var email = (d.get('email') || '').trim();
    var texto = (d.get('mensagem') || '').trim();
    if (!nome || !/^\S+@\S+\.\S+$/.test(email) || !texto) {
      msg.className = 'form-msg err';
      msg.textContent = 'Preencha nome, um e-mail válido e a mensagem.';
      return;
    }
    var body = texto + '\n\n— ' + nome + ' (' + email + ')';
    window.location.href = 'mailto:' + DESTINO +
      '?subject=' + encodeURIComponent('Contato pelo site Administração Emocional') +
      '&body=' + encodeURIComponent(body);
    msg.className = 'form-msg ok';
    msg.textContent = 'Abrindo seu aplicativo de e-mail…';
  });

  document.getElementById('ano').textContent = new Date().getFullYear();
})();
