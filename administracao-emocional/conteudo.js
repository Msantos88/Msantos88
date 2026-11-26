/*
 * CONTEÚDO DO SITE ADMINISTRAÇÃO EMOCIONAL
 * ----------------------------------------
 * Este é o único arquivo que precisa ser editado para atualizar o site:
 * depoimentos, vídeos, podcasts, posts e pontos de venda.
 * Leia o "Manual de Atualização do Site" antes de editar.
 *
 * REGRAS DE OURO:
 *  1. Cada item fica entre chaves { ... } e termina com vírgula.
 *  2. Os textos ficam entre aspas retas duplas "assim".
 *  3. Dentro de um texto, NÃO use aspas retas ("). Use as aspas curvas “ ”.
 *  4. Não apague os colchetes [ ] nem os sinais : , ; que já existem.
 *  5. Faça uma cópia deste arquivo antes de editar (conteudo.js.copia).
 */
window.CONTEUDO = {

  // ===== DEPOIMENTOS (aparecem em "O que as pessoas estão dizendo") =====
  depoimentos: [
    {
      texto: "Como administrar nossas emoções sem compreender sua natureza e sua função? Administração Emocional nos proporciona uma nova compreensão de nós mesmos e do comportamento humano. Uma leitura que nos convida a repensar atitudes e a construir relações mais saudáveis, tanto na vida pessoal quanto na profissional.",
      nome: "Luciana Bittencourt",
      cargo: "jornalista"
    },
  ],

  // ===== VÍDEOS =====
  // "arquivo" é o nome do vídeo (MP4) enviado para a pasta assets/videos/
  videos: [
    { titulo: "O que são emoções e sentimentos", arquivo: "video-1.mp4" },
    { titulo: "Como esse livro pode mudar a saúde", arquivo: "video-2.mp4" },
    { titulo: "3 ações de liderança que podem mudar o ambiente", arquivo: "video-3.mp4" },
  ],

  // ===== PODCASTS =====
  // "youtube" é o link do episódio no YouTube (pode colar o link inteiro)
  podcasts: [
    { titulo: "Participação em podcast", youtube: "https://youtu.be/FCj0TthwZGQ" },
  ],

  // ===== POSTS DO INSTAGRAM =====
  // "link" é o endereço do post. "imagem" é a capa, enviada para assets/posts/
  posts: [
    { titulo: "O orçamento aumenta, as iniciativas se intensificam, mas os afastamentos e o turnover só crescem", link: "https://www.instagram.com/p/DfhLMyHH04o/", imagem: "orcamento.jpg" },
    { titulo: "Por que o estoicismo é ainda tão influente depois de séculos?", link: "https://www.instagram.com/p/DfS6OZCAJkZ/", imagem: "estoicismo.jpg" },
    { titulo: "Platão comparou as emoções a cavalos", link: "https://www.instagram.com/p/DfVX0RhH1Lr/", imagem: "platao.jpg" },
  ],

  // ===== ONDE COMPRAR =====
  // destaque: true  = botão grande no topo (use "formato" para o título do botão)
  // destaque: false = botão pequeno em "Também disponível em"
  lojas: [
    { nome: "Alta Books", formato: "Livro físico", destaque: true, link: "https://altabooks.com.br/produto/administracao-emocional/" },
    { nome: "Amazon", formato: "Livro digital", destaque: true, link: "https://www.amazon.com.br/dp/8550830410" },
    { nome: "Livraria da Travessa", destaque: false, link: "https://www.travessa.com.br/administracao-emocional-a-neurociencia-das-emocoes-transformando-as-organizacoes/artigo/4777101d-02e2-4151-b4bc-19608dfacd3c" },
    { nome: "Livraria Leitura", destaque: false, link: "https://leitura.com.br/produto/administracao-emocional-leonardo-de-souza-9788550830414" },
    { nome: "Livraria da Vila", destaque: false, link: "https://www.livrariadavila.com.br/970922-administracao-emocional-a-neurociencia/p" },
    { nome: "Martins Fontes", destaque: false, link: "https://www.martinsfontespaulista.com.br/administracao-emocional-1218841/p" },
    { nome: "Mercado Livre", destaque: false, link: "https://www.mercadolivre.com.br/administracao-emocional-souza-leonardo-de-editora-alta-books-capa-mole/p/MLB79083527" },
  ],
};
