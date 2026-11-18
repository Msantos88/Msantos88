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

  // Formulário de contato: envio direto pelo site (enviar.php), sem abrir app de email.
  var form = document.getElementById('contato');
  var msg = form.querySelector('.form-msg');
  var submit = form.querySelector('button[type=submit]');
  var EMAIL_FALLBACK = ''; // opcional: email exibido se o envio falhar
  form.elements.t.value = Math.floor(Date.now() / 1000);

  function aviso(tipo, texto) { msg.className = 'form-msg ' + tipo; msg.textContent = texto; }

  form.addEventListener('submit', function (e) {
    e.preventDefault();
    var nome = form.elements.nome.value.trim();
    var email = form.elements.email.value.trim();
    var texto = form.elements.mensagem.value.trim();
    if (!nome || !/^\S+@\S+\.\S+$/.test(email) || !texto) {
      aviso('err', 'Preencha nome, um email válido e a mensagem.');
      return;
    }
    submit.disabled = true;
    aviso('', 'Enviando…');
    fetch(form.getAttribute('action'), { method: 'POST', body: new FormData(form), headers: { 'Accept': 'application/json' } })
      .then(function (r) { return r.json().catch(function () { return { ok: false, mensagem: '' }; }); })
      .then(function (d) {
        if (d.ok) {
          aviso('ok', d.mensagem || 'Mensagem enviada!');
          form.reset();
          form.elements.t.value = Math.floor(Date.now() / 1000);
        } else {
          aviso('err', d.mensagem || 'Não foi possível enviar agora. Tente novamente em instantes.');
        }
      })
      .catch(function () {
        aviso('err', 'Não foi possível enviar agora. Verifique sua conexão e tente novamente' + (EMAIL_FALLBACK ? ', ou escreva para ' + EMAIL_FALLBACK + '.' : '.'));
      })
      .then(function () { submit.disabled = false; });
  });

  // Vídeos: o cartão aparece sozinho quando o arquivo existe no servidor
  var player = document.getElementById('player');
  var pv = player.querySelector('video');
  document.querySelectorAll('.vcard').forEach(function (card) {
    fetch(card.dataset.video, { method: 'HEAD' }).then(function (r) {
      if (!r.ok) return;
      var thumb = card.querySelector('.thumb');
      var img = new Image();
      img.alt = '';
      img.loading = 'lazy';
      img.onerror = function () {
        // Sem imagem de capa: usa um quadro do próprio vídeo
        img.remove();
        var v = document.createElement('video');
        v.muted = true; v.playsInline = true; v.preload = 'metadata'; v.tabIndex = -1;
        v.setAttribute('aria-hidden', 'true');
        v.src = card.dataset.video + '#t=1';
        thumb.prepend(v);
      };
      img.src = card.dataset.poster;
      thumb.prepend(img);
      card.hidden = false;
    }).catch(function () {});
    card.addEventListener('click', function () {
      pv.poster = card.dataset.poster || '';
      pv.src = card.dataset.video;
      if (typeof player.showModal === 'function') player.showModal(); else player.setAttribute('open', '');
      pv.play().catch(function () {});
    });
  });
  function fecharPlayer() { pv.pause(); pv.removeAttribute('src'); pv.load(); if (player.open) player.close(); }
  player.querySelector('.player-close').addEventListener('click', fecharPlayer);
  player.addEventListener('click', function (e) { if (e.target === player) fecharPlayer(); });
  player.addEventListener('close', function () { pv.pause(); });

  // Mostra apenas as lojas que já têm link preenchido
  var lojas = document.querySelectorAll('li[data-pendente]');
  var algum = false;
  lojas.forEach(function (li) {
    var a = li.querySelector('a');
    if (a.getAttribute('href')) { li.hidden = false; algum = true; }
  });
  document.querySelectorAll('[data-more]').forEach(function (el) { el.hidden = !algum; });

  // Links para o topo: rolam até y=0 de forma confiável
  document.querySelectorAll('a[href="#topo"]').forEach(function (a) {
    a.addEventListener('click', function (e) {
      e.preventDefault();
      window.scrollTo({ top: 0, behavior: reduce ? 'auto' : 'smooth' });
      toggle(false);
    });
  });

  document.getElementById('ano').textContent = new Date().getFullYear();
})();
