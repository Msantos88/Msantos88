# Site — Administração Emocional (Leonardo de Souza)

Site estático (HTML/CSS/JS), sem build. Abra `index.html` ou sirva a pasta (`python3 -m http.server`).

Seções: Sobre o livro · O que as pessoas estão dizendo · Autor e contato · Vídeos, Artigos & Podcasts · Onde comprar.

## Pendências (marcadas com `TODO` no código)
- Depoimentos reais (hoje são placeholders)
- E-mail de destino do formulário (`js/main.js`)
- Links reais de vídeos, artigos e podcasts
- Outras livrarias, se houver (hoje: Alta Books e Amazon)

## Deploy (Hostinger)
- Envie o conteúdo do zip para `public_html` **apagando antes os arquivos antigos** (ao extrair por cima, o gerenciador pode manter versões velhas).
- `.htaccess` aplica HTTPS, cabeçalhos de segurança (CSP, HSTS etc.), cache e compressão.
- CSS/JS usam `?v=4`; ao alterar, aumente o número para forçar atualização.
- Fontes são locais (`assets/fonts`); o site não faz requisições a terceiros.

## Pendências para publicar (atualizado)
- Email de destino do formulário: está no topo de `enviar.php` (`DESTINO`), hoje com email de TESTE do desenvolvedor. Trocar pelo do cliente e, se possível, definir `REMETENTE` com uma caixa real do domínio (ex.: contato@dominio)
- Links de vídeos, artigos e podcasts (hoje apontam para o perfil do Instagram)
- Depoimentos reais (seção mostra aviso "estão chegando" até lá)
- Definir onde publicar: domínio principal ou subdomínio (cliente já tem WordPress na Hostinger)

## Formulário de contato
- Envia direto pelo site via `enviar.php` (PHP, sem serviços de terceiros). Exige hospedagem com PHP (Hostinger e a maioria das hospedagens têm).
- Proteções: só aceita requisições do próprio site, campo armadilha, tempo mínimo, limite por IP (1 a cada 30 s, 5 por hora) e limpeza de cabeçalhos.
- Após publicar, envie uma mensagem de teste e confira também a caixa de spam.
