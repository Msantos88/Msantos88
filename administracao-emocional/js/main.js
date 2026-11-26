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

  // ===== Conteúdo editável (arquivo conteudo.js) =====
  var C = window.CONTEUDO;
  if (!C && window.console) console.error('[site] conteudo.js não carregou: usando o conteúdo padrão.');
  C = C || {};
  function lista(x) { return Array.isArray(x) ? x : []; }
  function el(tag, cls, txt) {
    var e = document.createElement(tag);
    if (cls) e.className = cls;
    if (txt != null) e.textContent = txt;
    return e;
  }
  function urlSegura(u) { u = typeof u === 'string' ? u.trim() : ''; return /^https?:\/\//i.test(u) ? u : ''; }
  function nomeArquivo(n) { return String(n || '').replace(/[^A-Za-z0-9._\-]/g, ''); }
  function idYoutube(v) {
    v = String(v || '').trim();
    var m = v.match(/(?:youtu\.be\/|[?&]v=|\/embed\/|\/shorts\/|\/live\/)([A-Za-z0-9_\-]{11})/);
    if (m) return m[1];
    return /^[A-Za-z0-9_\-]{11}$/.test(v) ? v : '';
  }

  // Depoimentos
  var dep = lista(C.depoimentos).filter(function (d) { return d && d.texto; });
  var depBox = document.getElementById('lista-depoimentos');
  if (dep.length && depBox) {
    depBox.textContent = '';
    function figuraDepoimento(d, classe) {
      var f = el('figure', classe);
      f.appendChild(el('blockquote', '', String(d.texto).replace(/^[“"\s]+|[”"\s]+$/g, '')));
      var cap = el('figcaption');
      cap.appendChild(el('strong', '', d.nome || ''));
      if (d.cargo) cap.appendChild(el('span', '', d.cargo));
      f.appendChild(cap);
      return f;
    }
    if (dep.length === 1) {
      var grid = el('div', 'quotes');
      grid.setAttribute('data-n', '1');
      grid.appendChild(figuraDepoimento(dep[0], 'quote reveal'));
      depBox.appendChild(grid);
    } else {
      // Carrossel: arrastar (celular), setas, bolinhas e passagem automática lenta
      var car = el('div', 'depo-car reveal');
      car.setAttribute('role', 'region');
      car.setAttribute('aria-roledescription', 'carrossel');
      car.setAttribute('aria-label', 'Depoimentos');
      var track = el('div', 'car-track');
      track.tabIndex = 0;
      var slides = dep.map(function (d) { var s = figuraDepoimento(d, 'quote car-slide'); track.appendChild(s); return s; });
      var ctrl = el('div', 'car-ctrl');
      var prev = el('button', 'car-btn', '‹');
      prev.type = 'button';
      prev.setAttribute('aria-label', 'Depoimento anterior');
      var next = el('button', 'car-btn', '›');
      next.type = 'button';
      next.setAttribute('aria-label', 'Próximo depoimento');
      var dots = el('div', 'car-dots');
      var bolas = slides.map(function (s, i) {
        var d = el('button', 'car-dot');
        d.type = 'button';
        d.setAttribute('aria-label', 'Ir para o depoimento ' + (i + 1));
        d.addEventListener('click', function () { parar(); ir(i); });
        dots.appendChild(d);
        return d;
      });
      ctrl.appendChild(prev); ctrl.appendChild(dots); ctrl.appendChild(next);
      car.appendChild(track); car.appendChild(ctrl);
      depBox.appendChild(car);

      var atual = 0, timer = null, parado = false, pausado = false, visivel = false;
      function ir(i) {
        i = (i + slides.length) % slides.length;
        track.scrollTo({ left: slides[i].offsetLeft - track.offsetLeft, behavior: reduce ? 'auto' : 'smooth' });
      }
      function marcar() {
        var melhor = 0, dist = Infinity;
        slides.forEach(function (s, i) {
          var d = Math.abs((s.offsetLeft - track.offsetLeft) - track.scrollLeft);
          if (d < dist) { dist = d; melhor = i; }
        });
        atual = melhor;
        bolas.forEach(function (b, i) { b.classList.toggle('on', i === melhor); if (i === melhor) b.setAttribute('aria-current', 'true'); else b.removeAttribute('aria-current'); });
      }
      var tick = false;
      track.addEventListener('scroll', function () {
        if (!tick) { tick = true; requestAnimationFrame(function () { marcar(); tick = false; }); }
      }, { passive: true });
      function parar() { parado = true; if (timer) { clearInterval(timer); timer = null; } }
      prev.addEventListener('click', function () { parar(); ir(atual - 1); });
      next.addEventListener('click', function () { parar(); ir(atual + 1); });
      track.addEventListener('keydown', function (e) {
        if (e.key === 'ArrowRight') { e.preventDefault(); parar(); ir(atual + 1); }
        if (e.key === 'ArrowLeft') { e.preventDefault(); parar(); ir(atual - 1); }
      });
      ['mouseenter', 'focusin', 'touchstart'].forEach(function (ev) { car.addEventListener(ev, function () { pausado = true; }, { passive: true }); });
      ['mouseleave', 'focusout', 'touchend'].forEach(function (ev) { car.addEventListener(ev, function () { pausado = false; }, { passive: true }); });
      track.addEventListener('pointerdown', function () { parar(); });
      if (!reduce) {
        timer = setInterval(function () { if (!parado && !pausado && visivel && !document.hidden) ir(atual + 1); }, 8000);
      }
      if ('IntersectionObserver' in window) {
        new IntersectionObserver(function (es) { visivel = es[0].isIntersecting; }, { threshold: 0.4 }).observe(car);
      } else { visivel = true; }
      marcar();
    }
    var cta = el('div', 'depo-cta reveal');
    cta.appendChild(el('p', '', 'Leu o livro? Conte o que achou.'));
    var ca = el('a', 'btn btn-primary', 'Enviar meu depoimento');
    ca.href = '#contato';
    cta.appendChild(ca);
    depBox.appendChild(cta);
  }

  // Vídeos, podcasts e posts
  var midia = document.getElementById('lista-midia');
  if (midia) {
    lista(C.videos).forEach(function (v) {
      var arq = nomeArquivo(v && v.arquivo);
      if (!arq) return;
      var b = el('button', 'card vcard reveal');
      b.type = 'button';
      b.setAttribute('data-t', 'video');
      b.setAttribute('data-video', 'assets/videos/' + arq);
      b.hidden = true;
      var th = el('span', 'thumb');
      var pl = el('span', 'play');
      pl.setAttribute('aria-hidden', 'true');
      th.appendChild(pl);
      b.appendChild(th);
      b.appendChild(el('span', 'tag tag-video', 'Vídeo'));
      b.appendChild(el('h3', '', v.titulo || 'Vídeo'));
      if (v.detalhe) b.appendChild(el('span', 'det', v.detalhe));
      b.appendChild(el('span', 'more', 'Assistir →'));
      midia.appendChild(b);
    });

    var pods = lista(C.podcasts).filter(function (p) { return p && idYoutube(p.youtube); });
    pods.forEach(function (p) {
      var b = el('button', 'card pcast reveal');
      b.type = 'button';
      b.setAttribute('data-t', 'podcast');
      b.setAttribute('data-yt', idYoutube(p.youtube));
      var th = el('span', 'thumb');
      if (p.imagem) {
        var capa = new Image();
        capa.src = 'assets/podcasts/' + nomeArquivo(p.imagem);
        capa.alt = '';
        capa.loading = 'lazy';
        capa.onerror = function () { capa.remove(); };
        th.appendChild(capa);
        th.classList.add('capa');
        capa.onerror = function () { capa.remove(); th.classList.remove('capa'); };
      }
      var pl = el('span', 'play');
      pl.setAttribute('aria-hidden', 'true');
      th.appendChild(pl);
      b.appendChild(th);
      b.appendChild(el('span', 'tag tag-podcast', 'Podcast'));
      b.appendChild(el('h3', '', p.titulo || 'Podcast'));
      if (p.detalhe) b.appendChild(el('span', 'det', p.detalhe));
      b.appendChild(el('span', 'more', 'Ouvir →'));
      midia.appendChild(b);
    });
    if (!pods.length) {
      var ph = el('div', 'card reveal placeholder');
      ph.setAttribute('data-t', 'podcast');
      ph.appendChild(el('span', 'tag tag-podcast', 'Podcast'));
      ph.appendChild(el('h3', '', 'Participações em podcast'));
      ph.appendChild(el('span', 'more muted', 'Em breve'));
      midia.appendChild(ph);
    }

    lista(C.posts).forEach(function (p) {
      if (!p || (!p.titulo && !p.imagem)) return;
      var a = el('a', 'card pcard reveal');
      a.setAttribute('data-t', 'post');
      a.href = urlSegura(p.link) || 'https://www.instagram.com/administracao.emocional/';
      a.target = '_blank';
      a.rel = 'noopener';
      if (p.imagem) {
        var pt = el('span', 'pthumb');
        var im = new Image();
        im.src = 'assets/posts/' + nomeArquivo(p.imagem);
        im.alt = 'Capa do post: ' + (p.titulo || '');
        im.loading = 'lazy';
        pt.appendChild(im);
        a.appendChild(pt);
      }
      a.appendChild(el('span', 'tag tag-artigo', 'Post'));
      a.appendChild(el('h3', '', p.titulo || 'Post'));
      a.appendChild(el('span', 'more', 'Ver no Instagram →'));
      midia.appendChild(a);
    });
  }

  // Onde comprar
  var lj = lista(C.lojas).filter(function (l) { return l && l.nome && urlSegura(l.link); });
  var ljPrin = document.getElementById('lojas-principais');
  var ljOut = document.getElementById('lojas-outras');
  var ljTit = document.getElementById('lojas-outras-titulo');
  if (lj.length && ljPrin && ljOut && ljTit) {
    ljPrin.textContent = '';
    ljOut.textContent = '';
    lj.forEach(function (l) {
      var li = el('li');
      var a = el('a', 'store' + (l.destaque ? ' primary' : ''));
      a.href = urlSegura(l.link);
      a.target = '_blank';
      a.rel = 'noopener';
      a.appendChild(el('strong', '', l.destaque ? (l.formato || l.nome) : l.nome));
      a.appendChild(el('span', '', l.destaque ? 'Comprar na ' + l.nome + ' →' : 'Comprar online →'));
      li.appendChild(a);
      (l.destaque ? ljPrin : ljOut).appendChild(li);
    });
    var temOutras = ljOut.children.length > 0;
    ljOut.hidden = !temOutras;
    ljTit.hidden = !temOutras;
  }

  // Podcast (YouTube sem cookies, só carrega ao clicar)
  var ytDlg = document.getElementById('player-yt');
  var ytBox = ytDlg ? ytDlg.querySelector('.yt-frame') : null;
  function fecharYt() { if (!ytDlg) return; ytBox.textContent = ''; if (ytDlg.open) ytDlg.close(); }
  if (ytDlg) {
    document.querySelectorAll('.pcast').forEach(function (card) {
      card.addEventListener('click', function () {
        var f = document.createElement('iframe');
        f.src = 'https://www.youtube-nocookie.com/embed/' + card.getAttribute('data-yt') + '?autoplay=1&rel=0&modestbranding=1';
        f.title = card.querySelector('h3').textContent;
        f.allow = 'autoplay; encrypted-media; picture-in-picture; fullscreen';
        f.referrerPolicy = 'strict-origin-when-cross-origin';
        f.setAttribute('allowfullscreen', '');
        ytBox.textContent = '';
        ytBox.appendChild(f);
        document.getElementById('player-yt-title').textContent = card.querySelector('h3').textContent;
        if (typeof ytDlg.showModal === 'function') ytDlg.showModal(); else ytDlg.setAttribute('open', '');
      });
    });
    ytDlg.querySelector('.player-close').addEventListener('click', fecharYt);
    ytDlg.addEventListener('click', function (e) { if (e.target === ytDlg) fecharYt(); });
    ytDlg.addEventListener('close', function () { ytBox.textContent = ''; });
  }

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

  // Filtro de conteúdo (Vídeos, Posts & Podcasts) e botão "Ver mais"
  var LIMITE = 6;           // quantos cartões aparecem antes do "Ver mais"
  var filtroAtual = 'all';
  var expandido = {};
  var tabs = document.querySelectorAll('.tabs button');
  var verMais = document.getElementById('ver-mais');
  function aplicarFiltroMidia() {
    var elegiveis = [];
    document.querySelectorAll('#lista-midia .card').forEach(function (c) {
      var ok = !c.hidden && (filtroAtual === 'all' || c.dataset.t === filtroAtual);
      if (ok) elegiveis.push(c); else c.classList.add('hide');
    });
    // só esconde cartões se sobrarem 3 ou mais (evita esconder 1 ou 2 itens)
    var recolher = !expandido[filtroAtual] && elegiveis.length > LIMITE + 2;
    elegiveis.forEach(function (c, i) { c.classList.toggle('hide', recolher && i >= LIMITE); });
    if (verMais) {
      verMais.hidden = !recolher;
      verMais.textContent = 'Ver mais (' + (elegiveis.length - LIMITE) + ')';
    }
  }
  tabs.forEach(function (tab) {
    tab.addEventListener('click', function () {
      filtroAtual = tab.dataset.f;
      tabs.forEach(function (t) { t.setAttribute('aria-pressed', String(t === tab)); });
      aplicarFiltroMidia();
    });
  });
  if (verMais) verMais.addEventListener('click', function () { expandido[filtroAtual] = true; aplicarFiltroMidia(); });
  aplicarFiltroMidia();

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
    var url = card.dataset.video;
    function revelar() {
      if (!card.hidden) return;
      // Capa = quadro do próprio vídeo (sem depender de imagem separada)
      var v = document.createElement('video');
      v.muted = true; v.playsInline = true; v.preload = 'metadata'; v.tabIndex = -1;
      v.setAttribute('aria-hidden', 'true');
      v.src = url + '#t=1';
      card.querySelector('.thumb').prepend(v);
      card.hidden = false;
      aplicarFiltroMidia();
    }
    // O cartão só aparece se o navegador conseguir abrir o vídeo (arquivo existe e formato é compatível)
    var teste = document.createElement('video');
    teste.preload = 'metadata';
    teste.muted = true;
    teste.addEventListener('loadedmetadata', revelar);
    teste.addEventListener('error', function () {
      if (window.console) console.warn('[video] não foi possível abrir ' + url + ' (código ' + (teste.error ? teste.error.code : '?') + ')');
    });
    teste.src = url;
    card.addEventListener('click', function () {
      pv.src = url;
      var tt = document.getElementById('player-title');
      if (tt) tt.textContent = card.querySelector('h3').textContent;
      if (typeof player.showModal === 'function') player.showModal(); else player.setAttribute('open', '');
      pv.play().catch(function () {});
    });
  });
  function fecharPlayer() { pv.pause(); pv.removeAttribute('src'); pv.load(); if (player.open) player.close(); }
  player.querySelector('.player-close').addEventListener('click', fecharPlayer);
  player.addEventListener('click', function (e) { if (e.target === player) fecharPlayer(); });
  player.addEventListener('close', function () { pv.pause(); });

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
