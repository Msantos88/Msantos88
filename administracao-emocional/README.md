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
