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


  var reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  // Barra de progresso + botão fixo
  var bar = document.querySelector('.progress');
  var sticky = document.querySelector('.dock');
  var hero = document.querySelector('.hero');
  var buy = document.getElementById('comprar');
  var ticking = false;
  function onScroll() {
    var max = document.documentElement.scrollHeight - window.innerHeight;
    bar.style.transform = 'scaleX(' + (max > 0 ? window.scrollY / max : 0) + ')';
    var past = window.scrollY > hero.offsetHeight * 0.8;
    var inBuy = buy.getBoundingClientRect().top < window.innerHeight * 0.6;
    sticky.classList.toggle('show', past && !inBuy);
    ticking = false;
  }
  window.addEventListener('scroll', function () {
    if (!ticking) { ticking = true; requestAnimationFrame(onScroll); }
  }, { passive: true });
  onScroll();

  // Tilt 3D da capa + parallax do cérebro
  if (!reduce && window.matchMedia('(hover: hover)').matches) {
    var tilt = document.querySelector('.tilt');
    var art = document.querySelector('.hero-art');
    art.addEventListener('mousemove', function (e) {
      var r = art.getBoundingClientRect();
      var x = (e.clientX - r.left) / r.width - 0.5;
      var y = (e.clientY - r.top) / r.height - 0.5;
      tilt.style.transform = 'rotateY(' + (x * 18) + 'deg) rotateX(' + (-y * 14) + 'deg)';
    });
    art.addEventListener('mouseleave', function () { tilt.style.transform = ''; });
  }
  var brain = document.querySelector('#livro .col-art img');
  if (brain && !reduce) {
    window.addEventListener('scroll', function () {
      var r = brain.getBoundingClientRect();
      if (r.bottom < 0 || r.top > window.innerHeight) return;
      var p = (r.top + r.height / 2 - window.innerHeight / 2) / window.innerHeight;
      brain.style.transform = 'translateY(' + (p * -30) + 'px) rotate(' + (p * 2) + 'deg)';
    }, { passive: true });
  }

  // Contador animado
  var counters = document.querySelectorAll('[data-count]');
  if ('IntersectionObserver' in window && !reduce) {
    var co = new IntersectionObserver(function (es) {
      es.forEach(function (en) {
        if (!en.isIntersecting) return;
        var el = en.target, end = +el.dataset.count, t0 = null;
        (function step(t) {
          t0 = t0 || t;
          var k = Math.min((t - t0) / 1400, 1);
          el.textContent = Math.round(end * (1 - Math.pow(1 - k, 3)));
          if (k < 1) requestAnimationFrame(step);
        })(performance.now());
        co.unobserve(el);
      });
    }, { threshold: 0.6 });
    counters.forEach(function (c) { c.textContent = '0'; co.observe(c); });
  }

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
